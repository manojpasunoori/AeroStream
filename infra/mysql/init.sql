CREATE TABLE flights (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flight_number VARCHAR(20) NOT NULL,
    origin VARCHAR(50),
    destination VARCHAR(50),
    departure_time DATETIME,
    arrival_time DATETIME,
    status VARCHAR(20)
);

CREATE TABLE crew (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    role VARCHAR(50),
    flight_id BIGINT,
    FOREIGN KEY (flight_id) REFERENCES flights(id)
);

CREATE TABLE delays (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flight_id BIGINT,
    reason VARCHAR(255),
    delay_minutes INT,
    FOREIGN KEY (flight_id) REFERENCES flights(id)
);

CREATE TABLE kpi_metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_name VARCHAR(100),
    metric_value DOUBLE,
    calculated_at DATETIME
);
