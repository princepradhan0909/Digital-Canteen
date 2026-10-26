# CampusBite

CampusBite is a Spring Boot campus canteen ordering application with a responsive,
installable web app. The web app is also the supported Android experience; this
repository does not contain a separate native Android project.

## Configure and run

Use Java 21 and PostgreSQL. Configure the following environment variables before
starting the application:

| Variable | Required | Description |
| --- | --- | --- |
| `DATABASE_URL` | Yes | JDBC URL for the PostgreSQL database |
| `DB_USERNAME` / `DB_PASSWORD` | Yes | Database credentials |
| `JWT_SECRET` | Yes | Random secret of at least 32 characters for HS256 access tokens |
| `ADMIN_EMAIL` | For staff tools | Dedicated canteen administrator account |
| `ADMIN_PASSWORD` | For staff tools | Initial admin password; at least 12 characters |
| `ADMIN_PHONE` | For staff tools | Administrator contact number |
| `ADMIN_NAME` | No | Display name for the initial admin account |
| `SMTP_HOST` / `SMTP_PORT` | For student registration | Authenticated SMTP server used to send verification codes |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | For student registration | SMTP credentials |
| `CAMPUSBITE_MAIL_FROM` | For student registration | Verified sender address |
| `RAZORPAY_KEY_ID` | For online payments | Razorpay merchant key ID |
| `RAZORPAY_KEY_SECRET` | For online payments | Razorpay API key secret |
| `RAZORPAY_WEBHOOK_SECRET` | For webhook updates | Secret configured for the payment webhook |

Generate a strong JWT secret, for example with `openssl rand -base64 48`. Do not
commit production credentials or send them in chat. Configure the Razorpay
webhook endpoint as `/api/payments/razorpay/webhook` and subscribe to
`payment.captured`. Use Razorpay test credentials and test mode before enabling
live payments. Payment card data is collected only by Razorpay Checkout.

The initial staff account is created from the admin environment variables if it
does not exist. The application refuses to promote an already-registered
student account to staff. Register student accounts through the storefront.
Student accounts require an email verification code before login. Passwords
are BCrypt hashed; JWTs expire after one hour and are held in tab-scoped browser
storage. The storefront displays the sign-in/create-account entry screen before
loading the menu; food and order APIs require an authenticated token. Logout
revokes issued tokens for that account. Google sign-in and password recovery
are not configured or presented by the application.

Run the project with `./mvnw spring-boot:run`. The app uses Hibernate
`ddl-auto=update`; back up the database before deploying schema changes.

## Features

- Student registration, sign-in, profile updates, and JWT-protected APIs.
- Canteen staff menu management, order status updates, and daily/weekly/monthly
  sales summaries.
- Cart and checkout with counter payment or Razorpay UPI/card checkout.
- Razorpay signature verification and signed `payment.captured` webhooks.
- Private order tracking and account-scoped in-app notifications.
- Popular-food suggestions ranked from non-cancelled order quantities.
- Responsive 3D-animated UI and installable PWA shell caching.

In-app notifications refresh while the app is open; browser push notifications
are not included. Popular items use order history and do not send student
preferences to an external AI service.

## Catalog API

The authenticated catalog endpoints are:

- `GET /api/catalog/categories`
- `GET /api/catalog/products?categoryId=<id>&search=<text>`
- `GET /api/catalog/products/{id}`

The signed-in storefront uses the catalog endpoints for category filtering and
product cards. Products with variants or add-ons open a customization dialog;
the cart preserves each selection, refreshes prices from the catalog, and
sends the selected option IDs with checkout.

Staff with the `ADMIN` role can manage categories and products through
`/api/admin/catalog/categories` and `/api/admin/catalog/products` using `POST`,
`PUT`, and `GET`. Product requests include a category ID, a decimal base price,
and product-specific variant and add-on lists. A variant's price is its full
unit price; an add-on's price is added to the selected base/variant price.
Inactive products, categories, variants, and add-ons are omitted from student
catalog responses and remain available to staff for management.

Order checkout accepts `variantId` and `addonIds` on each item in
`POST /api/orders`. Prices are recalculated from the current catalog on the
server; a product with an available variant requires one to be selected, and
every add-on must belong to that product and be available. New order records
store decimal totals plus snapshots of the selected variant and add-on names
and prices. The legacy `totalAmount` and `itemPrice` fields remain available,
and the added snapshot columns are nullable so existing order history remains
compatible.

Catalog persistence is additive: existing food rows and order history are not
deleted. The legacy category text and price columns are retained; missing
category relations are linked during startup, while exact catalog prices use a
new nullable decimal column so existing database rows remain compatible. Back
up the database before deployment and verify the generated schema update in a
staging database first.
