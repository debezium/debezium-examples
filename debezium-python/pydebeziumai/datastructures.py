"""
datastructures.py

Pure Python data structures representing a Debezium CDC event.
These are built from the ConnectMessageExtractor after inspecting
what fields the Java ChangeEvent exposes.

Flow:
    Java ChangeEvent (JPype)
        → ConnectMessageExtractor  (connect_message.py)
            → DebeziumRecord        (this file)
"""

from dataclasses import dataclass, field
from typing import Any, Dict, List, Mapping, Optional, Type

from .connect_message import ConnectMessageExtractor, ConversionConfig


@dataclass
class DebeziumSchema:
    """
    Represents the schema section of a Debezium JSON envelope.
    Contains field definitions, types, and metadata about the message structure.
    """
    raw: Dict[str, Any] = field(default_factory=dict)

    @property
    def type(self) -> Optional[str]:
        return self.raw.get("type")

    @property
    def fields(self) -> List[Dict[str, Any]]:
        return self.raw.get("fields", [])

    @property
    def name(self) -> Optional[str]:
        return self.raw.get("name")

    def __repr__(self) -> str:
        return f"DebeziumSchema(name={self.name!r}, type={self.type!r})"


@dataclass
class DebeziumRecord:
    """
    A Python-native representation of a single Debezium CDC event.

    Fields:
        destination: The topic/table where the event originates.
        partition:   Kafka partition (if applicable).
        key:         The record key (JSON string).
        op:          Operation type: 'c'=create, 'u'=update, 'd'=delete, 'r'=read/snapshot.
        before:      Row state before the change (None for inserts/reads).
        after:       Row state after the change (None for deletes).
        ts_ms:       Timestamp in ms when the event was captured by Debezium.
        schema:      The Debezium schema envelope.
    """
    destination: str
    op: str
    before: Optional[Dict[str, Any]]
    after: Optional[Dict[str, Any]]
    ts_ms: Optional[int]
    key: Optional[str] = None
    partition: Optional[int] = None
    schema: Optional[DebeziumSchema] = None

    @classmethod
    def from_extractor(cls, extractor: ConnectMessageExtractor) -> "DebeziumRecord":
        """
        Factory method: builds a DebeziumRecord from a ConnectMessageExtractor.

        Args:
            extractor: A ConnectMessageExtractor wrapping a raw Java ChangeEvent.

        Returns:
            A DebeziumRecord populated with all available fields.
        """
        schema = DebeziumSchema(raw=extractor.schema) if extractor.schema else None
        return cls(
            destination=extractor.destination,
            op=extractor.op or "r",
            before=extractor.before,
            after=extractor.after,
            ts_ms=extractor.ts_ms,
            key=extractor.raw_key,
            partition=extractor.partition,
            schema=schema,
        )

    @classmethod
    def from_java_record(cls, java_record) -> "DebeziumRecord":
        """
        Convenience shortcut: builds a DebeziumRecord directly from a raw Java ChangeEvent.

        Args:
            java_record: Raw Java ChangeEvent from pydbzengine handleJsonBatch.

        Returns:
            A DebeziumRecord populated with all available fields.
        """
        return cls.from_extractor(ConnectMessageExtractor(java_record))

    @classmethod
    def from_source_record(
        cls,
        source_record,
        conversion_config: Optional[ConversionConfig] = None,
        topic_class_map: Optional[Mapping[str, Type[Any]]] = None,
    ) -> "DebeziumRecord":
        """
        Builds a DebeziumRecord from a raw Java SourceRecord (EngineFormat.CONNECT).
        No JSON serialization/deserialization overhead — uses struct_to_dict internally.

        Args:
            source_record: Raw Java SourceRecord from BaseConnectChangeHandler.handleConnectBatch.

        Returns:
            A DebeziumRecord populated with all available fields.
        """
        from .connect_message import SourceRecordExtractor
        extractor = SourceRecordExtractor(
            source_record,
            conversion_config=conversion_config,
            topic_class_map=topic_class_map,
        )
        return cls(
            destination=extractor.destination,
            op=extractor.op or "r",
            before=extractor.before,
            after=extractor.after,
            ts_ms=extractor.ts_ms,
            key=str(extractor.key_dict) if extractor.key_dict else None,
            partition=extractor.partition,
            schema=None,
        )

    def is_create(self) -> bool:
        return self.op == "c"

    def is_update(self) -> bool:
        return self.op == "u"

    def is_delete(self) -> bool:
        return self.op == "d"

    def is_snapshot(self) -> bool:
        return self.op == "r"

    def get_current_state(self) -> Optional[Dict[str, Any]]:
        """Returns the most recent state: after for c/u/r, before for d."""
        if self.is_delete():
            return self.before
        return self.after

    def __repr__(self) -> str:
        return (
            f"DebeziumRecord("
            f"destination={self.destination!r}, "
            f"op={self.op!r}, "
            f"after={self.after!r})"
        )


def records_from_batch(java_records: List[Any]) -> List[DebeziumRecord]:
    """
    Converts a batch of raw Java ChangeEvent objects (JSON format) into DebeziumRecords.

    Args:
        java_records: List of raw Java ChangeEvent objects from handleJsonBatch.

    Returns:
        List of DebeziumRecord instances.
    """
    return [DebeziumRecord.from_java_record(r) for r in java_records]


def connect_records_from_batch(
    source_records: List[Any],
    conversion_config: Optional[ConversionConfig] = None,
    topic_class_map: Optional[Mapping[str, Type[Any]]] = None,
) -> List[DebeziumRecord]:
    """
    Converts a batch of raw Java SourceRecord objects (Connect format) into DebeziumRecords.
    No JSON serialization/deserialization overhead.

    Args:
        source_records: List of raw Java SourceRecord objects from handleConnectBatch.

    Returns:
        List of DebeziumRecord instances.
    """
    return [
        DebeziumRecord.from_source_record(
            r,
            conversion_config=conversion_config,
            topic_class_map=topic_class_map,
        )
        for r in source_records
    ]
