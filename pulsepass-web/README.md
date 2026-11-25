# PulsePass Web (Fase 5 — Frontend)

SPA de PulsePass con React 19, TypeScript estricto, Tailwind CSS v4, React Router v8 (modo datos),
TanStack Query, React Hook Form + zod y pruebas con Vitest + Testing Library + MSW.
Requisitos: PRD Frontend (`docs/`, fuera del repo).

> Estado: **bloque A** (base, componentes, router, sesión, cliente HTTP y pruebas de infraestructura).
> Las páginas de los bloques B, C y D aparecen como "Esta página se implementa en el bloque …".

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
