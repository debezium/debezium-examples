# Debezium Python - Connect Mode Example

This example demonstrates consuming Debezium CDC events in Python using **Connect mode**, where Java `SourceRecord` objects are converted directly to Python dicts via JPype (no JSON serialization overhead).

> **Note:** JAR dependencies are **not** committed to this repository. They are downloaded automatically via Maven using the `setup_jars.py` script during setup.

## Key Features

- **Zero JSON overhead**: Direct Java → Python conversion without serialization
- **Type-safe**: Pydantic validation for CDC events
- **Extended type conversion**: Handles Debezium/Kafka Connect logical types (timestamps, dates, decimals)
- **Configurable numeric output**: Python native scalars or NumPy scalars
- **Optional static class mapping**: Map topic/table names to Python classes for typed `before`/`after`
- **Simple setup**: Automated script to configure Debezium 3.0 JARs
- **Full CDC operations**: Supports read (r), create (c), update (u), and delete (d) operations

## Prerequisites

- Python 3.9+
- Java 17+ (`JAVA_HOME` must be set)
- Docker (for testcontainers Postgres)
- Maven (for downloading Debezium JAR dependencies)

## Quick Start

1. **Create and activate a virtual environment:**

   ```bash
   python3 -m venv .venv
   source .venv/bin/activate  # On Windows: .venv\Scripts\activate
   ```
2. **Install dependencies:**

   ```bash
   pip install -r requirements.txt
   ```
3. **Install Maven** (if not already installed):

   ```bash
   sudo apt update && sudo apt install maven
   ```
4. **Set up Debezium JARs:**

   ```bash
   python3 setup_jars.py
   ```

   This script uses Maven to download ~400 Debezium 3.0.0.Final JAR dependencies and installs them to the pydbzengine package. The JARs are intentionally not committed to the repository to keep it lightweight.
5. **Run the example:**

   ```bash
   python3 connect_mode_test.py
   ```

## What This Example Does

The script:

1. Starts a Postgres container with sample data (`inventory.customers`)
2. Runs Debezium embedded engine in Connect mode
3. Captures initial snapshot (4 existing records with operation='r')
4. Shows the expanded `before`/`after` Python structures
5. Validates events with Pydantic models
6. Stops after processing 5 events

## Expected Output

```
================================================================================
Received batch with 4 records (Connect mode - zero JSON overhead)
================================================================================

--- Record 1/4 ---
Destination: connect_test.inventory.customers
Operation:   r

AFTER (fully expanded Python dict):
  Type: <class 'dict'>
  Content: {'id': 1001, 'first_name': 'Sally', 'last_name': 'Thomas', 'email': 'sally.thomas@acme.com'}

Pydantic Validation: PASSED
  Validated op: r
  Is create: False
  Is update: False
  Is delete: False
```

## Connect Mode vs JSON Mode

| Mode                   | Pipeline                                                              |
| ---------------------- | --------------------------------------------------------------------- |
| **JSON mode**    | `record.value()` → JSON string → `json.loads()` → Python dict  |
| **Connect mode** | `record.value()` → Java Struct → direct conversion → Python dict |

Connect mode eliminates JSON serialization, providing better performance and lower memory usage.

## Advanced conversion options

The Connect-mode extractor supports configurable conversion and optional typed mapping:

```python
from dataclasses import dataclass
from pydebeziumai import ConversionConfig, SourceRecordExtractor

@dataclass
class Customer:
   id: int
   first_name: str
   last_name: str
   email: str

cfg = ConversionConfig(numeric_mode="numpy")  # or "native"

topic_map = {
   "connect_test.inventory.customers": Customer,
   # also supports "inventory.customers" or "customers"
}

extractor = SourceRecordExtractor(
   source_record,
   conversion_config=cfg,
   topic_class_map=topic_map,
)

# Default dict access
print(extractor.after)

# Typed class access (if mapping exists)
print(extractor.after_typed)
```
