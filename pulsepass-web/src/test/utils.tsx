import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, type InitialEntry } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { onTestFinished } from 'vitest'
import { AvisosProvider } from '../componentes/Avisos'
import { AuthProvider } from '../funcionalidades/auth/AuthContext'
import { guardarSesion } from '../lib/sesionAlmacen'
import { rutas } from '../router'
import { server } from './mocks/servidor'
import type { NombreUsuario } from './mocks/datos'

// Cliente de pruebas: sin reintentos para que los errores aparezcan de inmediato
export function crearClientePruebas(): QueryClient {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
}

// Simula una sesión ya iniciada (como si la persona hubiera recargado la página con token vigente)
export function sesionComo(usuario: NombreUsuario): void {
  guardarSesion({ token: `token-${usuario}`, expiraEn: Date.now() + 15 * 60 * 1000 })
}

interface OpcionesRender {
  usuario?: NombreUsuario
  queryClient?: QueryClient
}

// PRD Frontend §17 (test/utils.tsx): la app completa (Router + Query + Auth + Avisos) en una ruta dada.
// Usa las MISMAS rutas de producción, con createMemoryRouter en vez del navegador
// `ruta` también acepta { pathname, state } para simular location.state (por ejemplo, { registrado: true })
export function renderApp(ruta: InitialEntry = '/', { usuario, queryClient = crearClientePruebas() }: OpcionesRender = {}) {
  if (usuario) sesionComo(usuario)
  const router = createMemoryRouter(rutas, { initialEntries: [ruta] })
  const user = userEvent.setup()
  const resultado = render(
    <QueryClientProvider client={queryClient}>
      <AvisosProvider>
        <AuthProvider>
          <RouterProvider router={router} />
        </AuthProvider>
      </AvisosProvider>
    </QueryClientProvider>,
  )
  return { ...resultado, router, queryClient, user }
}

// Cuenta las peticiones cuyo método y ruta coinciden (por ejemplo, 'GET /api/artists/'). Sirve para
// comprobar que una página NO llamó a la API (TEST-FE-ART-003). Se desconecta al terminar la prueba.
export function contarPeticiones(metodo: string, prefijoRuta: string): () => number {
  let total = 0
  const escuchar = ({ request }: { request: Request }) => {
    if (request.method === metodo && new URL(request.url).pathname.startsWith(prefijoRuta)) total += 1
  }
  server.events.on('request:start', escuchar)
  onTestFinished(() => server.events.removeListener('request:start', escuchar))
  return () => total
}
