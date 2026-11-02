# Shopora API

Spring Boot REST API for Shopora, a full-stack ecommerce application. Serves the product catalog,
checkout, and order history for the Angular storefront in
[shopora-web](https://github.com/ManojGaikwadLearner/shopora-web).

Built as a hands-on exercise in Java backend development: JPA entity modelling, Spring Data REST,
OAuth2 resource-server security, and a real third-party payment integration.

## Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 |
| Persistence | Spring Data JPA, Hibernate 7 |
| API | Spring Data REST + hand-written controllers |
| Database | MySQL 8 |
| Auth | Spring Security OAuth2 Resource Server (JWT), Auth0 as the issuer |
| Payments | Stripe (Payment Intents API) |
| Build | Maven |

## Features

- **Product catalog** — paginated and searchable products grouped by category, exposed through
  Spring Data REST with write methods disabled so the catalog is strictly read-only over HTTP.
- **Checkout** — a transactional `POST /api/checkout/purchase` that persists a customer, their
  order, line items, and billing/shipping addresses as one aggregate, and returns a generated
  order tracking number.
- **Stripe payments** — `POST /api/checkout/payment-intent` creates a Payment Intent server-side
  and returns the client secret; card details never touch this API.
- **Order history** — `GET /api/orders/search/findByCustomerEmail` requires a valid JWT, so a
  signed-in user can only reach their own orders.
- **HTTPS** — runs on 8443 with a local PKCS#12 keystore (not committed; see Setup).

## Running locally

### Prerequisites

- JDK 21
- Maven 3.9+
- MySQL 8
- An [Auth0](https://auth0.com) tenant (free) and a [Stripe](https://stripe.com) account in test mode

### 1. Database

Create the schema and a dedicated application user — the app never connects as `root`:

```sql
CREATE DATABASE `full-stack-ecommerce`;
CREATE USER 'ecommerceapp'@'localhost' IDENTIFIED BY 'choose-a-password';
GRANT ALL PRIVILEGES ON `full-stack-ecommerce`.* TO 'ecommerceapp'@'localhost';
```

Then load the schema and seed data from `sql-scripts/`.

### 2. Auth0

Create an **API** in the Auth0 dashboard with identifier `https://shopora-api` and RS256 signing.
That same value must appear in this API's `auth0.audience` property and in the Angular client's
`audience` parameter, or Auth0 issues an opaque token this API cannot validate.

### 3. TLS keystore

Generate your own; the repository deliberately contains no key material.

```bash
keytool -genkeypair -alias shopora -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore src/main/resources/keystore.p12 -validity 365
```

### 4. Configuration

Every secret is read from an environment variable — see
[`application-example.properties`](src/main/resources/application-example.properties) for the full
list. Nothing sensitive is committed.

```bash
export DB_USER=ecommerceapp
export DB_PASSWORD='choose-a-password'
export AUTH0_ISSUER_URI=https://your-tenant.us.auth0.com/
export STRIPE_SECRET_KEY=sk_test_...
export SSL_KEYSTORE_PASSWORD='your-keystore-password'
```

### 5. Run

```bash
mvn spring-boot:run
```

The API starts on `https://localhost:8443`.

## API

Spring Data REST exposes repositories under `/api`; checkout is a hand-written controller.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/products` | — | Paginated products |
| `GET` | `/api/products/{id}` | — | A single product |
| `GET` | `/api/products/search/findByCategoryId?id={id}` | — | Products in a category |
| `GET` | `/api/products/search/findByNameContaining?name={q}` | — | Keyword search |
| `GET` | `/api/product-category` | — | All categories |
| `GET` | `/api/countries`, `/api/states/search/findByCountryCode` | — | Address reference data |
| `POST` | `/api/checkout/purchase` | — | Place an order; returns a tracking number |
| `POST` | `/api/checkout/payment-intent` | — | Create a Stripe Payment Intent |
| `GET` | `/api/orders/search/findByCustomerEmail?email={e}` | JWT | A customer's order history |

Paginate and sort with the usual Spring Data parameters:
`?page=0&size=10&sort=unitPrice,desc`.

## Design notes

Decisions worth explaining, since this project exists to be understood rather than just to run.

**Spring Data REST for the catalog, a controller for checkout.** The catalog is plain CRUD over
entities, so Spring Data REST removes boilerplate that would add nothing. Checkout has real
business logic — building the order aggregate, generating a tracking number, calling Stripe — so it
gets an explicit controller and service. Mixing the two deliberately, rather than picking one for
the whole API, keeps each endpoint as simple as its job allows.

**Write methods disabled on the catalog.** `MyDataRestConfig` turns off POST, PUT, PATCH, and
DELETE for `Product`, `ProductCategory`, `Country`, and `State`. Spring Data REST would otherwise
expose a fully writable catalog to anyone who can reach the API.

**Entity IDs exposed on purpose.** REST's default is to hide them behind HAL links, but the Angular
client routes by product id, so `exposeIdsFor` is applied to every entity type from the JPA
metamodel.

**`open-in-view` disabled.** With it on, lazy collections load silently during JSON serialization,
hiding N+1 queries behind what looks like a single call. Off, a missed fetch fails loudly instead
of quietly costing a query per row.

**Hand-written getters and setters instead of Lombok.** `@Data` generates `equals`, `hashCode`, and
`toString` across all fields, which on a bidirectional relationship such as
`Product` ↔ `ProductCategory` means infinite recursion in `toString()` and a forced load of the
lazy collection whenever two entities are compared. Explicit accessors avoid the whole category of
problem.

**JWT validation, not session cookies.** The API is a stateless resource server: Angular performs
the OIDC login against Auth0 and sends the access token as a bearer header. The API validates the
signature against Auth0's JWKS and checks the `aud` claim, so a token minted for a different API is
rejected. CSRF protection is off because there are no cookies to protect.

**CORS is not access control.** It is configured for the Angular origin, but CORS only stops other
sites' JavaScript from reading responses in a browser. Anyone with curl can call these endpoints
regardless, which is why authorization is enforced in the filter chain and not by the origin list.

## Known limitations

A learning project, and these are deliberate stopping points rather than oversights.

- `ddl-auto` generates the schema; a production system would use Flyway or Liquibase migrations.
- No integration test suite yet. A `@SpringBootTest` over the checkout flow is the obvious next step.
- The Stripe call happens inside the checkout transaction, holding a database connection across a
  network call. The correct pattern is to commit the order as `PENDING` first and reconcile via a
  webhook.
- Order status is not modelled as a state machine, and there is no admin surface for fulfilment.

## Related

- [shopora-web](https://github.com/ManojGaikwadLearner/shopora-web) — Angular storefront
