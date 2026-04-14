ALTER TABLE products ADD COLUMN organization_id BIGINT REFERENCES organizations(id);

CREATE INDEX idx_products_organization ON products(organization_id);
