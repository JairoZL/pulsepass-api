import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { tickets } from '../../test/mocks/datos'
import { errorApi } from '../../test/mocks/handlers'
import { server } from '../../test/mocks/servidor'
import { contarPeticiones, renderApp } from '../../test/utils'

// PRD Frontend §9.5, FR-FE-TKT-001 y DES-10
describe('Compra de una entrada', () => {
  it('TEST-FE-TKT-001/004: confirmar envía userEmail, eventCode y type (nunca el precio) y va a la confirmación', async () => {
    let cuerpo: unknown
    server.use(
      http.post('/api/tickets', async ({ request }) => {
        cuerpo = await request.json()
        return HttpResponse.json({ ...tickets[0]!, ticketCode: 'TKT-NUEVO1', type: 'VIP' }, { status: 201 })
      }),
    )
    const { user, router } = renderApp('/eventos/CMF-2026', { usuario: 'andrea' })

    await user.click(await screen.findByRole('radio', { name: 'VIP' }))
    await user.click(screen.getByRole('button', { name: 'Comprar entrada' }))

    const modal = await screen.findByRole('dialog', { name: 'Confirmar compra' })
    expect(within(modal).getByText('Caribbean Music Fest 2026')).toBeInTheDocument()
    expect(within(modal).getByText('VIP')).toBeInTheDocument()
    expect(within(modal).getByText('andrea@pulsepass.test')).toBeInTheDocument()

    await user.click(within(modal).getByRole('button', { name: 'Confirmar' }))

    expect(await screen.findByRole('heading', { name: '¡Compra realizada con éxito!' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/compra/confirmacion/TKT-NUEVO1')
    expect(cuerpo).toEqual({ userEmail: 'andrea@pulsepass.test', eventCode: 'CMF-2026', type: 'VIP' })
  })

  it('"Cancelar" cierra el modal sin comprar', async () => {
    const compras = contarPeticiones('POST', '/api/tickets')
    const { user } = renderApp('/eventos/CMF-2026', { usuario: 'andrea' })

    await user.click(await screen.findByRole('button', { name: 'Comprar entrada' }))
    const modal = await screen.findByRole('dialog', { name: 'Confirmar compra' })
    await user.click(within(modal).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(compras()).toBe(0)
  })

  it('TEST-FE-TKT-003: un 409 (sin cupo) se muestra DENTRO del modal, que sigue abierto', async () => {
    server.use(http.post('/api/tickets', () => errorApi(409, 'No hay cupo disponible para el evento CMF-2026')))
    const { user, router } = renderApp('/eventos/CMF-2026', { usuario: 'andrea' })

    await user.click(await screen.findByRole('button', { name: 'Comprar entrada' }))
    const modal = await screen.findByRole('dialog', { name: 'Confirmar compra' })
    await user.click(within(modal).getByRole('button', { name: 'Confirmar' }))

    expect(await within(modal).findByText('No hay cupo disponible para el evento CMF-2026')).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: 'Confirmar compra' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/eventos/CMF-2026')
  })

  it('un 401 al comprar lleva al login y luego permite volver al evento', async () => {
    server.use(http.post('/api/tickets', () => errorApi(401, 'Token expirado')))
    const { user, router } = renderApp('/eventos/CMF-2026', { usuario: 'andrea' })

    await user.click(await screen.findByRole('button', { name: 'Comprar entrada' }))
    await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Confirmar' }))

    expect(await screen.findByText('Tu sesión expiró. Inicia sesión de nuevo.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(router.state.location.state).toMatchObject({ desde: '/eventos/CMF-2026' })
  })
})
