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
Set-Location "C:\path\to\xian-The-Big-Fourt-Portfolio-Manager"
Set-Item Env:SPRING_DATASOURCE_URL "jdbc:mysql://localhost:3306/portfolio_db?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=UTC"
Set-Item Env:SPRING_DATASOURCE_USERNAME "root"
Set-Item Env:SPRING_DATASOURCE_PASSWORD "<your-password>"
Set-Item Env:ZHIPU_API_KEY "<your-api-key>"
.\mvnw.cmd spring-boot:run
```

Open in browser:

- `http://localhost:9001/`

## Run tests

```powershell
Set-Location "C:\path\to\xian-The-Big-Fourt-Portfolio-Manager"
.\mvnw.cmd test -DskipTests=false
```

## Front-end usage notes

- Portfolio tab supports create/find/update/delete + list all.
- Holding tab supports create/find/update/delete + list by portfolio id.
- Transaction tab supports create/find/update/delete + list by holding id.
- Price History tab supports create/find/delete + list by ticker and date range.

## Environment

The app reads sensitive runtime values from environment variables.
Set these before starting the app if you are not using the defaults:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ZHIPU_API_KEY`
