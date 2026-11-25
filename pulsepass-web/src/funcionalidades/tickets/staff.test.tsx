import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { errorApi } from '../../test/mocks/handlers'
import { server } from '../../test/mocks/servidor'
import { contarPeticiones, renderApp } from '../../test/utils'

describe('Validación de ingreso (FR-FE-STF-001, §9.14)', () => {
  it('TEST-FE-STF-001: buscar y marcar como usado -> "Ingreso registrado." y el campo se limpia', async () => {
    const { user } = renderApp('/staff/validacion', { usuario: 'laura' })

    const campo = await screen.findByLabelText('Código de ticket')
    expect(campo).toHaveFocus()
    await user.type(campo, 'TKT-8F2A1C{Enter}')

    expect(await screen.findByRole('heading', { name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
    expect(screen.getByText(/Titular: andrea@pulsepass.test/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Marcar como usado' }))

    expect(await screen.findByText('Ingreso registrado.')).toBeInTheDocument()
    expect(screen.getByText('USED')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Marcar como usado' })).not.toBeInTheDocument()
    expect(campo).toHaveValue('')
  })

  it('TEST-FE-STF-002: un ticket ya utilizado (409) muestra el mensaje del backend', async () => {
    server.use(http.patch('/api/tickets/:ticketCode/use', () => errorApi(409, 'El ticket ya fue utilizado')))
    const { user } = renderApp('/staff/validacion', { usuario: 'laura' })

    await user.type(await screen.findByLabelText('Código de ticket'), 'TKT-8F2A1C{Enter}')
    await user.click(await screen.findByRole('button', { name: 'Marcar como usado' }))

    expect(await screen.findByText('El ticket ya fue utilizado')).toBeInTheDocument()
    expect(screen.queryByText('Ingreso registrado.')).not.toBeInTheDocument()
  })

  it('TEST-FE-STF-003: un código inexistente muestra "Ticket no encontrado."', async () => {
    const { user } = renderApp('/staff/validacion', { usuario: 'laura' })

    await user.type(await screen.findByLabelText('Código de ticket'), 'TKT-NOEXISTE{Enter}')

    expect(await screen.findByText('Ticket no encontrado.')).toBeInTheDocument()
  })

  it('un código vacío se valida sin llamar a la API', async () => {
    const consultas = contarPeticiones('GET', '/api/tickets/')
    const { user } = renderApp('/staff/validacion', { usuario: 'laura' })

    await user.click(await screen.findByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('Ingresa el código del ticket')).toBeInTheDocument()
    expect(consultas()).toBe(0)
  })

  it('un ticket que no está PAID no ofrece "Marcar como usado"', async () => {
    const { user } = renderApp('/staff/validacion', { usuario: 'laura' })

    await user.type(await screen.findByLabelText('Código de ticket'), 'TKT-5D0E77{Enter}')

    expect(await screen.findByText(/no está habilitado para ingresar/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Marcar como usado' })).not.toBeInTheDocument()
  })
})

describe('Tickets pagados por evento (FR-FE-STF-002, §9.15)', () => {
  it('TEST-FE-STF-004: tabla con los tickets pagados del evento y su total', async () => {
    renderApp('/staff/eventos/CMF-2026/tickets', { usuario: 'laura' })

    const tabla = await screen.findByRole('table', { name: 'Tickets pagados de CMF-2026' })
    expect(within(tabla).getAllByRole('row')).toHaveLength(3) // encabezado + 2
    expect(within(tabla).getByText('TKT-8F2A1C')).toBeInTheDocument()
    expect(within(tabla).getByText('carlos@pulsepass.test')).toBeInTheDocument()
    expect(screen.getByText('Total: 2')).toBeInTheDocument()
  })

  it('"Usar" marca el ticket y avisa', async () => {
    const usos = contarPeticiones('PATCH', '/api/tickets/TKT-91B07D/use')
    const { user } = renderApp('/staff/eventos/CMF-2026/tickets', { usuario: 'admin' })

    const tabla = await screen.findByRole('table')
    await user.click(within(tabla).getByRole('button', { name: 'Usar ticket TKT-91B07D' }))

    expect(await screen.findByText('Ingreso registrado: TKT-91B07D.')).toBeInTheDocument()
    expect(usos()).toBe(1)
  })

  it('sin código de evento pide uno y "Ir" navega a su lista', async () => {
    const { user, router } = renderApp('/staff/eventos/tickets', { usuario: 'laura' })

    expect(await screen.findByText('Ingresa el código de un evento para ver sus tickets pagados.')).toBeInTheDocument()
    await user.type(screen.getByLabelText('Evento'), 'cmf-2026')
    await user.click(screen.getByRole('button', { name: 'Ir' }))

    expect(await screen.findByRole('table', { name: 'Tickets pagados de CMF-2026' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/staff/eventos/CMF-2026/tickets')
  })

  it('sin tickets pagados muestra el vacío; un evento inexistente, su aviso', async () => {
    server.use(http.get('/api/events/:eventCode/tickets/paid', () => HttpResponse.json([])))
    const { unmount } = renderApp('/staff/eventos/JAZZ-2026/tickets', { usuario: 'laura' })
    expect(await screen.findByText('No hay tickets pagados')).toBeInTheDocument()
    unmount()

    server.use(http.get('/api/events/:eventCode/tickets/paid', () => errorApi(404, 'Event not found')))
    renderApp('/staff/eventos/NOPE-1/tickets', { usuario: 'laura' })
    expect(await screen.findByText('No encontramos el evento NOPE-1.')).toBeInTheDocument()
  })
})
