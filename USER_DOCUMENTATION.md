# E-Commerce Application User Guide

Welcome to the E-Commerce Platform user documentation. This guide covers the standard features, navigation, and workflows for end users, store managers, delivery staff, and system administrators.

---

## Table of Contents
1. [Getting Started & Authentication](#1-getting-started--authentication)
2. [Browsing & Searching Products](#2-browsing--searching-products)
3. [Cart, Checkout & Payments](#3-cart-checkout--payments)
4. [Order Tracking & Order History](#4-order-tracking--order-history)
5. [Real-time Customer Support (Chat)](#5-real-time-customer-support-chat)
6. [User Profile & Security Settings](#6-user-profile--security-settings)
7. [Admin & Store Operations](#7-admin--store-operations)
   - [Product & Inventory Management](#product--inventory-management)
   - [Order Lifecycle Management](#order-lifecycle-management)
   - [User & Role Management](#user--role-management)
   - [Content Management (CMS) & Banners](#content-management-cms--banners)
   - [CSV Reports & Analytics](#csv-reports--analytics)

---

## 1. Getting Started & Authentication

### User Registration
1. Navigate to the **Sign Up** page.
2. Enter your Name, valid Email address, and Password (minimum 8 characters).
3. Check your email for a **6-digit One-Time Password (OTP)**.
4. Enter the OTP in the verification screen to activate your account.

### Logging In
- **Email & Password**: Enter registered email credentials to log in.
- **Google OAuth**: Click "Continue with Google" for instant single-sign-on.
- **Forgot Password**: Click "Forgot Password", enter your email to receive a password reset OTP, verify, and set a new password.

---

## 2. Browsing & Searching Products

- **Homepage Carousel & Categories**: The home page highlights promotions, top-rated products, and category shortcuts.
- **Search & Filters**:
  - Filter products by category, availability, rating, or date range.
  - Search by keywords with instant debounce and pagination.
- **Product Details**: View product images, descriptions, pricing, real-time stock availability, and verified customer reviews.

---

## 3. Cart, Checkout & Payments

### Adding Items to Cart
1. Select quantity and click **Add to Cart**.
2. Review selected items in the slide-over cart drawer.

### Checkout Flow
1. Proceed to **Checkout**.
2. Provide your recipient name, contact phone number, and delivery shipping address.
3. Choose your preferred Payment Method:
   - **Cash on Delivery (COD)**: Pay upon package delivery.
   - **bKash Payment Gateway**: Pay securely through the bKash payment gateway. Upon completing the transaction, you will be redirected to the order confirmation page with your transaction ID.

---

## 4. Order Tracking & Order History

1. Navigate to **My Orders** in the user dropdown menu.
2. View detailed statuses for each order:
   - `PENDING`: Order placed, awaiting store confirmation.
   - `CONFIRMED`: Store has acknowledged and prepared the order.
   - `PROCESSING`: Items packed in warehouse.
   - `SHIPPED`: Handed over to delivery carrier.
   - `DELIVERED`: Successfully handed over to customer.
   - `CANCELLED` / `REJECTED`: Cancelled by user or rejected due to inactivity.

---

## 5. Real-time Customer Support (Chat)

- Click the floating **Chat** widget at the bottom right.
- Connect directly with store support agents.
- Real-time message synchronization powered by Socket.io and Redis.

---

## 6. User Profile & Security Settings

- Update avatar picture and profile name.
- View active permissions and assigned roles.
- Change password securely with active session revocation.

---

## 7. Admin & Store Operations

### Product & Inventory Management
- **Add/Edit Products**: Upload high-resolution images, set category relations, base price, and description.
- **Stock Batch Ingestion**: Record supplier purchase batches with cost price, quantity, and track FIFO inventory.

### Order Lifecycle Management
- Filter orders by customer, status, date range, or payment status.
- Update order statuses (`CONFIRMED`, `SHIPPED`, `DELIVERED`, `RETURNED`).
- Automated inactivity scheduler notifies admins of stale pending orders and auto-rejects unconfirmed orders.

### User & Role Management
- **Role Assignment**: Assign granular permissions to staff accounts (`ADMIN_ACCESS`, `DELIVERY_MAN_ACCESS`, `CMS_ACCESS`, etc.).
- **User Moderation**: Suspend, ban, or delete abusive accounts with instant cache eviction.

### Content Management (CMS) & Banners
- Create interactive promotional banners with custom click-through targets.
- Publish store blogs, news, and maintain FAQ sections.

### CSV Reports & Analytics
- Navigate to **Reports**.
- Export streaming CSV reports for **Users**, **Orders**, and **Sales/Profits** with custom date filters.
