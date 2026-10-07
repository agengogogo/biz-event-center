-- Business event type registry: definitions and their payload JSON Schemas.
-- The catalog is global (shared across parks); presets are seeded below.

CREATE TABLE event_type (
    type             varchar(128) PRIMARY KEY,
    name             varchar(128) NOT NULL,
    source_category  varchar(32) NOT NULL,
    default_severity varchar(16) NOT NULL,
    payload_schema   text,
    enabled          boolean NOT NULL DEFAULT true,
    version          integer NOT NULL DEFAULT 1,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_event_type_category ON event_type (source_category, enabled);

INSERT INTO event_type (type, name, source_category, default_severity, payload_schema) VALUES
    ('iot.temperature', '温度上报', 'IOT', 'INFO', '{"type":"object","properties":{"value":{"type":"number"},"unit":{"type":"string"}},"required":["value"]}'),
    ('iot.humidity', '湿度上报', 'IOT', 'INFO', '{"type":"object","properties":{"value":{"type":"number"},"unit":{"type":"string"}},"required":["value"]}'),
    ('iot.device.offline', '设备离线', 'IOT', 'MAJOR', '{"type":"object","properties":{"deviceId":{"type":"string"},"lastSeen":{"type":"string"}},"required":["deviceId"]}'),
    ('iot.device.fault', '设备故障', 'IOT', 'MAJOR', '{"type":"object","properties":{"deviceId":{"type":"string"},"code":{"type":"string"},"message":{"type":"string"}},"required":["deviceId","code"]}'),
    ('access.granted', '通行放行', 'ACCESS', 'INFO', '{"type":"object","properties":{"personId":{"type":"string"},"gateId":{"type":"string"}},"required":["personId","gateId"]}'),
    ('access.denied', '通行拒绝', 'ACCESS', 'WARNING', '{"type":"object","properties":{"personId":{"type":"string"},"gateId":{"type":"string"},"attempts":{"type":"integer"}},"required":["personId","gateId"]}'),
    ('access.tailgating', '尾随告警', 'ACCESS', 'MAJOR', '{"type":"object","properties":{"gateId":{"type":"string"},"personId":{"type":"string"}},"required":["gateId"]}'),
    ('energy.consumption.spike', '能耗尖峰', 'ENERGY', 'WARNING', '{"type":"object","properties":{"meterId":{"type":"string"},"kwh":{"type":"number"},"threshold":{"type":"number"}},"required":["meterId","kwh"]}'),
    ('facility.equipment.alarm', '设施设备告警', 'ENERGY', 'MAJOR', '{"type":"object","properties":{"equipmentId":{"type":"string"},"code":{"type":"string"},"message":{"type":"string"}},"required":["equipmentId","code"]}'),
    ('parking.enter', '车辆入场', 'PARKING', 'INFO', '{"type":"object","properties":{"plate":{"type":"string"},"lotId":{"type":"string"},"spaceId":{"type":"string"}},"required":["plate","lotId"]}'),
    ('parking.exit', '车辆出场', 'PARKING', 'INFO', '{"type":"object","properties":{"plate":{"type":"string"},"lotId":{"type":"string"},"fee":{"type":"number"}},"required":["plate","lotId"]}'),
    ('parking.full', '车位已满', 'PARKING', 'WARNING', '{"type":"object","properties":{"lotId":{"type":"string"},"occupied":{"type":"integer"},"capacity":{"type":"integer"}},"required":["lotId"]}'),
    ('security.intrusion', '入侵告警', 'SECURITY', 'CRITICAL', '{"type":"object","properties":{"cameraId":{"type":"string"},"zone":{"type":"string"}},"required":["cameraId"]}'),
    ('security.face.match', '人脸命中', 'SECURITY', 'MAJOR', '{"type":"object","properties":{"cameraId":{"type":"string"},"personId":{"type":"string"},"confidence":{"type":"number"}},"required":["cameraId","personId"]}'),
    ('thirdparty.workorder.created', '第三方工单创建', 'THIRD_PARTY', 'INFO', '{"type":"object","properties":{"orderId":{"type":"string"},"system":{"type":"string"}},"required":["orderId"]}'),
    ('manual.inspection', '人工巡检', 'MANUAL', 'INFO', '{"type":"object","properties":{"inspector":{"type":"string"},"note":{"type":"string"}},"required":["inspector"]}');
