-- Payload/type validation status recorded per event (non-blocking, SQLite dialect).
ALTER TABLE events ADD COLUMN event_type_validation varchar(16);
ALTER TABLE events ADD COLUMN validation_reason varchar(512);
