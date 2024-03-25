CREATE TABLE IF NOT EXISTS fusion_order (
	id VARCHAR(36) PRIMARY KEY,
	description VARCHAR(100),
	name VARCHAR(50),
	creation_date VARCHAR(100),
	last_update_date VARCHAR(100),
	status VARCHAR(100));

CREATE TABLE IF NOT EXISTS fusion_order_product (
	order_id VARCHAR(36),
	product_id VARCHAR(36),
	PRIMARY KEY (order_id, product_id),
	FOREIGN KEY (order_id) REFERENCES fusion_order (id)
);
