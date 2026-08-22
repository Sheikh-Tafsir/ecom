CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_stock_items_product_id ON stock_items(product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_blogs_status ON blogs(status);

-- chat --
CREATE INDEX IF NOT EXISTS idx_chat_participants_user ON chat_participants (user_id);
CREATE INDEX IF NOT EXISTS idx_chat_participants_chat ON chat_participants (chat_id);
CREATE INDEX IF NOT EXISTS idx_messages_chat ON messages (chat_id);
CREATE INDEX IF NOT EXISTS idx_message_receipts_message ON message_receipts (message_id);