# Copyright Debezium Authors.
#
# Licensed under the Apache License version 2.0, available at
# http://www.apache.org/licenses/LICENSE-2.0

from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path


EVENTS_FILE = Path("/events/events.ndjson")


class EventHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length)
        EVENTS_FILE.parent.mkdir(parents=True, exist_ok=True)
        with EVENTS_FILE.open("ab") as events:
            events.write(body + b"\n")
        print(f"Received {len(body)} bytes", flush=True)
        self.send_response(204)
        self.end_headers()

    def log_message(self, format, *args):
        return


HTTPServer(("0.0.0.0", 9900), EventHandler).serve_forever()
