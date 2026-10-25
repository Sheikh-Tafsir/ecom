# Personal Data & Privacy

This page lists what personal data the platform stores, where it goes, how long it is kept, and what happens when a user deletes their account. It describes the code as of 2026-10-25. It is an engineering reference, not a legal privacy policy. Check the obligations that apply to you (for example Bangladesh data-protection rules, or GDPR if you serve EU customers) before publishing a policy.

## 1. What is stored, and where

### Main database (`postgres`)

| Data | Table.column | Collected when | Visible to |
|---|---|---|---|
| Name, email | `users.name`, `users.email` | Signup or Google login | The user; admins (user list); super admin (edit); CSV user report (ID, name, email, status) |
| Password | `users.password` (BCrypt hash) | Signup or password change | Nobody (only the hash is stored) |
| Gender, avatar URL | `users.gender`, `users.image` | Profile update | The user; admins |
| Delivery name, phone, address | `orders.name`, `orders.phone`, `orders.address` (copied per order) | Checkout | The user; admins; **all** delivery staff for SHIPPED/DELIVERED/RETURNED orders; CSV order report |
| Purchase history | `orders`, `order_items`, `payments`, `sales` | Checkout and fulfilment | The user (own orders); admins |
| Reviews | `reviews.comment`, `reviews.rating` (linked to `user_id`) | Review submitted | Public |
| Order audit trail | `order_history.changed_by_user_id` | Every status change | Admins |
| Sessions | `user_refresh_tokens` (JTI, status) | Login or refresh | Nobody (internal) |

### Chat database (`postgres-chat`)

| Data | Table | Notes |
|---|---|---|
| Message content and images | `messages.content` (text, or a Cloudinary URL for images) | Visible to the chat's participants |
| Who talks to whom | `chat_participants`, `chats.last_message`, `last_sender_id` | — |
| Read and delivery receipts | `message_receipts` | — |
| Block list | `blocked_users` | — |
| Last-seen time | `chat_participants.last_seen`; Redis `presence:<userId>` | The presence query returns any user's online status to any logged-in user (audit F-46) |

### Redis (short-lived)

| Data | TTL |
|---|---|
| OTP codes and attempt counters (keyed by email) | 5 min |
| User and profile cache (name, email, permissions) | 10 min |
| Chat user-profile cache (`cache:user:<id>`) | 10 min |
| Idempotency responses (can contain order IDs and payment URLs) | 60 min |
| Revoked token markers | Access-token lifetime |

### Browser

| Store | Contents |
|---|---|
| Memory | Access token |
| `HttpOnly` cookie | Refresh token (7 days) |
| `localStorage` | Cart items, a boolean "has session" hint |
| `sessionStorage` | Current checkout idempotency key |

## 2. Third parties that receive data

| Provider | What it receives | Why |
|---|---|---|
| **bKash** | Order amount, invoice number, `payerReference = "Order_<orderId>"`. The customer enters their wallet details on bKash's own page. | Payment |
| **Google** | ID token verification (`tokeninfo`) | Google login |
| **SMTP provider** (`MAIL_SMTP_HOST`, Gmail by default) | Recipient email; OTP and notification text | Email |
| **Cloudinary / Supabase** | Product images, user avatars, chat images (public URLs) | Image storage and CDN |
| **Google Drive** (optional, `GDRIVE_SYNC_ENABLED=true`) | Encrypted database backups | Off-site backup |

## 3. Retention

| Data | Retention | Mechanism |
|---|---|---|
| Unverified accounts | Hard-deleted after **24 h** | `DeleteNotVerifiedUserScheduler` (daily at midnight) |
| Revoked refresh tokens | Deleted **1 day** after revocation | `DeleteUserRefreshTokenScheduler` (daily) |
| OTPs | 5 min | Redis TTL |
| User accounts (deleted, banned) | **Indefinitely** (soft delete) | `users.deleted = true`, `status = DELETED/BANNED` |
| Orders, payments, sales, order history | **Indefinitely** | — |
| Reviews | Indefinitely; they stay public after the author deletes their account | — |
| Chat messages and images | **Indefinitely**; there is no message deletion feature | — |
| Server and gateway log files | 7 days / 100 MB cap (`logback-spring.xml`); startup logs 3 days | Rolling file appender |
| Docker container logs | 3 × 10 MB per container | `json-file` driver |
| Main-DB backups | 7 days (`BACKUP_RETENTION_DAYS`), AES-256 encrypted | `db-backup` |

## 4. Account deletion: what actually happens

A user deletes their own account through `ProfileService.delete`, and a super admin does it through `UserService.delete` or `banned`. Either way:

1. `users.status` is set to `DELETED` (or `BANNED`), and `users.deleted` is set to `true`.
2. All of the user's tokens are revoked.
3. A user-changed event is published to chat on `ecom:user-updates`, and the user and profile caches are evicted.

These steps do **not** happen:

- Name, email, password hash, gender and avatar are **not** erased or anonymised.
- Orders keep the delivery name, phone and address.
- Reviews stay public.
- Chat messages, images and participation remain.
- The avatar file in Cloudinary/Supabase is not deleted. It is only deleted when the user *replaces* it (`ProfileService` line 58).
- The email can't be registered again; signup responds "Email already exists! Account is Deleted".
- Backups keep everything for 7 more days.

**No data export feature exists** for users.

## 5. Personal data in logs

| Location | Data | Status |
|---|---|---|
| `OrderController.create` (`log.info("request {}", request)`) | Customer name, phone, address | ⚠️ Remove (audit F-36) |
| Mail services (INFO) | Recipient email | ⚠️ Mask or drop |
| `BkashPaymentService` | Request body at INFO, bKash responses at DEBUG | ⚠️ Log IDs only |
| MDC on every server/gateway line | `requestId`, client IP, user ID | Acceptable (pseudonymous) |
| Nginx access logs | Client IP, URL, user agent | Acceptable; rotate |
| Chat group handlers | Full group request bodies at INFO | ⚠️ Log IDs only |

`LoggingFilter` masks only **query-string** parameters (`token`, `password`, `secret`, `phone`, …). It does not mask request bodies, or anything the application logs explicitly.

## 6. Gaps and recommended changes

| # | Gap | Recommendation |
|---|---|---|
| 1 | Deletion keeps all personal data | On deletion, anonymise `users` (name → "Deleted user", email → `deleted+<id>@invalid`, clear the image, gender and password). Delete the avatar file. Decide whether reviews are anonymised or removed. |
| 2 | Orders keep addresses forever | Keep orders for accounting, and set a retention period (e.g. N years per local tax rules). After that, blank `name`, `phone` and `address`. |
| 3 | Chat history kept forever | Decide on a retention period, or let users delete their messages and conversations. |
| 4 | Every delivery account sees every shipped customer's phone and address | Assign orders to couriers (audit F-31). |
| 5 | No user data export | Add `GET /profile/export` that returns the user's profile, orders, reviews and chat messages as JSON. |
| 6 | Personal data in logs | Remove or mask it (section 5). |
| 7 | A real person's name and email are seeded by `V3__initial_data.sql` into every deployment | Seed the super admin from env vars at first start instead (audit F-54). |
| 8 | The client has a `/privacy` page, but it isn't derived from this inventory | Check its text against sections 1–4, and link it from signup and checkout. |
| 9 | Presence leaks online status to any user | Restrict the presence query to the user's contacts or chat participants. |
