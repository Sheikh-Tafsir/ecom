CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- User lookups
CREATE INDEX idx_user_name_trgm ON users USING gin(name gin_trgm_ops);

-- Product filtering and searching
CREATE INDEX idx_product_name_trgm ON products USING gin(name gin_trgm_ops);
CREATE INDEX idx_products_status_price ON products(status, price);

-- Foreign Key lookups for Relationships
CREATE INDEX idx_product_image_product_id ON product_images(product_id);

CREATE INDEX idx_orders_user_status ON orders(user_id, status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

CREATE INDEX idx_order_item_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

CREATE INDEX idx_reviews_product_rating ON reviews(product_id, rating);

CREATE INDEX idx_stock_item_stock_id ON stock_items(stock_id);
CREATE INDEX idx_stock_item_product_id ON stock_items(product_id);
