import { screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { tickets } from '../../test/mocks/datos'
import { errorApi } from '../../test/mocks/handlers'
import { server } from '../../test/mocks/servidor'
import { renderApp } from '../../test/utils'

const deAndrea = tickets.filter((t) => t.userEmail === 'andrea@pulsepass.test')

describe('Mis tickets (FR-FE-TKT-003, §9.11)', () => {
  it('TEST-FE-TKT-005: lista los tickets de la sesión y filtra por estado', async () => {
    const { user, router } = renderApp('/mis-tickets', { usuario: 'andrea' })

    expect(await screen.findByRole('heading', { name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Noche de Jazz y Cultura' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Usados' }))

    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: 'Caribbean Music Fest 2026' })).not.toBeInTheDocument(),
    )
    expect(screen.getByRole('heading', { name: 'Noche de Jazz y Cultura' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Usados' })).toHaveAttribute('aria-pressed', 'true')
    await waitFor(() => expect(router.state.location.search).toBe('?estado=USED'))
  })

  it('pide los tickets con el correo de la sesión', async () => {
    let correo: string | null = null
    server.use(
      http.get('/api/tickets/by-user', ({ request }) => {
        correo = new URL(request.url).searchParams.get('email')
        return HttpResponse.json([])
      }),
    )
    renderApp('/mis-tickets', { usuario: 'carlos' })

    await screen.findByText('Aún no tienes tickets')
    expect(correo).toBe('carlos@pulsepass.test')
  })

  it('TEST-FE-TKT-006: sin tickets muestra el vacío con enlace a la cartelera', async () => {
    server.use(http.get('/api/tickets/by-user', () => HttpResponse.json([])))
    renderApp('/mis-tickets', { usuario: 'andrea' })

    expect(await screen.findByText('Aún no tienes tickets')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver cartelera' })).toHaveAttribute('href', '/eventos')
  })

  it('TEST-FE-TKT-010: "Cancelar" solo aparece en tickets PAID', async () => {
    renderApp('/mis-tickets', { usuario: 'andrea' })

    expect(await screen.findByRole('button', { name: 'Cancelar ticket TKT-8F2A1C' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar ticket TKT-5D0E77' })).not.toBeInTheDocument()
  })

  it('TEST-FE-TKT-007: cancelar un PAID cierra el modal, avisa y actualiza la lista', async () => {
    let cancelado = false
    server.use(
      http.get('/api/tickets/by-user', () =>
        HttpResponse.json(
          deAndrea.map((t) => (cancelado && t.ticketCode === 'TKT-8F2A1C' ? { ...t, status: 'CANCELLED' } : t)),
        ),
      ),
      http.patch('/api/tickets/:ticketCode/cancel', () => {
        cancelado = true
        return HttpResponse.json({ ...deAndrea[0]!, status: 'CANCELLED' })
      }),
    )
    const { user } = renderApp('/mis-tickets', { usuario: 'andrea' })

    await user.click(await screen.findByRole('button', { name: 'Cancelar ticket TKT-8F2A1C' }))
    const modal = await screen.findByRole('dialog', { name: 'Cancelar ticket' })
    await user.click(within(modal).getByRole('button', { name: 'Sí, cancelar' }))

    expect(await screen.findByText('Ticket cancelado.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(await screen.findByText('CANCELLED')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar ticket TKT-8F2A1C' })).not.toBeInTheDocument()
  })

  it('TEST-FE-TKT-008: un 409 al cancelar se muestra en el modal, que sigue abierto', async () => {
    server.use(
      http.patch('/api/tickets/:ticketCode/cancel', () => errorApi(409, 'El ticket TKT-8F2A1C no se puede cancelar')),
    )
    const { user } = renderApp('/mis-tickets', { usuario: 'andrea' })

    await user.click(await screen.findByRole('button', { name: 'Cancelar ticket TKT-8F2A1C' }))
    const modal = await screen.findByRole('dialog', { name: 'Cancelar ticket' })
    await user.click(within(modal).getByRole('button', { name: 'Sí, cancelar' }))

    expect(await within(modal).findByText('El ticket TKT-8F2A1C no se puede cancelar')).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: 'Cancelar ticket' })).toBeInTheDocument()
  })

  it('"Volver" cierra el modal sin cancelar', async () => {
    const { user } = renderApp('/mis-tickets', { usuario: 'andrea' })

    await user.click(await screen.findByRole('button', { name: 'Cancelar ticket TKT-8F2A1C' }))
    await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Volver' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByText('PAID')).toBeInTheDocument()
  })
})

describe('Detalle de ticket (FR-FE-TKT-004, §9.12)', () => {
  it('muestra código, tipo, precio, fecha y titular, y permite copiar el código', async () => {
    const { user } = renderApp('/mis-tickets/TKT-8F2A1C', { usuario: 'andrea' })

    expect(await screen.findByRole('heading', { level: 1, name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
    expect(screen.getByText('TKT-8F2A1C')).toBeInTheDocument()
    expect(screen.getByText(/250\.000/)).toBeInTheDocument()
    expect(screen.getByText('andrea@pulsepass.test')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Copiar código TKT-8F2A1C' }))

    expect(await screen.findByText('Código copiado.')).toBeInTheDocument()
    expect(await navigator.clipboard.readText()).toBe('TKT-8F2A1C')
  })

  it('TEST-FE-TKT-009: un ticket ajeno (403) se ve como "Ticket no disponible", sin sus datos', async () => {
    renderApp('/mis-tickets/TKT-91B07D', { usuario: 'andrea' })

    expect(await screen.findByRole('heading', { name: 'Ticket no disponible' })).toBeInTheDocument()
    expect(screen.queryByText('carlos@pulsepass.test')).not.toBeInTheDocument()
  })

  it('un ticket USED no ofrece cancelar', async () => {
    renderApp('/mis-tickets/TKT-5D0E77', { usuario: 'andrea' })

    expect(await screen.findByRole('heading', { level: 1, name: 'Noche de Jazz y Cultura' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar ticket' })).not.toBeInTheDocument()
  })
})

describe('Confirmación de compra (§9.13)', () => {
  it('muestra el ticket comprado y los dos caminos siguientes', async () => {
    renderApp('/compra/confirmacion/TKT-8F2A1C', { usuario: 'andrea' })

    expect(await screen.findByRole('heading', { name: '¡Compra realizada con éxito!' })).toBeInTheDocument()
    expect(screen.getByText('Caribbean Music Fest 2026')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ver mis tickets' })).toHaveAttribute('href', '/mis-tickets')
    expect(screen.getByRole('link', { name: 'Seguir explorando' })).toHaveAttribute('href', '/eventos')
  })

  it('un ticket ajeno muestra "Ticket no disponible"', async () => {
    renderApp('/compra/confirmacion/TKT-91B07D', { usuario: 'andrea' })

    expect(await screen.findByRole('heading', { name: 'Ticket no disponible' })).toBeInTheDocument()
  })
})
