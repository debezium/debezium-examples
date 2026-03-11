"""
connect_message.py

Provides utilities to:
1. Expose raw Debezium ChangeEvent (Java ConnectRecord) fields to Python via JPype.
2. Introspect the Java object to discover available methods/fields.
3. Support both JSON format (current) and native Connect format (extended).

Usage:
    from pydebeziumai.connect_message import ConnectMessageExtractor, print_record_info
"""

import json
from typing import Any, Dict, List, Optional


def print_record_info(record) -> None:
    """
    Introspects a raw Java ChangeEvent object via JPype and prints all available fields/methods.
    Useful for exploring what the Java object exposes to Python.

    Args:
        record: A raw Java ChangeEvent object received in handleJsonBatch/handleConnectBatch.
    """
    print("=" * 60)
    print("ConnectRecord Introspection")
    print("=" * 60)

    # Standard ChangeEvent interface methods (always available)
    try:
        print(f"  destination() : {record.destination()}")
    except Exception as e:
        print(f"  destination() : ERROR - {e}")

    try:
        print(f"  partition()   : {record.partition()}")
    except Exception as e:
        print(f"  partition()   : ERROR - {e}")

    try:
        raw_key = record.key()
        print(f"  key()         : {raw_key}")
    except Exception as e:
        print(f"  key()         : ERROR - {e}")

    try:
        raw_value = record.value()
        print(f"  value()       : {raw_value}")
    except Exception as e:
        print(f"  value()       : ERROR - {e}")

    # Try to access the underlying SourceRecord fields via JPype
    try:
        print(f"  Java class    : {record.getClass().getName()}")
    except Exception as e:
        print(f"  Java class    : ERROR - {e}")

    # Try extra fields available on SourceRecord if accessible
    for method_name in ["timestamp", "topic", "kafkaPartition", "headers"]:
        try:
            method = getattr(record, method_name, None)
            if method is not None:
                print(f"  {method_name}() : {method()}")
        except Exception as e:
            print(f"  {method_name}() : ERROR - {e}")

    print("=" * 60)


class ConnectMessageExtractor:
    """
    Extracts fields from a Debezium ChangeEvent (Java object, passed via JPype).

    When using JSON format (EngineFormat.JSON):
        - key() and value() return JSON strings.
        - This class parses them and exposes schema + payload as Python dicts.

    When using Connect format (EngineFormat.Connect):  [future]
        - key() and value() return Java Struct objects.
        - Full schema info is available from keySchema() / valueSchema().
    """

    def __init__(self, record):
        """
        Args:
            record: Raw Java ChangeEvent object from pydbzengine handleJsonBatch.
        """
        self._record = record
        self._raw_key: Optional[str] = None
        self._raw_value: Optional[str] = None
        self._parsed_value: Optional[Dict[str, Any]] = None

    @property
    def destination(self) -> str:
        """The destination topic/table name."""
        return str(self._record.destination())

    @property
    def partition(self) -> Optional[int]:
        """The partition of the record."""
        p = self._record.partition()
        return int(p) if p is not None else None

    @property
    def raw_key(self) -> Optional[str]:
        """The raw key as a JSON string (JSON format)."""
        if self._raw_key is None:
            k = self._record.key()
            self._raw_key = str(k) if k is not None else None
        return self._raw_key

    @property
    def raw_value(self) -> Optional[str]:
        """The raw value as a JSON string (JSON format)."""
        if self._raw_value is None:
            v = self._record.value()
            self._raw_value = str(v) if v is not None else None
        return self._raw_value

    @property
    def parsed_value(self) -> Optional[Dict[str, Any]]:
        """The parsed JSON value as a Python dict (schema + payload envelope)."""
        if self._parsed_value is None and self.raw_value:
            self._parsed_value = json.loads(self.raw_value)
        return self._parsed_value

    @property
    def schema(self) -> Optional[Dict[str, Any]]:
        """The schema part of the Debezium JSON envelope."""
        if self.parsed_value:
            return self.parsed_value.get("schema")
        return None

    @property
    def payload(self) -> Optional[Dict[str, Any]]:
        """The payload part of the Debezium JSON envelope (before, after, op, ts_ms, etc.)."""
        if self.parsed_value:
            return self.parsed_value.get("payload")
        return None

    @property
    def before(self) -> Optional[Dict[str, Any]]:
        """The row state before the change."""
        return self.payload.get("before") if self.payload else None

    @property
    def after(self) -> Optional[Dict[str, Any]]:
        """The row state after the change."""
        return self.payload.get("after") if self.payload else None

    @property
    def op(self) -> Optional[str]:
        """The operation type: c=create, u=update, d=delete, r=read/snapshot."""
        return self.payload.get("op") if self.payload else None

    @property
    def ts_ms(self) -> Optional[int]:
        """The timestamp in milliseconds when the event was captured."""
        return self.payload.get("ts_ms") if self.payload else None

    def to_dict(self) -> Dict[str, Any]:
        """Returns a full Python dict representation of the record."""
        return {
            "destination": self.destination,
            "partition": self.partition,
            "key": self.raw_key,
            "op": self.op,
            "before": self.before,
            "after": self.after,
            "ts_ms": self.ts_ms,
            "schema": self.schema,
        }

    def __repr__(self) -> str:
        return (
            f"ConnectMessageExtractor("
            f"destination={self.destination!r}, "
            f"op={self.op!r}, "
            f"after={self.after!r})"
        )


def extract_all(records: List[Any]) -> List[ConnectMessageExtractor]:
    """
    Convenience function: wraps a list of raw Java ChangeEvent objects into
    ConnectMessageExtractor instances.

    Args:
        records: List of raw Java ChangeEvent objects from handleJsonBatch.

    Returns:
        List of ConnectMessageExtractor instances.
    """
    return [ConnectMessageExtractor(r) for r in records]


# ─────────────────────────────────────────────────────────────────────────────
# Connect Format (EngineFormat.CONNECT) — zero JSON overhead
# ─────────────────────────────────────────────────────────────────────────────

def struct_to_dict(value: Any, schema: Any) -> Any:
    """
    Recursively converts a Java Kafka Connect Struct (with its Schema) to Python-native types.

    This is the core function that eliminates JSON serialization/deserialization overhead.
    It uses JPype to directly read Java Schema.Type and Struct.get() calls.

    Supported Schema types:
        STRUCT, STRING, INT8, INT16, INT32, INT64,
        FLOAT32, FLOAT64, BOOLEAN, BYTES, ARRAY, MAP

    Args:
        value:  A Java value (Struct, String, Integer, List, Map, etc.)
        schema: A Java org.apache.kafka.connect.data.Schema describing value's type.

    Returns:
        Python-native equivalent (dict, str, int, float, bool, bytes, list, or None).
    """
    if value is None:
        return None

    try:
        schema_type = str(schema.type().name())
    except Exception:
        # Fallback: unknown schema type, return string representation
        return str(value)

    if schema_type == "STRUCT":
        result = {}
        for field in schema.fields():
            field_name = str(field.name())
            try:
                field_value = value.get(field)
            except Exception:
                field_value = None
            result[field_name] = struct_to_dict(field_value, field.schema())
        return result

    elif schema_type == "STRING":
        return str(value)

    elif schema_type in ("INT8", "INT16", "INT32", "INT64"):
        return int(value)

    elif schema_type in ("FLOAT32", "FLOAT64"):
        return float(value)

    elif schema_type == "BOOLEAN":
        return bool(value)

    elif schema_type == "BYTES":
        try:
            return bytes(value)
        except Exception:
            return str(value)

    elif schema_type == "ARRAY":
        try:
            item_schema = schema.valueSchema()
            return [struct_to_dict(item, item_schema) for item in value]
        except Exception:
            return str(value)

    elif schema_type == "MAP":
        try:
            key_schema = schema.keySchema()
            val_schema = schema.valueSchema()
            result = {}
            for entry in value.entrySet():
                k = struct_to_dict(entry.getKey(), key_schema)
                v = struct_to_dict(entry.getValue(), val_schema)
                result[k] = v
            return result
        except Exception:
            return str(value)

    else:
        # Unknown type — best-effort string fallback
        try:
            return str(value)
        except Exception:
            return None


class SourceRecordExtractor:
    """
    Extracts fields from a raw Java SourceRecord object (EngineFormat.CONNECT).

    Zero JSON overhead: reads Java Struct objects directly via JPype and converts
    them to Python dicts using struct_to_dict().

    Use this when using DebeziumConnectEngine.
    """

    def __init__(self, source_record: Any):
        """
        Args:
            source_record: Raw Java EmbeddedEngineChangeEvent or SourceRecord 
                          from BaseConnectChangeHandler.handleConnectBatch.
        """
        # If it's an EmbeddedEngineChangeEvent, unwrap to get the SourceRecord
        if hasattr(source_record, 'record'):
            self._record = source_record.record()
        else:
            self._record = source_record
        self._key_dict: Optional[Dict[str, Any]] = None
        self._value_dict: Optional[Dict[str, Any]] = None

    @property
    def destination(self) -> str:
        """The topic/table name."""
        return str(self._record.topic())

    @property
    def partition(self) -> Optional[int]:
        p = self._record.kafkaPartition()
        return int(p) if p is not None else None

    @property
    def key_dict(self) -> Optional[Dict[str, Any]]:
        """The record key as a Python dict (converted from Java Struct, no JSON)."""
        if self._key_dict is None:
            key = self._record.key()
            key_schema = self._record.keySchema()
            if key is not None and key_schema is not None:
                self._key_dict = struct_to_dict(key, key_schema)
        return self._key_dict

    @property
    def value_dict(self) -> Optional[Dict[str, Any]]:
        """The full record value as a Python dict (converted from Java Struct, no JSON)."""
        if self._value_dict is None:
            val = self._record.value()
            val_schema = self._record.valueSchema()
            if val is not None and val_schema is not None:
                self._value_dict = struct_to_dict(val, val_schema)
        return self._value_dict

    @property
    def before(self) -> Optional[Dict[str, Any]]:
        return self.value_dict.get("before") if self.value_dict else None

    @property
    def after(self) -> Optional[Dict[str, Any]]:
        return self.value_dict.get("after") if self.value_dict else None

    @property
    def op(self) -> Optional[str]:
        return self.value_dict.get("op") if self.value_dict else None

    @property
    def ts_ms(self) -> Optional[int]:
        return self.value_dict.get("ts_ms") if self.value_dict else None

    def to_dict(self) -> Dict[str, Any]:
        return {
            "destination": self.destination,
            "partition": self.partition,
            "key": self.key_dict,
            "op": self.op,
            "before": self.before,
            "after": self.after,
            "ts_ms": self.ts_ms,
        }

    def __repr__(self) -> str:
        return (
            f"SourceRecordExtractor("
            f"destination={self.destination!r}, "
            f"op={self.op!r}, "
            f"after={self.after!r})"
        )


def extract_connect_all(records: List[Any]) -> List[SourceRecordExtractor]:
    """
    Converts a batch of raw Java SourceRecord objects into SourceRecordExtractor instances.

    Args:
        records: List of raw Java SourceRecord objects from handleConnectBatch.

    Returns:
        List of SourceRecordExtractor instances.
    """
    return [SourceRecordExtractor(r) for r in records]
