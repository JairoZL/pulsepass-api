# PulsePass Web (Fase 5 — Frontend)

SPA de PulsePass con React 19, TypeScript estricto, Tailwind CSS v4, React Router v8 (modo datos),
TanStack Query, React Hook Form + zod y pruebas con Vitest + Testing Library + MSW.
Requisitos: PRD Frontend (`docs/`, fuera del repo).

> Estado: **completo** — las 18 páginas del PRD (§9.3–§9.19) con sus pruebas (`npm test`).

## Páginas por rol

| Rol | Páginas |
|---|---|
| Público | Inicio, Cartelera (filtros en la URL), Detalle de evento, Artistas, Detalle de artista, Sedes, Detalle de sede, Login, Registro |
| Con sesión | Mi cuenta |
| USER | Compra (modal de confirmación), Confirmación, Mis tickets (filtros y cancelación), Detalle de ticket |
| STAFF / ADMIN | Validación de ingreso, Tickets pagados por evento |
| ADMIN | Eventos (búsqueda por código y creados en la sesión), Nuevo evento, Gestionar evento (publicar y asociar artistas) |

## Usuarios de prueba (backend con `DemoUsersInitializer`)

Con el perfil `dev` el backend crea `laura` (USER, STAFF) y `admin` (USER, ADMIN); ver el README de la raíz. Las cuentas USER se crean desde Registro.

## Requisitos

- Node **22.22+** (lo exige React Router v8): `node -v`
- Backend PulsePass corriendo en `http://localhost:8080` (ver README de la raíz)

## Comandos

```bash
cd pulsepass-web
npm install
npm run dev      # http://localhost:5173  (Vite reenvía /api al backend)
npm test         # Vitest + MSW, sin backend real
npm run build    # tsc estricto + build de producción
npm run lint
```

## Variables

| Variable | Valor | Nota |
|---|---|---|
| `VITE_API_BASE_URL` | `/api` | Pública por definición; sin secretos (ver `.env.example`) |

## Estructura

```
src/
  lib/              http.ts (fetch + Bearer + ApiError), rutas.ts, formato.ts, sesionAlmacen.ts
  componentes/      UI genérica (Boton, Campo, Modal, Tabla<T>, Badge, Aviso, estados…)
  layouts/          Layout, Navbar, guardas (RutaProtegida, RutaPorRol, SoloAnonimo)
  funcionalidades/  por dominio: tipos.ts (zod), api.ts, hooks.ts, AuthContext
  pages/            una página por ruta (lazy)
  test/             setup, MSW (23 endpoints), renderApp
```

## Contrato con el backend real (PRD §29 etapa 1)

| El PRD supone | El backend expone | Decisión |
|---|---|---|
| `/api/auth/me` trae email | `id`, `username`, `roles` | El email se obtiene con `GET /api/users/by-username` del propio usuario |
| `UserResponse` con nombres y nacimiento | `id`, `username`, `email`, `active` | "Mi cuenta" muestra lo disponible |
| Ticket con nombre del titular | `userEmail` | Se muestra el correo como titular |
| Resumen de evento con ciudad | `venueName` | Se muestra el nombre de la sede |
| Listado completo de eventos para ADMIN | Solo búsqueda por código | "Creados en esta sesión" en `sessionStorage` (se borra al cerrar la pestaña o la sesión) |
| Entradas disponibles por evento | Capacidad de la sede | El detalle muestra la capacidad de la sede; el cupo real lo valida el backend (409) |

## Decisiones de seguridad (PRD §24)

- El token vive en memoria y en `sessionStorage`, nunca en `localStorage`, cookies ni la URL.
- Un 401 cierra la sesión, limpia la caché y lleva al login con "Tu sesión expiró".
- El correo de la compra y de "Mis tickets" sale siempre de la sesión, nunca de un campo editable.
- El precio nunca se envía: lo fija el backend.
- Un borrador, un ticket ajeno o uno inexistente se muestran como "no disponible", sin revelar si existen.
- Toda respuesta de la API, parámetro de URL, `location.state` y `sessionStorage` se valida con zod.
- Los roles en la UI solo ocultan opciones; la autorización real es del backend (`@PreAuthorize`).
