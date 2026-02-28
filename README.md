# Clearflow Website Backend

Backend API для управления пользователями, профилями, товарами, промокодами, статистикой и PDF-отчетами.

## Технологический стек

- Java 21
- Spring Boot 4 (Web MVC, Security, Data JPA)
- PostgreSQL 17
- Flyway
- JWT (access/refresh в HttpOnly cookie)
- OpenAPI/Swagger (`springdoc`)
- OpenPDF (генерация PDF)

## Быстрый старт

1. Поднять PostgreSQL (локально или в Docker) и создать БД/пользователя из `application.yaml`.
2. Проверить конфиг в `src/main/resources/application.yaml`.
3. Запустить backend:

```bash
mvn spring-boot:run
```

4. Swagger UI:

- `http://localhost:8080/swagger-ui/index.html`

## Полезные команды

```bash
mvn clean verify
mvn -DskipTests compile
```

## Основные модули API

- `POST /auth/register-seller`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

- `GET /users`
- `POST /users`
- `PATCH /users/{id}`
- `DELETE /users/{id}`

- `GET /profile/me`
- `PATCH /profile/me`
- `DELETE /profile/me`
- `GET /profile/me/photo`
- `PUT /profile/me/photo`
- `DELETE /profile/me/photo`
- `GET /profile/me/photo/file`

- `GET /verification/sellers`
- `GET /verification/sellers/{userId}`
- `POST /verification/sellers/{userId}/approve`
- `POST /verification/sellers/{userId}/reject`

- `GET /products`
- `GET /products/{id}`
- `POST /products`
- `PATCH /products/{id}`

- `GET /promo-codes`
- `GET /promo-codes/{id}`
- `POST /promo-codes`
- `PATCH /promo-codes/{id}`

- `PATCH /stats/promo/daily`
- `GET /stats/promo/daily`
- `GET /stats/promo/dashboard`
- `GET /stats/promo/product-dashboard`
- `GET /stats/promo/monthly-report.pdf`

## Ozon-интеграция по фото товара

Фото товаров не хранятся в БД backend.  
Для поля `photoUrl` в ответе товаров используется live-запрос в Ozon API (`/v2/product/pictures/info`).

### Важно

- Если Ozon API недоступен, backend не падает.
- В этом случае `photoUrl` возвращается как `null`.
- Для получения фото нужны Ozon-креды продавца:
  - `ozonClientId`
  - `ozonApiKey` (хранится зашифрованным)

## PDF-отчет за месяц

`GET /stats/promo/monthly-report.pdf` генерирует синхронный PDF-отчет по продавцу за месяц:

- реквизиты продавца;
- таблица по промокодам (кол-во продаж и доход);
- итоговые суммы.

Параметры:

- `sellerId` (optional для OWNER/ADMIN, SELLER берет свой id)
- `month` в формате `yyyy-MM` (optional)
- `inline` (`true` для открытия в браузере, иначе attachment)

## Доступ и роли

- `OWNER`: полный доступ
- `ADMIN`: доступ к операциям управления без ограничения по `parentId` в модулях `products/promo/stats`
- `SELLER`: работа со своими данными
- `MANAGER`: ограниченный доступ (без управления пользователями)

Актуальные детали авторизации и валидации смотри в Swagger и в сервисных слоях `logic/*`.
