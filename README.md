# PulsePass API

API REST académica (Programación para Web, Universidad del Magdalena) para gestionar eventos,
venues, artistas, usuarios y entradas (tickets).

## Stack

- Java 21, Spring Boot 4.1, Maven (wrapper incluido: `mvnw`)
- Spring MVC, Bean Validation, Spring Data JPA / Hibernate
- PostgreSQL 18 (Docker), Flyway
- Spring Security 7 + JJWT 0.13 (JWT HS256, API stateless con `Authorization: Bearer`)
- JUnit 5, Mockito, MockMvc, Testcontainers

## Arquitectura

```
HTTP Request → Controller → Service → Repository → PostgreSQL
                    ↓            ↓
              Request DTO   Excepción de negocio
                                 ↓
                     GlobalExceptionHandler → ErrorResponse
```

- **Controller** (`controller/`): solo traduce HTTP ↔ Service. Sin reglas de negocio ni acceso a repositories.
- **Service** (`service/`): reglas de negocio y transacciones. Devuelve DTOs, nunca entidades.
- **Repository** (`repository/`): acceso a datos con Spring Data JPA.
- **DTOs** (`dto/request`, `dto/response`): `record` de Java; son el único contrato público de la API.
- **GlobalExceptionHandler** (`exception/`): único punto donde las excepciones se convierten en respuestas HTTP.

## Requisitos

- JDK 21
- Docker Desktop (para la base de datos y para las pruebas de persistencia)

## Ejecutar la aplicación

La aplicación **no arranca sin la clave JWT** (PRD Seguridad §12.2). Una sola vez:

```powershell
Copy-Item .env.example .env
# Generar una clave aleatoria de 32 bytes y pegarla en .env como PULSEPASS_JWT_SECRET_BASE64
$b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

`.env` está en `.gitignore` y Spring Boot lo lee al arrancar (`spring.config.import`). Nunca lo subas al repo.

| Variable | Obligatoria | Valor por defecto |
|---|---|---|
| `PULSEPASS_JWT_SECRET_BASE64` | Sí (Base64 de 32+ bytes) | — |
| `PULSEPASS_JWT_ISSUER` | No | `pulsepass-api` |
| `PULSEPASS_DEMO_STAFF_PASSWORD` | Solo perfil `dev` | — (vacía: no crea `laura`) |
| `PULSEPASS_DEMO_ADMIN_PASSWORD` | Solo perfil `dev` | — (vacía: no crea `admin`) |

```bash
docker compose up -d        # levanta PostgreSQL en localhost:5432 (db/usuario/clave: pulsepass)
./mvnw spring-boot:run      # Flyway crea el esquema al arrancar
```

La API queda en `http://localhost:8080`.

### Cuentas de prueba (perfil `dev`)

El registro público solo crea usuarios con rol `USER`. Para la demo, el perfil `dev` crea dos cuentas
con las contraseñas definidas en `.env` (si ya existen, no las toca):

```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

| Usuario | Correo | Roles |
|---|---|---|
| `laura` | `laura@pulsepass.com` | USER, STAFF |
| `admin` | `admin@pulsepass.com` | USER, ADMIN |

## Endpoints

### Reglas de acceso (PRD Seguridad §11)

| Acceso | Endpoints |
|---|---|
| Público | `POST /api/users`, `POST /api/auth/login`, GET de venues y artistas, `GET /api/events/published` |
| Público, solo eventos `PUBLISHED` (STAFF/ADMIN ven todos) | `GET /api/events/{eventCode}` (borrador: 404), `GET /api/events/by-artist` |
| Dueño o ADMIN | `GET /api/users/by-email`, `GET /api/users/by-username`, `GET /api/tickets/by-user`, `PATCH /api/tickets/{ticketCode}/cancel` |
| Dueño, STAFF o ADMIN | `GET /api/tickets/{ticketCode}` |
| USER, con su propio `userEmail` | `POST /api/tickets` |
| STAFF o ADMIN | `GET /api/events/{eventCode}/tickets/paid`, `PATCH /api/tickets/{ticketCode}/use` |
| ADMIN | `POST /api/events`, `PATCH /api/events/{eventCode}/publish`, `POST /api/events/{eventCode}/artists/{artistId}` |
| Autenticado | `GET /api/auth/me` y cualquier ruta no listada |

La propiedad se comprueba con el id del usuario del token contra la base, nunca con el correo
que envía el cliente. Recurso ajeno: 403. Limitación del MVP: STAFF opera sobre todos los eventos,
porque el dominio no modela qué personal trabaja en qué evento (§11.1).

### Autenticación

| Método | Endpoint | Acceso | Respuesta |
|---|---|---|---|
| POST | `/api/auth/login` | Público | 200 · 400 · 401 |
| GET | `/api/auth/me` | Bearer | 200 · 401 |

```json
POST /api/auth/login
{ "identifier": "andrea", "password": "UnaFraseSeguraLarga2026!" }

200
{ "accessToken": "<JWT>", "tokenType": "Bearer", "expiresIn": 900 }
```

`identifier` acepta username o correo. Las rutas protegidas se llaman con el header
`Authorization: Bearer <JWT>`. El token dura 15 minutos y no se acepta en la URL.

### Venues

| Método | Endpoint | Respuesta |
|---|---|---|
| GET | `/api/venues/{code}` | 200 · 404 |
| GET | `/api/venues/active` | 200 |

### Eventos

| Método | Endpoint | Respuesta |
|---|---|---|
| POST | `/api/events` | 201 · 400 · 404 · 409 |
| GET | `/api/events/{eventCode}` | 200 · 404 |
| GET | `/api/events/published` | 200 |
| GET | `/api/events/by-artist?stageName=...` | 200 · 400 |
| PATCH | `/api/events/{eventCode}/publish` | 200 · 404 · 409 |
| POST | `/api/events/{eventCode}/artists/{artistId}` | 200 · 400 · 404 · 409 |

### Artistas

| Método | Endpoint | Respuesta |
|---|---|---|
| GET | `/api/artists/{id}` | 200 · 400 · 404 |
| GET | `/api/artists/by-stage-name?stageName=...` | 200 · 400 · 404 |
| GET | `/api/artists/active` | 200 |

### Usuarios

| Método | Endpoint | Respuesta |
|---|---|---|
| POST | `/api/users` | 201 · 400 · 409 |
| GET | `/api/users/by-email?email=...` | 200 · 400 · 404 |
| GET | `/api/users/by-username?username=...` | 200 · 400 · 404 |

### Tickets

| Método | Endpoint | Respuesta |
|---|---|---|
| POST | `/api/tickets` | 201 · 400 · 404 · 409 |
| GET | `/api/tickets/{ticketCode}` | 200 · 404 |
| GET | `/api/tickets/by-user?email=...` | 200 · 400 |
| GET | `/api/events/{eventCode}/tickets/paid` | 200 · 404 |
| PATCH | `/api/tickets/{ticketCode}/cancel` | 200 · 404 · 409 |
| PATCH | `/api/tickets/{ticketCode}/use` | 200 · 404 · 409 |

### Ejemplos de request

Crear evento (`POST /api/events`):

```json
{
  "eventCode": "CMF-2026",
  "name": "Caribbean Music Fest 2026",
  "description": "Festival de música caribe en Santa Marta",
  "category": "MUSIC",
  "eventDate": "2026-12-20T20:00:00",
  "minimumAge": 18,
  "venueCode": "VEN-SMR-01"
}
```

Registrar usuario (`POST /api/users`):

```json
{
  "username": "andrea",
  "email": "andrea@pulsepass.com",
  "firstName": "Andrea",
  "lastName": "Gomez",
  "phone": "3001234567",
  "city": "Santa Marta",
  "birthDate": "2000-05-10",
  "password": "UnaFraseSeguraLarga2026!"
}
```

`password`: mínimo 12 caracteres y máximo 72 bytes (límite de BCrypt). Se guarda solo su hash
y nunca aparece en la respuesta. El registro público siempre asigna el rol `USER`.

Comprar ticket (`POST /api/tickets`). El precio no se envía: lo calcula el sistema.

```json
{
  "userEmail": "andrea@pulsepass.com",
  "eventCode": "CMF-2026",
  "type": "GENERAL"
}
```

## Códigos HTTP y errores

| Código | Cuándo |
|---|---|
| 200 OK | Consulta o actualización correcta |
| 201 Created | Creación de evento, usuario o ticket |
| 400 Bad Request | Bean Validation falla, JSON mal formado, parámetro faltante o con tipo inválido |
| 401 Unauthorized | Sin token, token inválido o vencido, login fallido |
| 403 Forbidden | Token válido pero sin el rol necesario, o recurso de otro usuario |
| 404 Not Found | Recurso inexistente o ruta que no existe |
| 409 Conflict | Duplicado o regla de negocio incumplida |
| 500 Internal Server Error | Error inesperado (sin exponer detalles internos) |

Todos los errores usan el mismo formato (`ErrorResponse`):

```json
{
  "timestamp": "2026-10-04T20:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "eventCode": "Event code is required",
    "venueCode": "Venue code is required"
  }
}
```

`details` solo trae contenido en errores de validación; en el resto llega vacío (`{}`).

## Pruebas

| Tipo | Clases | Herramientas | ¿Necesita Docker? |
|---|---|---|---|
| Controladores | `controller/*ControllerTest` | `@WebMvcTest`, MockMvc, `@MockitoBean` | No |
| Persistencia | `repository/*Test` | `@DataJpaTest`, Testcontainers + PostgreSQL | Sí |
| Seguridad web | `security/SeguridadRutasTest`, `security/AutenticacionJwtTest` | `@WebMvcTest` con la `SecurityConfig` real | No |
| Seguridad unitaria | `security/JwtServiceTest`, `security/CustomUserDetailsServiceTest`, `security/AuthorizationServiceTest`, `security/DemoUsersInitializerTest`, `service/AuthServiceTest` | JUnit 5, Mockito | No |
| Integración | `integration/*IntegracionTest` | `@SpringBootTest` + MockMvc + Testcontainers | Sí |
| Contexto completo | `PulsepassApiApplicationTests` | `@SpringBootTest` | Sí |

```bash
./mvnw clean test                              # toda la suite (Docker Desktop encendido)
./mvnw clean test -Dtest="*ControllerTest"     # solo controladores, sin Docker
./mvnw clean test -Dtest=VenueControllerTest   # una clase
```

En las pruebas de controlador los Services son mocks: se valida status HTTP, Content-Type,
campos del JSON con `jsonPath`, la llamada al Service con `verify(...)` y que un request
inválido nunca llegue al Service con `verify(..., never())`.

## Flujo de trabajo

Git flow: ramas `feature/*` desde `develop`, PR a `develop`, `release/*` a `main` con tag.
