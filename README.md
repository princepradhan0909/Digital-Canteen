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
storage.

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
