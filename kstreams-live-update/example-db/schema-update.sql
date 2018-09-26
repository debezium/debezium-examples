# Switch to this database
USE inventory;

CREATE TABLE stations (
  id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

INSERT INTO stations VALUES (10001, 'Hamburg');
INSERT INTO stations VALUES (10002, 'Dresden');
INSERT INTO stations VALUES (10003, 'Berlin');
INSERT INTO stations VALUES (10004, 'Munich');
INSERT INTO stations VALUES (10005, 'Cologne');
INSERT INTO stations VALUES (10006, 'Bremen');
INSERT INTO stations VALUES (10007, 'Hannover');
INSERT INTO stations VALUES (10008, 'Stuttgart');
INSERT INTO stations VALUES (10009, 'Dortmund');
INSERT INTO stations VALUES (10010, 'Leipzig');

CREATE TABLE temperature_measurements (
  id BIGINT NOT NULL AUTO_INCREMENT,
  station_id BIGINT NOT NULL,
  value DECIMAL(6,3) NOT NULL,
  ts TIMESTAMP NOT NULL,
  PRIMARY KEY (id)
);
ALTER TABLE temperature_measurements ADD CONSTRAINT fk_temperature_measurements_station_id FOREIGN KEY (station_id) REFERENCES inventory.stations(id);
