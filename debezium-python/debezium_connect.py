"""
debezium_connect.py

Extends pydbzengine with Connect mode support (EngineFormat.CONNECT).

This module provides:
- BaseConnectChangeHandler: Abstract handler for receiving raw Java SourceRecord objects
- DebeziumConnectEngine: Engine wrapper for Connect format (zero JSON overhead)
- _create_connect_consumer: Factory for creating JPype-based Java consumer

Connect mode eliminates JSON serialization/deserialization overhead by passing raw
Java SourceRecord objects to Python handlers via JPype. Use SourceRecordExtractor
(from pydebeziumai.connect_message) to convert these records to native Python dicts.
"""

import traceback
from abc import ABC
from functools import cached_property
from typing import Union, Any, Dict


class BaseConnectChangeHandler(ABC):
    """
    Abstract base class for handlers that receive Debezium events in Connect format.

    Unlike BasePythonChangeHandler (which gets JSON strings), this handler receives
    raw Java SourceRecord objects — no serialization or deserialization happens at all.

    Use DebeziumConnectEngine + BaseConnectChangeHandler to eliminate JSON overhead.
    Use SourceRecordExtractor (pydebeziumai.connect_message) to convert records to Python.
    """

    def handleConnectBatch(self, records):
        """
        Handles a batch of raw Java SourceRecord objects from Debezium Connect format.

        Args:
            records: A list of raw Java SourceRecord objects (via JPype).

        Raises:
            NotImplementedError: If not implemented by subclass.
        """
        raise NotImplementedError(
            "Please implement BaseConnectChangeHandler.handleConnectBatch!"
        )


def _create_connect_consumer():
    """
    Factory function to create PythonConnectConsumer class after JVM is started.
    This avoids JPype trying to load Java classes at module import time.
    """
    import jpype
    
    @jpype.JImplements("io/debezium/engine/DebeziumEngine$ChangeConsumer")
    class PythonConnectConsumer:
        """
        Python implementation of DebeziumEngine.ChangeConsumer for EngineFormat.CONNECT.

        Records passed to handleBatch are raw Java SourceRecord objects.
        No JSON serialization/deserialization happens — zero overhead.
        The handler's handleConnectBatch receives these raw Java records which
        can be converted to Python dicts via SourceRecordExtractor (connect_message.py).
        """

        def __init__(self):
            self.handler = None

        @jpype.JOverride
        def handleBatch(self, records, committer):
            try:
                self.handler.handleConnectBatch(records=records)
                for e in records:
                    committer.markProcessed(e)
                committer.markBatchFinished()
            except Exception as e:
                print("ERROR: failed to consume connect events in python")
                print(str(e))
                print(traceback.format_exc())
                # Interrupt the Debezium engine thread
                JavaLangThread = jpype.JClass("java.lang.Thread")
                JavaLangThread.currentThread().interrupt()

        @jpype.JOverride
        def supportsTombstoneEvents(self):
            return True

        def set_change_handler(self, handler: BaseConnectChangeHandler):
            self.handler = handler
    
    return PythonConnectConsumer


class DebeziumConnectEngine:
    """
    Debezium embedded engine using Connect format (EngineFormat.CONNECT).

    Key difference from DebeziumJsonEngine:
    - No JSON serialization/deserialization — records are raw Java SourceRecord objects.
    - Lower latency and memory usage.
    - Use SourceRecordExtractor or DebeziumRecord.from_source_record() to convert records.
    """

    def __init__(
        self,
        properties: Union[Dict[str, Any], "Properties"],
        handler: BaseConnectChangeHandler,
    ):
        self.properties = properties
        if self.properties is None:
            raise ValueError("Please provide debezium config properties!")
        if handler is None:
            raise ValueError(
                "Please provide a BaseConnectChangeHandler subclass!"
            )
        self._handler = handler
        self._consumer_class = None

    @cached_property
    def consumer(self):
        # Lazy create consumer class after JVM starts
        if self._consumer_class is None:
            self._consumer_class = _create_connect_consumer()
        return self._consumer_class()

    @cached_property
    def engine(self):
        # Import from pydbzengine JVM bridge
        from pydbzengine._jvm import DebeziumEngine, Properties
        import jpype

        # Access Connect format directly via JPype since pydbzengine doesn't expose it
        try:
            ConnectFormat = jpype.JClass("io.debezium.embedded.Connect")
            print("✓ Successfully loaded io.debezium.embedded.Connect format")
        except Exception as e:
            raise RuntimeError(
                f"Connect format is not available: {e}. "
                "You need Debezium 3.0+ with io.debezium.embedded.Connect available. "
                "Ensure Maven dependencies include debezium-embedded properly and are "
                "copied to pydbzengine/debezium/libs/ directory."
            )

        java_props = Properties()
        if isinstance(self.properties, dict):
            for key, value in self.properties.items():
                java_props.setProperty(str(key), str(value))
        else:
            java_props = self.properties

        try:
            # Create engine using Connect format directly
            return (
                DebeziumEngine.create(ConnectFormat)
                .using(java_props)
                .notifying(self.consumer)
                .build()
            )
        except Exception as e:
            raise RuntimeError(
                f"Failed to create Debezium Connect engine: {e}. "
                "Make sure io.debezium.embedded.Connect is in your classpath."
            )

    def run(self):
        self.consumer.set_change_handler(self._handler)
        self.engine.run()

    def close(self):
        if self.engine:
            try:
                self.engine.close()
            except Exception:
                pass

    def interrupt(self):
        self.close()
