CREATE TABLE roles
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(63),
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    phone      VARCHAR(11),
    gender     VARCHAR(31),
    image      VARCHAR(255),
    status     VARCHAR(31)  NOT NULL DEFAULT 'NOT_VERIFIED',
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    version    INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- Users, Roles Join Table
CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE products
(
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255)   NOT NULL,
    description  TEXT,
    price        DECIMAL(19, 2) NOT NULL CHECK (price >= 0),
    quantity     INT            NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    status       VARCHAR(31)    NOT NULL DEFAULT 'COMING_SOON',
    rating       DECIMAL(3, 2)  NOT NULL DEFAULT 0.00 CHECK (rating BETWEEN 0 AND 5),
    review_count BIGINT         NOT NULL DEFAULT 0,
    deleted      BOOLEAN        NOT NULL DEFAULT FALSE,
    version      INT            NOT NULL DEFAULT 0,
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_products_name UNIQUE (name)
);

CREATE TABLE categories
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_categories_name UNIQUE (name)
);

-- Products, Categories Join Table
CREATE TABLE product_categories
(
    product_id  BIGINT NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    category_id BIGINT NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, category_id)
);

CREATE TABLE product_images
(
    id         BIGSERIAL PRIMARY KEY,
    image      VARCHAR(255) NOT NULL,
    product_id BIGINT       NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT uq_product_images_product_image UNIQUE (product_id, image)
);

CREATE TABLE stocks
(
    id         BIGSERIAL PRIMARY KEY,
    total_cost DECIMAL(19, 2) NOT NULL DEFAULT 0.00 CHECK (total_cost >= 0),
    version    INT            NOT NULL DEFAULT 0,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stock_items
(
    id             BIGSERIAL PRIMARY KEY,
    stock_id       BIGINT         NOT NULL REFERENCES stocks (id) ON DELETE CASCADE,
    product_id     BIGINT         NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    quantity       INT            NOT NULL CHECK (quantity > 0),
    purchase_price DECIMAL(19, 2) NOT NULL CHECK (purchase_price >= 0),
    remaining      INT            NOT NULL CHECK (remaining >= 0 AND remaining <= quantity),
    version        INT            NOT NULL DEFAULT 0,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users (id) ON DELETE RESTRICT,
    total_price     DECIMAL(19, 2) NOT NULL DEFAULT 0.00 CHECK (total_price >= 0),
    status          VARCHAR(31)    NOT NULL DEFAULT 'PENDING',
    name            VARCHAR(63)    NOT NULL,
    phone           VARCHAR(11)    NOT NULL,
    address         VARCHAR(255)   NOT NULL,
    payment_method  VARCHAR(20)    NOT NULL DEFAULT 'CASH_ON_DELIVERY',
    version         INT            NOT NULL DEFAULT 0,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items
(
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id BIGINT         NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    quantity   INT            NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(19, 2) NOT NULL CHECK (unit_price >= 0),
    CONSTRAINT uq_order_items_order_product UNIQUE (order_id, product_id)
);

CREATE TABLE payments
(
    id                      BIGSERIAL PRIMARY KEY,
    order_id                BIGINT         NOT NULL REFERENCES orders (id) ON DELETE RESTRICT,
    payment_intent_id       VARCHAR(255),
    transaction_id          VARCHAR(255),
    merchant_invoice_number VARCHAR(255),
    amount                  DECIMAL(19, 2) NOT NULL CHECK (amount >= 0),
    status                  VARCHAR(31)    NOT NULL DEFAULT 'PENDING',
    version                 INT            NOT NULL DEFAULT 0,
    created_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reviews
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    product_id BIGINT    NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    rating     INT       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    version    INT       NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reviews_user_product UNIQUE (user_id, product_id)
);

CREATE TABLE sales
(
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT         NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    quantity   INT            NOT NULL CHECK (quantity > 0),
    profit     DECIMAL(19, 2) NOT NULL CHECK (profit >= 0),
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

