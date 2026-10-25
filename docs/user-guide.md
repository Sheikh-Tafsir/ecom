# User Guide

How to use the store, by role. This describes the application as of 2026-10-25. Items marked **Known issue** are bugs that are tracked in the [audit](../.context/2026-10-25-system-audit.md) and will change once they are fixed.

| Role | Who | Can do |
|---|---|---|
| Guest | Not logged in | Browse products, blogs and FAQs |
| Customer | Any registered user | Everything a guest can, plus cart, checkout, own orders, reviews, chat, profile |
| Delivery man | `DELIVERY_MAN_ACCESS` | View shipped/delivered/returned orders; mark orders Delivered or Returned |
| Admin | `ADMIN_ACCESS` | All orders and status changes, view users/stock, CSV reports, blogs, bKash payment query and refund (API) |
| Super admin | `SUPER_ADMIN_ACCESS` | Everything, including products, stock batches, sales, banners, FAQs, roles, user status and role changes |

---

## Table of Contents
1. [Getting Started & Authentication](#1-getting-started--authentication)
2. [Browsing Products](#2-browsing-products)
3. [Cart, Checkout & Payments](#3-cart-checkout--payments)
4. [Orders](#4-orders)
5. [Reviews](#5-reviews)
6. [Chat](#6-chat)
7. [Notifications](#7-notifications)
8. [Profile & Account](#8-profile--account)
9. [Delivery Staff](#9-delivery-staff)
10. [Admin & Store Operations](#10-admin--store-operations)

---

## 1. Getting Started & Authentication

### Sign up
1. Open **Sign Up**.
2. Enter your name (2–31 characters), email, password and password confirmation. Passwords must be **8–15 characters**.
3. A **6-digit code** is emailed to you. It is valid for **5 minutes**.
4. Enter the code. Your account is activated and you are logged in.
   - After **3 wrong attempts** the code stops working.
   - You can request a new code (**Resend**) only after the previous one has expired.
   - Accounts that are not verified within **24 hours** are deleted automatically.

### Log in
- **Email and password.**
- **Google:** under "or continue with", choose **Google Account**.

### Forgot password
1. On the login page click **Forgot?** and enter your email.
2. Enter the 6-digit code from the email, plus your new password and its confirmation.
3. You are sent back to the login page. Every session on every device is signed out.

> **Known issue (F-55):** the **Resend** button on the reset screen doesn't work. Start the forgot-password flow again to get a new code.

---

## 2. Browsing Products

- **Home page:** a promotional banner slider, a call-to-action and the FAQs.
- **Products page:**
  - **Search** by keyword (results update as you type).
  - **Filter** by category.
  - **Sort** by Newest, Oldest, Price (low → high / high → low) or Name A–Z.
  - Results are paginated.
- **Product details:** images, description, price, current stock and customer reviews. The stock figure is loaded when the page opens; refresh the page to update it.
- **Blogs and FAQs** are public. The **About**, **Privacy** and **Terms** pages are linked from the site.

> **Known issue (F-13):** the product list only shows products added since 1 January of the current year.

---

## 3. Cart, Checkout & Payments

### Cart
1. On a product page, choose a quantity and click **Add to Cart**. You can't add more than the stock that is available.
2. Open the cart with the **cart icon** in the navigation bar (the `/cart` page). The cart is saved in your browser, so it survives a reload but not a different device.

### Checkout
1. Go to **Checkout**.
2. Enter a **phone number (exactly 11 digits)** and a **delivery address** (5–1023 characters). The order uses your account name.
3. Choose a payment method:
   - **Cash on Delivery:** pay the delivery person. You are taken to your order page.
   - **bKash:** you are sent to bKash to pay. Afterwards you land on a **Payment Success** page showing the transaction ID, or on a **Payment Failed** page with a retry option.

The total you pay is always calculated by the store from current prices. If a price changed since you added the item, the cart may show an outdated total.

Stock is reserved as soon as the order is placed.

> **Known issues:**
> - **F-03:** bKash payments may not complete in the current deployment.
> - **F-07:** if starting the bKash payment fails, clicking **Place order** again creates a second order. Go to **Orders** and pay or cancel the first one instead.

---

## 4. Orders

Open **Orders** in the navigation bar. Admins find it under **Operations**. Customers see their own orders, filtered by status (**Pending** by default), date range and product name.

Open an order to see its items, total, payment status and **status-history timeline**.

| Status | Meaning |
|---|---|
| `PENDING` | Placed, waiting for the store |
| `ACCEPTED` | Confirmed by the store (prepaid bKash orders are accepted automatically once payment succeeds) |
| `SHIPPED` | Handed to delivery |
| `DELIVERED` | Delivered to you (cash-on-delivery orders are marked paid at this point) |
| `COMPLETED` | Closed by the store after delivery |
| `CANCELLED` | Cancelled by you while pending, or by the store before shipping |
| `REJECTED` | Rejected by the store, or automatically after **2 days** pending |
| `LOST` | Lost in delivery |
| `RETURNED` | Came back **during delivery** (e.g. refused or undeliverable). A delivered order can't be returned through the system. |
| `REFUNDED` | Refunded (after Lost/Returned, or for an accepted order before shipping) |

**Cancelling:** open a **Pending** order and cancel it. Once the store has accepted it, contact the store (e.g. via chat).

---

## 5. Reviews

Logged-in users can write **one review per product**: a rating and a comment of up to 1023 characters. You can edit or delete your own review. Admins can delete any review. Reviews are not limited to verified buyers.

---

## 6. Chat

Open **Chats** in the navigation bar.

- **Start a direct chat** by searching for any active user by name.
- **Messages:**
  - Messages arrive in real time.
  - When you reconnect, messages you missed are loaded automatically.
  - Messages can be up to 4096 characters.
  - You can send up to 20 messages per 10 seconds.
- **Presence and typing:** see who is online (or when they were last seen) and when someone is typing.
- **Block / unblock** a user from the chat info panel. Blocked users can't message you directly.
- **Group chats:**
  - A group needs at least 2 other members, and the creator becomes the group admin.
  - The group admin can rename the group and change its members.
  - In practice only admins can create groups, because the member picker lists all users and that list is admin-only.

> **Known issues:**
> - **F-56:** sending images in chat currently fails.
> - **F-14:** the first message of a brand-new conversation may not appear for the other person until they refresh.

---

## 7. Notifications

Notifications arrive over the same live connection as chat. They are currently sent **only to admins**, for example "orders pending for more than 1 day", once a day.

Customers **do not** yet get a notification when their order status changes. Check the **Orders** page instead.

---

## 8. Profile & Account

- **Profile:** change your name (2–31 characters) and avatar. Your role is shown.
- **Change password:**
  - You must enter your current password.
  - The new one must be 8–15 characters.
  - Every other session is signed out.
- **Delete account:** this deactivates your account and signs you out everywhere.
  - Your past orders and reviews remain.
  - The email can't be used to register again.

  See [privacy-data.md](./privacy-data.md) for exactly what is kept.

---

## 9. Delivery Staff

Delivery accounts see orders that are **Shipped**, **Delivered** or **Returned**. For a shipped order they can set:

- **Delivered:** this also marks the order **paid** (cash collected).
- **Returned:** the package came back. Its stock is restored.

---

## 10. Admin & Store Operations

### Products (super admin)
- Create, edit and delete products:
  - name (2–255 characters), description (5–1023), price (at least 1.0) and categories;
  - up to **5 images**, at most 5 MB each (images are compressed in the browser before upload).
- Product status: Coming Soon, Available, Stock Out or Discontinued. Discontinued products are hidden from customers.
- Categories can be managed through the API (admin and super admin). There is no UI for them yet.

### Stock (view: admin and super admin; create/edit: super admin)
- A **stock batch** is a purchase with one line per product: quantity and purchase (cost) price.
- Units are consumed **first in, first out** when an order is accepted. Profit per sale = selling price − batch purchase price.
- **Stocks** lists the batches. **Stock items** lists every line, with its remaining quantity.

### Orders (admin; super admin)
- Filter by status, date range and product name.
- Change the status following the allowed flow:
  ```
  PENDING  → ACCEPTED | CANCELLED | REJECTED
  ACCEPTED → SHIPPED | CANCELLED | REJECTED | REFUNDED
  SHIPPED  → DELIVERED | LOST | RETURNED
  DELIVERED → COMPLETED
  LOST | RETURNED → REFUNDED
  ```
- Accepting an order consumes stock batches and records the sales.
- Cancelling or rejecting an order, or marking it returned, puts its units back on sale.
- A daily job (00:00) notifies admins about orders pending for more than 1 day and **rejects** orders pending for more than 2 days. **Known issue (F-06):** this job currently fails. Reject stale orders by hand.

### Users & roles
- **Users** (admin: view; super admin: edit): search and filter by name, role and status.
  - Statuses: Not Verified, Active, Suspended, Inactive, Deleted, Banned.
  - Changing a user's status or roles signs that user out.
  - In the user list, the **delete** action sets the user to *Suspended*.
- **Roles** (super admin): create, edit and delete roles. Each role is a set of permissions: `SUPER_ADMIN_ACCESS`, `ADMIN_ACCESS`, `DELIVERY_MAN_ACCESS`. Assign roles to users on the user edit page.

### Content
- **Banners** (super admin in the UI): title, subtitle, image, click-through link and display order. At most **20 banners**, of which **5 can be active**.
- **Blogs** (admin and super admin): drafts and published posts with an image. Published posts are public.
- **FAQs** (super admin in the UI): at most **10**, ordered by display order.

### Sales (super admin)
**Sales** lists one record per accepted order line per stock batch, with quantity and profit. Filter it by date range and product name.

Sales are recorded when an order is **accepted**, not when it is delivered.

### CSV reports (admin and super admin)
Use the **Report** button on the **Users**, **Orders** or **Sales** page. Choose an optional date range, and the CSV downloads directly.

| Report | Columns |
|---|---|
| Users | User ID, Name, Email, Status |
| Orders | Order ID, User ID, Total Price, Status, Name, Phone, Address, Payment Method, Created At |
| Sales | Sale ID, Product ID, Order ID, Quantity, Profit, Created At |

Order and user reports contain personal data, so handle the files accordingly.

### bKash payments (admin, API only)
Payments can be queried and refunded through `GET /api/payment/query/{paymentId}` and `POST /api/payment/refund`. See the [runbook](./runbook.md#3-payment-incidents-bkash) for the procedure.
