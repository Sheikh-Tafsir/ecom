-- Products
CREATE INDEX IF NOT EXISTS idx_products_status_created_at ON products(status, created_at DESC);

-- Product Images
CREATE INDEX IF NOT EXISTS idx_product_images_product_id_id ON product_images (product_id, id ASC);

-- Product Categories
CREATE INDEX IF NOT EXISTS idx_product_categories_category_id ON product_categories(category_id);

-- Orders
CREATE INDEX IF NOT EXISTS idx_orders_user_id_created_at ON orders(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_status_created_at ON orders(status, created_at);

-- Order Items
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

-- Order History
CREATE INDEX IF NOT EXISTS idx_order_status_history_order_id ON order_history(order_id);

-- Stock
CREATE INDEX IF NOT EXISTS idx_stock_items_stock_id ON stock_items(stock_id);
CREATE INDEX IF NOT EXISTS idx_stock_items_product_id ON stock_items(product_id);
CREATE INDEX IF NOT EXISTS idx_stock_items_product_remaining ON stock_items(product_id, remaining) WHERE remaining > 0;

-- Payments
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX IF NOT EXISTS idx_payments_payment_intent_id ON payments(payment_intent_id);

-- Sales
CREATE INDEX IF NOT EXISTS idx_sales_order_id ON sales(order_id);
CREATE INDEX IF NOT EXISTS idx_sales_product_id ON sales(product_id);

-- Reviews & Blogs
CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_blogs_status_created_at ON blogs(status, created_at DESC);

-- Chat (server_chat schema)
CREATE INDEX IF NOT EXISTS idx_chat_participants_user ON chat_participants (user_id);
CREATE INDEX IF NOT EXISTS idx_chat_participants_chat ON chat_participants (chat_id);
CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON messages (chat_id, id DESC);
CREATE INDEX IF NOT EXISTS idx_messages_chat_created_at ON messages (chat_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_message_receipts_message ON message_receipts (message_id);
