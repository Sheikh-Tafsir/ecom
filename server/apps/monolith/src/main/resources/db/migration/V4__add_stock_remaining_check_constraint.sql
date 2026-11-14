-- Prevent stock_items.remaining from going negative at the database level.
-- This is a safety net; application-level pessimistic locking handles concurrency.
ALTER TABLE stock_items
    ADD CONSTRAINT chk_stock_items_remaining_non_negative CHECK (remaining >= 0);
