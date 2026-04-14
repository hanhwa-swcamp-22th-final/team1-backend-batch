CREATE TABLE IF NOT EXISTS daily_bin_snapshot (
    daily_bin_snapshot_id BIGINT NOT NULL AUTO_INCREMENT,
    snapshot_date DATE NOT NULL,
    seller_id VARCHAR(50) NOT NULL,
    warehouse_id VARCHAR(50) NOT NULL,
    occupied_bin_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (daily_bin_snapshot_id),
    CONSTRAINT uk_daily_bin_snapshot_date_seller_warehouse
        UNIQUE (snapshot_date, seller_id, warehouse_id)
);

CREATE TABLE IF NOT EXISTS monthly_fee_snapshot (
    monthly_fee_snapshot_id BIGINT NOT NULL AUTO_INCREMENT,
    billing_month VARCHAR(7) NOT NULL,
    seller_id VARCHAR(50) NOT NULL,
    warehouse_id VARCHAR(50) NOT NULL,
    storage_unit_price DECIMAL(19, 2) NOT NULL,
    pick_unit_price DECIMAL(19, 2) NOT NULL,
    pack_unit_price DECIMAL(19, 2) NOT NULL,
    captured_at DATETIME(6) NOT NULL,
    PRIMARY KEY (monthly_fee_snapshot_id),
    CONSTRAINT uk_monthly_fee_snapshot_month_seller_warehouse
        UNIQUE (billing_month, seller_id, warehouse_id)
);

CREATE TABLE IF NOT EXISTS seller_monthly_billing (
    seller_monthly_billing_id BIGINT NOT NULL AUTO_INCREMENT,
    billing_month VARCHAR(7) NOT NULL,
    seller_id VARCHAR(50) NOT NULL,
    warehouse_id VARCHAR(50) NOT NULL,
    occupied_bin_days INT NOT NULL,
    average_occupied_bins DECIMAL(19, 2) NOT NULL,
    storage_fee DECIMAL(19, 2) NOT NULL,
    pick_count INT NOT NULL,
    picking_fee DECIMAL(19, 2) NOT NULL,
    pack_count INT NOT NULL,
    packing_fee DECIMAL(19, 2) NOT NULL,
    total_fee DECIMAL(19, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    calculated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (seller_monthly_billing_id),
    CONSTRAINT uk_seller_monthly_billing_month_seller_warehouse
        UNIQUE (billing_month, seller_id, warehouse_id)
);

CREATE TABLE IF NOT EXISTS billing_dispatch_history (
    billing_dispatch_history_id BIGINT NOT NULL AUTO_INCREMENT,
    billing_month VARCHAR(7) NOT NULL,
    seller_id VARCHAR(50) NOT NULL,
    topic_name VARCHAR(100) NOT NULL,
    dispatch_status VARCHAR(20) NOT NULL,
    dispatched_at DATETIME(6) NULL,
    error_message VARCHAR(500) NULL,
    PRIMARY KEY (billing_dispatch_history_id),
    INDEX idx_billing_dispatch_history_month (billing_month),
    INDEX idx_billing_dispatch_history_month_seller (billing_month, seller_id)
);
