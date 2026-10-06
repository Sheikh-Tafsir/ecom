# E-Commerce Application User Guide

Welcome to the E-Commerce Platform user documentation. This guide covers the standard features, navigation, and workflows for end users, store managers, delivery staff, and system administrators.

---

## Table of Contents
1. [Getting Started & Authentication](#1-getting-started--authentication)
2. [Browsing & Searching Products](#2-browsing--searching-products)
3. [Cart, Checkout & Payments](#3-cart-checkout--payments)
4. [Order Tracking & Order History](#4-order-tracking--order-history)
5. [Real-time Customer Support (Chat)](#5-real-time-customer-support-chat)
6. [Real-time Notifications](#6-real-time-notifications)
7. [User Profile & Security Settings](#7-user-profile--security-settings)
8. [Admin & Store Operations](#8-admin--store-operations)
   - [Product & Inventory Management](#product--inventory-management)
   - [Order Lifecycle Management](#order-lifecycle-management)
   - [User & Role Management](#user--role-management)
   - [Content Management (CMS) & Banners](#content-management-cms--banners)
   - [Sales Analytics](#sales-analytics)
   - [CSV Reports](#csv-reports)

---

## 1. Getting Started & Authentication

### User Registration
1. Navigate to the **Sign Up** page.
2. Enter your Name, valid Email address, and Password (minimum 8 characters).
3. Check your email for a **6-digit One-Time Password (OTP)**.
4. Enter the OTP on the verification screen — your account is activated and you are logged in immediately.
5. Didn't receive the OTP? Use **Resend OTP** to request a new code.

### Logging In
- **Email & Password**: Enter registered email credentials to log in.
- **Google OAuth**: Click "Continue with Google" for instant single-sign-on.
- **Forgot Password**:
  1. Click "Forgot Password" and enter your email.
  2. Check your email for a password reset OTP.
  3. Enter the OTP and your new password (with confirmation) on the reset screen.
  4. On success, you are redirected to log in with the new password.

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
   - `ACCEPTED`: Store has confirmed and accepted the order.
   - `SHIPPED`: Dispatched to delivery carrier.
   - `DELIVERED`: Successfully handed over to customer.
   - `COMPLETED`: Order fully resolved after delivery.
   - `CANCELLED`: Cancelled by the customer (only possible while `PENDING`).
   - `REJECTED`: Rejected by the store (e.g., out of stock or inactivity timeout).
   - `LOST`: Package lost during delivery.
   - `RETURNED`: Package returned after delivery.
   - `REFUNDED`: Refund issued (after `LOST` or `RETURNED`).

---

## 5. Real-time Customer Support (Chat)

- Click the floating **Chat** widget at the bottom right.
- Connect directly with store support agents.
- Messages are delivered in real time via Socket.IO — missed messages are automatically recovered on reconnect.
- Supports both **direct chats** (user ↔ agent) and **group chats**.

---

## 6. Real-time Notifications

- Order status changes are pushed instantly to your browser via **Server-Sent Events (SSE)** — no page refresh required.
- Notifications appear automatically when an admin updates your order status.

---

## 7. User Profile & Security Settings

- Update avatar picture and profile name.
- View active permissions and assigned roles.
- Change password securely with active session revocation (all existing tokens are invalidated on password change).

---

## 8. Admin & Store Operations

### Product & Inventory Management
- **Add/Edit Products**: Upload high-resolution images, set category relations, base price, and description.
- **Stock Batch Ingestion**: Record supplier purchase batches with cost price and quantity — tracked using FIFO inventory costing.

### Order Lifecycle Management
- Filter orders by customer, status, date range, payment status, or product name.
- Update order statuses following the allowed transition flow:
  ```
  PENDING → ACCEPTED → SHIPPED → DELIVERED → COMPLETED
                              └→ LOST      → REFUNDED
                              └→ RETURNED  → REFUNDED
  PENDING → CANCELLED  (by customer only)
  PENDING / ACCEPTED → REJECTED  (by store)
  ACCEPTED → REFUNDED  (direct refund before shipping)
  ```
- Automated inactivity scheduler auto-rejects stale `PENDING` orders and notifies admins.
- **Real-time order update notifications** are pushed to the relevant user via SSE when their order status changes.

### User & Role Management
- **Role Assignment**: Assign granular permissions to staff accounts (`ADMIN_ACCESS`, `DELIVERY_MAN_ACCESS`, `CMS_ACCESS`, etc.)
- **User Moderation**: Suspend, ban, or delete abusive accounts with instant cache eviction.

### Content Management (CMS) & Banners
- Create interactive promotional banners with custom click-through targets.
- Publish store blogs, news, and maintain FAQ sections.

### Sales Analytics
- Navigate to **Sales** to view paginated sales records.
- Filter by date range, product ID, or product name.
- Sales records are automatically created when an order is delivered.

### CSV Reports
- Navigate to **Reports**.
- Export streaming CSV reports for **Users**, **Orders**, and **Sales/Profits** with custom date range filters.
- Reports stream directly to the browser as a downloadable `.csv` file — no server-side buffering.

