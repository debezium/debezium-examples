"""
models.py

Pydantic models for validating and type-checking Debezium CDC events.
These models mirror the Debezium JSON envelope (schema/payload) structure
and can also be built directly from a DebeziumRecord (datastructures.py).

Flow:
    Java ChangeEvent (JPype)
        → ConnectMessageExtractor  (connect_message.py)
            → DebeziumRecord        (datastructures.py)
                → DebeziumEventModel (this file, Pydantic validated)
"""

from typing import Any, Dict, List, Mapping, Optional, Type
from pydantic import BaseModel, model_validator

from .datastructures import DebeziumRecord
from .connect_message import ConversionConfig


class DebeziumSchemaField(BaseModel):
    """A single field in a Debezium schema definition."""
    field: Optional[str] = None
    type: Optional[str] = None
    optional: Optional[bool] = None
    name: Optional[str] = None
    version: Optional[int] = None


class DebeziumSchemaModel(BaseModel):
    """The schema section of a Debezium JSON envelope."""
    type: Optional[str] = None
    fields: Optional[List[DebeziumSchemaField]] = None
    name: Optional[str] = None
    optional: Optional[bool] = None
    version: Optional[int] = None


class DebeziumPayloadModel(BaseModel):
    """
    The payload section of a Debezium JSON envelope.

    op values:
        'c' = create (INSERT)
        'u' = update (UPDATE)
        'd' = delete (DELETE)
        'r' = read   (snapshot)
    """
    before: Optional[Dict[str, Any]] = None
    after: Optional[Dict[str, Any]] = None
    op: str
    ts_ms: Optional[int] = None
    transaction: Optional[Dict[str, Any]] = None
    source: Optional[Dict[str, Any]] = None

    @model_validator(mode="after")
    def check_op(self) -> "DebeziumPayloadModel":
        valid_ops = {"c", "u", "d", "r"}
        if self.op not in valid_ops:
            raise ValueError(f"Invalid op: {self.op!r}. Must be one of {valid_ops}")
        return self


class DebeziumEventModel(BaseModel):
    """
    Full validated Pydantic model for a Debezium CDC event.
    Combines destination, schema, and payload.
    """
    destination: str
    partition: Optional[int] = None
    key: Optional[str] = None
    schema: Optional[DebeziumSchemaModel] = None
    payload: DebeziumPayloadModel

    @classmethod
    def from_record(cls, record: DebeziumRecord) -> "DebeziumEventModel":
        """
        Builds a validated DebeziumEventModel from a DebeziumRecord data structure.

        Args:
            record: A DebeziumRecord instance (from datastructures.py).

        Returns:
            A validated DebeziumEventModel.
        """
        schema_data = dict(record.schema.raw) if record.schema else None
        payload_data = {
            "before": record.before,
            "after": record.after,
            "op": record.op,
            "ts_ms": record.ts_ms,
        }
        return cls(
            destination=record.destination,
            partition=record.partition,
            key=record.key,
            schema=DebeziumSchemaModel(**schema_data) if schema_data else None,
            payload=DebeziumPayloadModel(**payload_data),
        )

    @classmethod
    def from_java_record(cls, java_record) -> "DebeziumEventModel":
        """
        Convenience shortcut: builds a validated DebeziumEventModel directly from
        a raw Java ChangeEvent object (JSON format).

        Args:
            java_record: Raw Java ChangeEvent from pydbzengine handleJsonBatch.

        Returns:
            A validated DebeziumEventModel.
        """
        return cls.from_record(DebeziumRecord.from_java_record(java_record))

    @classmethod
    def from_source_record(
        cls,
        source_record,
        conversion_config: Optional[ConversionConfig] = None,
        topic_class_map: Optional[Mapping[str, Type[Any]]] = None,
    ) -> "DebeziumEventModel":
        """
        Builds a validated DebeziumEventModel from a raw Java SourceRecord (Connect format).
        No JSON serialization/deserialization overhead.

        Args:
            source_record: Raw Java SourceRecord from BaseConnectChangeHandler.handleConnectBatch.

        Returns:
            A validated DebeziumEventModel.
        """
        return cls.from_record(
            DebeziumRecord.from_source_record(
                source_record,
                conversion_config=conversion_config,
                topic_class_map=topic_class_map,
            )
        )

    def is_create(self) -> bool:
        return self.payload.op == "c"

    def is_update(self) -> bool:
        return self.payload.op == "u"

    def is_delete(self) -> bool:
        return self.payload.op == "d"

    def is_snapshot(self) -> bool:
        return self.payload.op == "r"

    def get_current_state(self) -> Optional[Dict[str, Any]]:
        """Returns the most recent state: after for c/u/r, before for d."""
        if self.is_delete():
            return self.payload.before
        return self.payload.after
