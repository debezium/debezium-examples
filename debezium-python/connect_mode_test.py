"""
connect_mode_test.py

Demonstrates the Connect mode pipeline (EngineFormat.CONNECT):
  Debezium  raw Java SourceRecord  struct_to_dict  Python dict  Pydantic validation

NO JSON serialization/deserialization overhead.

This example:
1. Starts a Postgres container with test data
2. Runs Debezium in Connect mode (not JSON mode)
3. Uses BaseConnectChangeHandler to receive raw SourceRecord objects
4. Converts them to validated Pydantic models
5. Prints the expanded before/after structures
"""

from pathlib import Path
from testcontainers.postgres import PostgresContainer
from testcontainers.core.waiting_utils import wait_for_logs

# Import local Connect mode extensions
from debezium_connect import DebeziumConnectEngine, BaseConnectChangeHandler
from pydebeziumai import DebeziumEventModel, SourceRecordExtractor, print_record_info

OFFSET_FILE = Path(__file__).parent.joinpath('connect-mode-offsets.dat')


def wait_for_postgresql_to_start(self) -> None:
    """Patch for testcontainers PostgreSQL startup detection."""
    wait_for_logs(self, ".*PostgreSQL init process complete.*")


class DbPostgresql:
    POSTGRES_USER = "postgres"
    POSTGRES_PASSWORD = "postgres"
    POSTGRES_DBNAME = "postgres"
    POSTGRES_IMAGE = "debezium/example-postgres:3.0.0.Final"
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
    PostgresContainer._connect = wait_for_postgresql_to_start

    def start(self):
        print("Starting Postgresql Db...")
        self.CONTAINER.start()

    def stop(self):
        print("Stopping Postgresql Db...")
        self.CONTAINER.stop()


def debezium_engine_props(sourcedb: DbPostgresql):
    """Create Debezium configuration for Connect mode."""
    from pydbzengine._jvm import Properties
    props = Properties()
    props.setProperty("name", "connect-test-engine")
    props.setProperty("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
    props.setProperty("offset.storage.file.filename", str(OFFSET_FILE))
    props.setProperty("offset.flush.interval.ms", "1000")
    props.setProperty("database.hostname", sourcedb.POSTGRES_HOST)
    props.setProperty("database.port", str(sourcedb.CONTAINER.get_exposed_port(sourcedb.POSTGRES_PORT_DEFAULT)))
    props.setProperty("database.user", sourcedb.POSTGRES_USER)
    props.setProperty("database.password", sourcedb.POSTGRES_PASSWORD)
    props.setProperty("database.dbname", sourcedb.POSTGRES_DBNAME)
    props.setProperty("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
    props.setProperty("topic.prefix", "connect_test")
    props.setProperty("schema.include.list", "inventory")
    props.setProperty("table.include.list", "inventory.customers")
    props.setProperty("plugin.name", "pgoutput")
    props.setProperty("snapshot.mode", "initial")
    return props


class ConnectModeHandler(BaseConnectChangeHandler):
    """
    Handler for Connect mode - receives raw Java SourceRecord objects.
    No JSON parsing happens.
    """

    def __init__(self):
        self.event_count = 0
        self.max_events = 5  # Stop after processing a few events

    def handleConnectBatch(self, records):
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
                from pydbzengine._jvm import JavaLangThread
                JavaLangThread.currentThread().interrupt()
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
        print("\nInitializing DebeziumConnectEngine (EngineFormat.CONNECT)...")
        engine = DebeziumConnectEngine(properties=props, handler=handler)
        
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
