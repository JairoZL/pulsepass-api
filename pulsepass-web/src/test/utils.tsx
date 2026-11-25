import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { AvisosProvider } from '../componentes/Avisos'
import { AuthProvider } from '../funcionalidades/auth/AuthContext'
import { guardarSesion } from '../lib/sesionAlmacen'
import { rutas } from '../router'
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
export function renderApp(ruta = '/', { usuario, queryClient = crearClientePruebas() }: OpcionesRender = {}) {
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
