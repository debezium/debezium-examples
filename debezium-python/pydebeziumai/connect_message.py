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
from dataclasses import dataclass
from datetime import date, datetime, time, timedelta, timezone
from decimal import Decimal
from typing import Any, Dict, List, Mapping, Optional, Type

try:
    import numpy as _np
except Exception:
    _np = None


@dataclass(frozen=True)
class ConversionConfig:
    """
    Controls how Java Connect values are converted to Python values.

    Attributes:
        numeric_mode:
            - "native": Python int/float
            - "numpy": NumPy scalar types when possible
    """

    numeric_mode: str = "native"


def _to_datetime_from_ms(ms: int) -> datetime:
    return datetime.fromtimestamp(ms / 1000.0, tz=timezone.utc)


def _to_datetime_from_us(us: int) -> datetime:
    return datetime.fromtimestamp(us / 1_000_000.0, tz=timezone.utc)


def _to_datetime_from_ns(ns: int) -> datetime:
    return datetime.fromtimestamp(ns / 1_000_000_000.0, tz=timezone.utc)


def _to_time_from_ms(ms: int) -> time:
    base = datetime(1970, 1, 1, tzinfo=timezone.utc) + timedelta(milliseconds=ms)
    return base.time()


def _to_time_from_us(us: int) -> time:
    base = datetime(1970, 1, 1, tzinfo=timezone.utc) + timedelta(microseconds=us)
    return base.time()


def _maybe_numpy_int(value: int, cfg: ConversionConfig) -> Any:
    if cfg.numeric_mode != "numpy" or _np is None:
        return value
    try:
        return _np.int64(value)
    except Exception:
        return value


def _maybe_numpy_float(value: float, cfg: ConversionConfig) -> Any:
    if cfg.numeric_mode != "numpy" or _np is None:
        return value
    try:
        return _np.float64(value)
    except Exception:
        return value


def _maybe_numpy_bool(value: bool, cfg: ConversionConfig) -> Any:
    if cfg.numeric_mode != "numpy" or _np is None:
        return value
    try:
        return _np.bool_(value)
    except Exception:
        return value


def _coerce_logical(schema_name: Optional[str], value: Any) -> Any:
    if value is None or not schema_name:
        return value

    try:
        if schema_name in ("org.apache.kafka.connect.data.Date", "io.debezium.time.Date"):
            return date(1970, 1, 1) + timedelta(days=int(value))

        if schema_name == "org.apache.kafka.connect.data.Time":
            return _to_time_from_ms(int(value))

        if schema_name in ("org.apache.kafka.connect.data.Timestamp", "io.debezium.time.Timestamp"):
            return _to_datetime_from_ms(int(value))

        if schema_name == "io.debezium.time.MicroTime":
            return _to_time_from_us(int(value))

        if schema_name == "io.debezium.time.MicroTimestamp":
            return _to_datetime_from_us(int(value))

        if schema_name == "io.debezium.time.NanoTimestamp":
            return _to_datetime_from_ns(int(value))

        if schema_name == "io.debezium.time.ZonedTimestamp":
            s = str(value)
            if s.endswith("Z"):
                s = s[:-1] + "+00:00"
            return datetime.fromisoformat(s)

        if schema_name in ("org.apache.kafka.connect.data.Decimal", "io.debezium.data.VariableScaleDecimal"):
            if hasattr(value, "toPlainString"):
                return Decimal(str(value.toPlainString()))
            return Decimal(str(value))

        if schema_name == "io.debezium.time.Year":
            return int(value)
    except Exception:
        return value

    return value


def _build_typed_instance(data: Optional[Mapping[str, Any]], cls: Type[Any]) -> Any:
    if data is None:
        return None

    # Pydantic v2
    if hasattr(cls, "model_validate"):
        return cls.model_validate(dict(data))
    # Pydantic v1
    if hasattr(cls, "parse_obj"):
        return cls.parse_obj(dict(data))
    # Dataclass/regular class constructor
    return cls(**dict(data))


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

def struct_to_dict(value: Any, schema: Any, config: Optional[ConversionConfig] = None) -> Any:
    """
    Recursively converts a Java Kafka Connect Struct (with its Schema) to Python-native types.

    This is the core function that eliminates JSON serialization/deserialization overhead.
    It uses JPype to directly read Java Schema.Type and Struct.get() calls.

    Supported Schema types:
        STRUCT, STRING, INT8, INT16, INT32, INT64,
        FLOAT32, FLOAT64, BOOLEAN, BYTES, ARRAY, MAP

    Supported logical schema names:
        org.apache.kafka.connect.data.Date
        org.apache.kafka.connect.data.Time
        org.apache.kafka.connect.data.Timestamp
        org.apache.kafka.connect.data.Decimal
        io.debezium.time.Date
        io.debezium.time.Timestamp
        io.debezium.time.MicroTime
        io.debezium.time.MicroTimestamp
        io.debezium.time.NanoTimestamp
        io.debezium.time.ZonedTimestamp
        io.debezium.time.Year
        io.debezium.data.VariableScaleDecimal

    Args:
        value:  A Java value (Struct, String, Integer, List, Map, etc.)
        schema: A Java org.apache.kafka.connect.data.Schema describing value's type.

    Returns:
        Python-native equivalent (dict, str, int, float, bool, bytes, list, or None).
    """
    if config is None:
        config = ConversionConfig()

    if value is None:
        return None

    try:
        schema_type = str(schema.type().name())
        schema_name = str(schema.name()) if schema.name() is not None else None
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
            result[field_name] = struct_to_dict(field_value, field.schema(), config=config)
        return result

    elif schema_type == "STRING":
        return _coerce_logical(schema_name, str(value))

    elif schema_type in ("INT8", "INT16", "INT32", "INT64"):
        iv = int(value)
        iv = _coerce_logical(schema_name, iv)
        if isinstance(iv, int):
            return _maybe_numpy_int(iv, config)
        return iv

    elif schema_type in ("FLOAT32", "FLOAT64"):
        fv = float(value)
        fv = _coerce_logical(schema_name, fv)
        if isinstance(fv, float):
            return _maybe_numpy_float(fv, config)
        return fv

    elif schema_type == "BOOLEAN":
        bv = bool(value)
        bv = _coerce_logical(schema_name, bv)
        if isinstance(bv, bool):
            return _maybe_numpy_bool(bv, config)
        return bv

    elif schema_type == "BYTES":
        try:
            b = bytes(value)
            b = _coerce_logical(schema_name, b)
            return b
        except Exception:
            return _coerce_logical(schema_name, str(value))

    elif schema_type == "ARRAY":
        try:
            item_schema = schema.valueSchema()
            return [struct_to_dict(item, item_schema, config=config) for item in value]
        except Exception:
            return str(value)

    elif schema_type == "MAP":
        try:
            key_schema = schema.keySchema()
            val_schema = schema.valueSchema()
            result = {}
            for entry in value.entrySet():
                k = struct_to_dict(entry.getKey(), key_schema, config=config)
                v = struct_to_dict(entry.getValue(), val_schema, config=config)
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

    def __init__(
        self,
        source_record: Any,
        conversion_config: Optional[ConversionConfig] = None,
        topic_class_map: Optional[Mapping[str, Type[Any]]] = None,
    ):
        """
        Args:
            source_record: Raw Java EmbeddedEngineChangeEvent or SourceRecord 
                          from BaseConnectChangeHandler.handleConnectBatch.
            conversion_config: Controls native vs NumPy scalar output.
            topic_class_map: Optional topic/table -> class mapping.
                Mapping keys can be full topic, short topic, or table name.
        """
        # If it's an EmbeddedEngineChangeEvent, unwrap to get the SourceRecord
        if hasattr(source_record, 'record'):
            self._record = source_record.record()
        else:
            self._record = source_record
        self._key_dict: Optional[Dict[str, Any]] = None
        self._value_dict: Optional[Dict[str, Any]] = None
        self._conversion_config = conversion_config or ConversionConfig()
        self._topic_class_map = dict(topic_class_map or {})

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
                self._key_dict = struct_to_dict(key, key_schema, config=self._conversion_config)
        return self._key_dict

    @property
    def value_dict(self) -> Optional[Dict[str, Any]]:
        """The full record value as a Python dict (converted from Java Struct, no JSON)."""
        if self._value_dict is None:
            val = self._record.value()
            val_schema = self._record.valueSchema()
            if val is not None and val_schema is not None:
                self._value_dict = struct_to_dict(val, val_schema, config=self._conversion_config)
        return self._value_dict

    def _lookup_mapped_class(self) -> Optional[Type[Any]]:
        if not self._topic_class_map:
            return None

        full_topic = self.destination
        short_topic = ".".join(full_topic.split(".")[-2:]) if "." in full_topic else full_topic
        table_name = full_topic.split(".")[-1]

        return (
            self._topic_class_map.get(full_topic)
            or self._topic_class_map.get(short_topic)
            or self._topic_class_map.get(table_name)
        )

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

    @property
    def before_typed(self) -> Any:
        mapped_class = self._lookup_mapped_class()
        if mapped_class is None:
            return self.before
        return _build_typed_instance(self.before, mapped_class)

    @property
    def after_typed(self) -> Any:
        mapped_class = self._lookup_mapped_class()
        if mapped_class is None:
            return self.after
        return _build_typed_instance(self.after, mapped_class)

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


def extract_connect_all(
    records: List[Any],
    conversion_config: Optional[ConversionConfig] = None,
    topic_class_map: Optional[Mapping[str, Type[Any]]] = None,
) -> List[SourceRecordExtractor]:
    """
    Converts a batch of raw Java SourceRecord objects into SourceRecordExtractor instances.

    Args:
        records: List of raw Java SourceRecord objects from handleConnectBatch.
        conversion_config: Controls native vs NumPy scalar output.
        topic_class_map: Optional topic/table -> class mapping.

    Returns:
        List of SourceRecordExtractor instances.
    """
    return [
        SourceRecordExtractor(
            r,
            conversion_config=conversion_config,
            topic_class_map=topic_class_map,
        )
        for r in records
    ]
