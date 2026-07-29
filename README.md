# Big Fourt Portfolio Manager

A Spring Boot portfolio management project with REST APIs and a complete static front-end dashboard.

## What is included

- Backend layers: `repository`, `service`, `controller`
- REST endpoints for:
  - Portfolio
  - Holding
  - Transaction
  - Price History
- Front-end UI (single-page dashboard):
  - `src/main/resources/static/index.html`
  - `src/main/resources/static/styles.css`
  - `src/main/resources/static/app.js`

## Run the project

```powershell
Set-Location "C:\Users\Administrator\Desktop\final\xian-The-Big-Fourt-Portfolio-Manager"
.\mvnw.cmd spring-boot:run
```

Open in browser:

- `http://localhost:9009/`

## First run behavior (for new clones)

Default startup now auto-bootstraps the database when it is new/empty:

- Creates tables from `schema.sql` if missing.
- Inserts the same seed dataset from `data.sql` only when `portfolio` has no rows.
- Does not wipe existing data on subsequent runs.

So new contributors can run the normal command directly and still get data:

```powershell
Set-Location "C:\Users\Administrator\Desktop\final\xian-The-Big-Fourt-Portfolio-Manager"
.\mvnw.cmd spring-boot:run
```

## Run tests

```powershell
Set-Location "C:\Users\Administrator\Desktop\final\xian-The-Big-Fourt-Portfolio-Manager"
.\mvnw.cmd test -DskipTests=false
```

## Front-end usage notes

- Portfolio tab supports create/find/update/delete + list all.
- Holding tab supports create/find/update/delete + list by portfolio id.
- Transaction tab supports create/find/update/delete + list by holding id.
- Price History tab supports create/find/delete + list by ticker and date range.

## Environment

The app currently uses MySQL from `src/main/resources/application.properties`.
Update connection values if needed:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

