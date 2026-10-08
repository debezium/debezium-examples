"""
connect_mode_test.py

Demonstrates the Connect mode pipeline (format="connect"):
  Debezium  raw Java SourceRecord  struct_to_dict  Python dict  Pydantic validation

NO JSON serialization/deserialization overhead.

This example:
1. Starts a Postgres container with test data
2. Runs Debezium in Connect mode (not JSON mode)
3. Uses a BasePythonChangeHandler to receive raw SourceRecord objects
4. Converts them to validated Pydantic models
5. Prints the expanded before/after structures
"""

from pathlib import Path
from testcontainers.postgres import PostgresContainer

from pydbzengine import BasePythonChangeHandler, DebeziumEngine
from pydebeziumai import DebeziumEventModel, SourceRecordExtractor, print_record_info

OFFSET_FILE = Path(__file__).parent.joinpath('connect-mode-offsets.dat')


class DbPostgresql:
    POSTGRES_USER = "postgres"
    POSTGRES_PASSWORD = "postgres"
    POSTGRES_DBNAME = "postgres"
    POSTGRES_IMAGE = "quay.io/debezium/example-postgres:3.5"
    POSTGRES_HOST = "localhost"
    POSTGRES_PORT_DEFAULT = 5432
    CONTAINER: PostgresContainer = (PostgresContainer(image=POSTGRES_IMAGE,
                                                      port=POSTGRES_PORT_DEFAULT,
                                                      username=POSTGRES_USER,
                                                      password=POSTGRES_PASSWORD,
                                                      dbname=POSTGRES_DBNAME,
                                                      driver=None)
                                    .with_exposed_ports(POSTGRES_PORT_DEFAULT)
                                    )

    def start(self):
        print("Starting Postgresql Db...")
        self.CONTAINER.start()

    def stop(self):
        print("Stopping Postgresql Db...")
        self.CONTAINER.stop()


def debezium_engine_props(sourcedb: DbPostgresql) -> dict:
    """Create Debezium configuration for Connect mode."""
    return {
        "name": "connect-test-engine",
        "offset.storage": "org.apache.kafka.connect.storage.FileOffsetBackingStore",
        "offset.storage.file.filename": str(OFFSET_FILE),
        "offset.flush.interval.ms": "1000",
        "database.hostname": sourcedb.POSTGRES_HOST,
        "database.port": str(sourcedb.CONTAINER.get_exposed_port(sourcedb.POSTGRES_PORT_DEFAULT)),
        "database.user": sourcedb.POSTGRES_USER,
        "database.password": sourcedb.POSTGRES_PASSWORD,
        "database.dbname": sourcedb.POSTGRES_DBNAME,
        "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
        "topic.prefix": "connect_test",
        "schema.include.list": "inventory",
        "table.include.list": "inventory.customers",
        "plugin.name": "pgoutput",
        "snapshot.mode": "initial",
    }


class ConnectModeHandler(BasePythonChangeHandler):
    """
    Handler for Connect mode - receives raw Java SourceRecord objects.
    No JSON parsing happens.

    pydbzengine delivers every batch through handleJsonBatch regardless of the
    engine format. With format="connect" the records are raw Java SourceRecord
    objects rather than JSON strings.
    """

    def __init__(self, max_events=4):
        self.event_count = 0
        self.max_events = max_events  # Stop after processing a few events
        self.stop_engine = None  # Set by main() once the engine exists

    def handleJsonBatch(self, records):
        """
        Process a batch of raw Java SourceRecord objects.
        
        Args:
            records: List of Java SourceRecord objects (not JSON strings!)
        """
        print(f"\n{'='*80}")
        print(f"Received batch with {len(records)} records (Connect mode - zero JSON overhead)")
        print(f"{'='*80}\n")

        for idx, record in enumerate(records):
            self.event_count += 1
            
            print(f"\n--- Record {idx + 1}/{len(records)} ---")
            
            # Option 1: Use SourceRecordExtractor for direct access
            extractor = SourceRecordExtractor(record)
            print(f"Destination: {extractor.destination}")
            print(f"Partition:   {extractor.partition}")
            print(f"Operation:   {extractor.op}")
            
            # Show the expanded Python structures (no JSON!)
            if extractor.before:
                print(f"\nBEFORE (fully expanded Python dict):")
                print(f"  Type: {type(extractor.before)}")
                print(f"  Content: {extractor.before}")
            
            if extractor.after:
                print(f"\nAFTER (fully expanded Python dict):")
                print(f"  Type: {type(extractor.after)}")
                print(f"  Content: {extractor.after}")
            
            # Option 2: Use Pydantic model for validation
            try:
                validated_event = DebeziumEventModel.from_source_record(record)
                print(f"\nPydantic Validation:  PASSED")
                print(f"  Validated op: {validated_event.payload.op}")
                print(f"  Is create: {validated_event.is_create()}")
                print(f"  Is update: {validated_event.is_update()}")
                print(f"  Is delete: {validated_event.is_delete()}")
                print(f"  Current state: {validated_event.get_current_state()}")
            except Exception as e:
                print(f"\nPydantic Validation:  FAILED - {e}")
            
            # Optional: introspect the raw Java object
            if idx == 0:
                print(f"\n--- Java SourceRecord Introspection (first record only) ---")
                print_record_info(record)
            
            print(f"\n{''*80}")
            
            # Stop after max_events
            if self.event_count >= self.max_events:
                print(f"\nReached {self.max_events} events, stopping engine...")
                self.stop_engine()
                break


def main():
    """
    Main test function:
    1. Start Postgres with Debezium example data
    2. Run Debezium in Connect mode
    3. Print expanded before/after structures
    4. Validate with Pydantic
    """
    print("\n" + "="*80)
    print("CONNECT MODE TEST - Zero JSON Overhead")
    print("="*80 + "\n")
    
    # Verify JARs are installed
    import pydbzengine
    jar_dir = Path(pydbzengine.__file__).parent / "debezium" / "libs"
    if not jar_dir.exists() or len(list(jar_dir.glob("*.jar"))) == 0:
        print("❌ ERROR: Debezium JARs not found!")
        print(f"   Expected location: {jar_dir}")
        print("\nPlease run the setup script first:")
        print("   python3 setup_jars.py")
        return
    
    print(f"✓ Found {len(list(jar_dir.glob('*.jar')))} JAR files\n")
    
    # Clean up old offset file
    if OFFSET_FILE.exists():
        OFFSET_FILE.unlink()
        print(f"Removed old offset file: {OFFSET_FILE}")
    
    # Start Postgres container
    sourcedb = DbPostgresql()
    sourcedb.start()
    
    try:
        # Create Debezium config for Connect mode
        props = debezium_engine_props(sourcedb)
        
        # Create handler
        handler = ConnectModeHandler()
        
        # Create engine in Connect mode (not JSON mode!)
        print("\nInitializing DebeziumEngine (format=\"connect\")...")
        engine = DebeziumEngine(properties=props, handler=handler, format="connect")
        # The handler runs on the engine thread, so it stops the engine by
        # interrupting that thread through the engine's consumer.
        handler.stop_engine = lambda: engine.consumer.interrupt()

        print("Starting engine... (will process snapshot and stop after a few events)\n")

        # Run the engine
        engine.run()

        print("\n" + "="*80)
        print("Test completed successfully!")
        print(f"Total events processed: {handler.event_count}")
        print("="*80 + "\n")
        
    except KeyboardInterrupt:
        print("\nInterrupted by user")
    except Exception as e:
        print(f"\nError during test: {e}")
        import traceback
        traceback.print_exc()
    finally:
        sourcedb.stop()
        print("\nPostgres container stopped.")


if __name__ == "__main__":
    main()
