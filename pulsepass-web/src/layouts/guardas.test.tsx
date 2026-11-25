import { screen, waitFor, within } from '@testing-library/react'
import { http } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { obtenerToken } from '../lib/sesionAlmacen'
import { server } from '../test/mocks/servidor'
import { renderApp } from '../test/utils'

describe('Guardas de ruta y sesión (PRD Frontend §12)', () => {
  it('TEST-FE-AUTH-007: anónimo en ruta protegida -> /login con state.desde', async () => {
    const { router } = renderApp('/cuenta')

    expect(await screen.findByRole('heading', { name: 'Inicia sesión' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(router.state.location.state).toMatchObject({ desde: '/cuenta' })
  })

  it('TEST-FE-AUTH-008: USER en /admin/eventos -> /403', async () => {
    const { router } = renderApp('/admin/eventos', { usuario: 'andrea' })

    expect(await screen.findByText('No tienes permisos para acceder a esta sección.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/403')
  })

  it('TEST-FE-AUTH-009: ADMIN en /admin/eventos -> se muestra la página', async () => {
    renderApp('/admin/eventos', { usuario: 'admin' })

    expect(await screen.findByRole('heading', { name: 'Administración de eventos' })).toBeInTheDocument()
  })

  it('STAFF entra a validación de ingreso; USER no', async () => {
    renderApp('/staff/validacion', { usuario: 'laura' })
    expect(await screen.findByRole('heading', { name: 'Validación de ingreso' })).toBeInTheDocument()
  })

  it('USER en /staff/validacion -> /403', async () => {
    renderApp('/staff/validacion', { usuario: 'carlos' })
    expect(await screen.findByText('No tienes permisos para acceder a esta sección.')).toBeInTheDocument()
  })

  it('TEST-FE-AUTH-010: cerrar sesión limpia token, sessionStorage y caché', async () => {
    const { user, queryClient, router } = renderApp('/', { usuario: 'andrea' })
    queryClient.setQueryData(['tickets', 'usuario', 'andrea@pulsepass.test'], [])

    const encabezado = screen.getByRole('banner')
    await user.click(await within(encabezado).findByRole('button', { name: /andrea/ }))
    await user.click(within(encabezado).getByRole('button', { name: 'Salir' }))

    expect(obtenerToken()).toBeNull()
    expect(sessionStorage.getItem('pulsepass.sesion')).toBeNull()
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0)
    expect(await within(encabezado).findByRole('link', { name: 'Ingresar' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/')
  })

  it('TEST-FE-AUTH-011: token vencido al restaurar -> anónimo, sin llamar a la API', async () => {
    const llamadas = vi.fn()
    server.use(
      http.get('/api/auth/me', () => {
        llamadas()
        return new Response(null, { status: 500 })
      }),
    )
    sessionStorage.setItem('pulsepass.sesion', JSON.stringify({ token: 'token-andrea', expiraEn: Date.now() - 1000 }))

    const { router } = renderApp('/cuenta')

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(llamadas).not.toHaveBeenCalled()
  })

  it('FR-FE-AUTH-004: token vigente al recargar -> la sesión se restaura', async () => {
    renderApp('/cuenta', { usuario: 'andrea' })

    expect(await screen.findByRole('heading', { name: 'Mi cuenta' })).toBeInTheDocument()
  })
})
