"""
pydebeziumai - Python utilities for Debezium CDC events

Provides extractors, data structures, and Pydantic models for working with
Debezium change events in both JSON and Connect formats.
"""

from .connect_message import (
    ConversionConfig,
    ConnectMessageExtractor,
    SourceRecordExtractor,
    print_record_info,
    struct_to_dict,
    extract_all,
    extract_connect_all,
)
from .datastructures import (
    DebeziumRecord,
    DebeziumSchema,
    records_from_batch,
    connect_records_from_batch,
)
from .models import (
    DebeziumEventModel,
    DebeziumPayloadModel,
    DebeziumSchemaModel,
    DebeziumSchemaField,
)

__all__ = [
    # Extractors
    "ConversionConfig",
    "ConnectMessageExtractor",
    "SourceRecordExtractor",
    "print_record_info",
    "struct_to_dict",
    "extract_all",
    "extract_connect_all",
    # Data structures
    "DebeziumRecord",
    "DebeziumSchema",
    "records_from_batch",
    "connect_records_from_batch",
    # Pydantic models
    "DebeziumEventModel",
    "DebeziumPayloadModel",
    "DebeziumSchemaModel",
    "DebeziumSchemaField",
]
