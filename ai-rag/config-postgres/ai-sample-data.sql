CREATE SCHEMA ai;
SET search_path TO ai;

CREATE TABLE documents (
  id VARCHAR(64) PRIMARY KEY,
  metadata JSON,
  text TEXT
);
ALTER TABLE documents REPLICA IDENTITY FULL;
