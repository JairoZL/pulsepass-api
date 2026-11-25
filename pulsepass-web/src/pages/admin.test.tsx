import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { artistas, borrador } from '../test/mocks/datos'
import { errorApi } from '../test/mocks/handlers'
import { server } from '../test/mocks/servidor'
import { contarPeticiones, renderApp } from '../test/utils'

type Usuario = ReturnType<typeof renderApp>['user']

async function llenarEvento(user: Usuario) {
  await user.type(await screen.findByLabelText('Código del evento'), 'rock-2027')
  await user.type(screen.getByLabelText('Nombre'), 'Rock al Parque Caribe')
  await user.type(screen.getByLabelText('Descripción'), 'Tres escenarios frente al mar.')
  await user.selectOptions(screen.getByLabelText('Categoría'), 'MUSIC')
  await user.clear(screen.getByLabelText('Edad mínima'))
  await user.type(screen.getByLabelText('Edad mínima'), '16')
  fireEvent.change(screen.getByLabelText('Fecha y hora'), { target: { value: '2027-03-20T19:30' } })
  await screen.findByRole('option', { name: 'Marina Convention Center (VEN-SMR-01)' })
  await user.selectOptions(screen.getByLabelText('Sede'), 'VEN-SMR-01')
}

describe('ADMIN — crear evento (FR-FE-ADM-002, §9.17)', () => {
  it('TEST-FE-ADM-001: campos vacíos -> errores y 0 peticiones', async () => {
    const creaciones = contarPeticiones('POST', '/api/events')
    const { user } = renderApp('/admin/eventos/nuevo', { usuario: 'admin' })

    await user.click(await screen.findByRole('button', { name: 'Crear evento' }))

    expect(await screen.findByText('Ingresa el código del evento')).toBeInTheDocument()
    expect(screen.getByText('Elige la sede')).toBeInTheDocument()
    expect(screen.getByText('Elige la fecha y hora')).toBeInTheDocument()
    expect(creaciones()).toBe(0)
  })

  it('TEST-FE-ADM-002: 201 -> navega a gestión y queda en "Creados en esta sesión"', async () => {
    let cuerpo: unknown
    server.use(
      http.post('/api/events', async ({ request }) => {
        cuerpo = await request.json()
        return HttpResponse.json(
          { ...borrador, ...(cuerpo as object), id: 50, status: 'DRAFT', venueName: 'Marina Convention Center', artists: [] },
          { status: 201 },
        )
      }),
    )
    const { user, router } = renderApp('/admin/eventos/nuevo', { usuario: 'admin' })

    await llenarEvento(user)
    await user.click(screen.getByRole('button', { name: 'Crear evento' }))

    expect(await screen.findByRole('heading', { level: 1, name: /Rock al Parque Caribe/ })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/admin/eventos/ROCK-2027')
    expect(screen.getByText('Evento creado como borrador.')).toBeInTheDocument()
    expect(cuerpo).toEqual({
      eventCode: 'ROCK-2027',
      name: 'Rock al Parque Caribe',
      description: 'Tres escenarios frente al mar.',
      category: 'MUSIC',
      eventDate: '2027-03-20T19:30',
      minimumAge: 16,
      venueCode: 'VEN-SMR-01',
    })

    await user.click(within(screen.getByRole('main')).getByRole('link', { name: 'Eventos' }))
    expect(await screen.findByRole('link', { name: 'Gestionar ROCK-2027' })).toBeInTheDocument()
  })

  it('TEST-FE-ADM-003: un 400 con details se muestra en cada campo', async () => {
    server.use(http.post('/api/events', () => errorApi(400, 'Validation failed', { eventDate: 'must be a future date' })))
    const { user } = renderApp('/admin/eventos/nuevo', { usuario: 'admin' })

    await llenarEvento(user)
    await user.click(screen.getByRole('button', { name: 'Crear evento' }))

    expect(await screen.findByText('must be a future date')).toBeInTheDocument()
    expect(screen.getByLabelText('Fecha y hora')).toHaveAttribute('aria-invalid', 'true')
  })

  it('un 409 (código duplicado) se muestra arriba del formulario', async () => {
    server.use(http.post('/api/events', () => errorApi(409, 'Event code already exists: ROCK-2027')))
    const { user } = renderApp('/admin/eventos/nuevo', { usuario: 'admin' })

    await llenarEvento(user)
    await user.click(screen.getByRole('button', { name: 'Crear evento' }))

    expect(await screen.findByText('Event code already exists: ROCK-2027')).toBeInTheDocument()
  })
})

describe('ADMIN — eventos y gestión (§9.16, §9.18)', () => {
  it('buscar por código abre la gestión del evento', async () => {
    const { user, router } = renderApp('/admin/eventos', { usuario: 'admin' })

    expect(await screen.findByText('Aún no has creado eventos en esta sesión.')).toBeInTheDocument()
    await user.type(screen.getByLabelText('Buscar evento por código'), 'brr-2027')
    await user.click(screen.getByRole('button', { name: 'Abrir' }))

    expect(await screen.findByRole('heading', { level: 1, name: /Festival en borrador/ })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/admin/eventos/BRR-2027')
  })

  it('un código inexistente muestra "Evento no encontrado"', async () => {
    renderApp('/admin/eventos/NOPE-1', { usuario: 'admin' })

    expect(await screen.findByRole('heading', { name: 'Evento no encontrado' })).toBeInTheDocument()
  })

  it('TEST-FE-ADM-004: publicar con confirmación -> badge PUBLISHED', async () => {
    const { user } = renderApp('/admin/eventos/BRR-2027', { usuario: 'admin' })

    expect(await screen.findByText('DRAFT')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Publicar evento' }))
    const modal = await screen.findByRole('dialog', { name: 'Publicar evento' })
    await user.click(within(modal).getByRole('button', { name: 'Sí, publicar' }))

    expect(await screen.findByText('PUBLISHED')).toBeInTheDocument()
    expect(screen.getByText('Evento publicado.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Publicar evento' })).toBeDisabled()
  })

  it('TEST-FE-ADM-005: un 409 al publicar se muestra en un aviso', async () => {
    server.use(http.patch('/api/events/:eventCode/publish', () => errorApi(409, 'Transición de estado inválida')))
    const { user } = renderApp('/admin/eventos/BRR-2027', { usuario: 'admin' })

    await user.click(await screen.findByRole('button', { name: 'Publicar evento' }))
    await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Sí, publicar' }))

    expect(await screen.findByText('Transición de estado inválida')).toBeInTheDocument()
    expect(screen.getByText('DRAFT')).toBeInTheDocument()
  })

  it('un evento publicado no se puede volver a publicar', async () => {
    renderApp('/admin/eventos/CMF-2026', { usuario: 'admin' })

    expect(await screen.findByRole('button', { name: 'Publicar evento' })).toBeDisabled()
  })

  it('TEST-FE-ADM-006: asociar artista -> aparece en la lista; los ya asociados están bloqueados', async () => {
    const { user } = renderApp('/admin/eventos/BRR-2027', { usuario: 'admin' })

    expect(await screen.findByText('Este evento aún no tiene artistas.')).toBeInTheDocument()
    await screen.findByRole('option', { name: 'Neon Waves' })
    await user.selectOptions(screen.getByLabelText('Agregar artista'), String(artistas[1]!.id))
    await user.click(screen.getByRole('button', { name: 'Asociar' }))

    const lista = await screen.findByRole('list', { name: 'Artistas del evento' })
    expect(within(lista).getByRole('link', { name: 'Neon Waves' })).toBeInTheDocument()
    await waitFor(() => expect(screen.getByRole('option', { name: 'Neon Waves (ya asociado)' })).toBeDisabled())
  })

  it('TEST-FE-ADM-006: un artista duplicado (409) muestra el mensaje del backend', async () => {
    server.use(http.post('/api/events/:eventCode/artists/:artistId', () => errorApi(409, 'El artista ya está asociado')))
    const { user } = renderApp('/admin/eventos/BRR-2027', { usuario: 'admin' })

    await screen.findByRole('option', { name: 'Solar Beat' })
    await user.selectOptions(screen.getByLabelText('Agregar artista'), String(artistas[0]!.id))
    await user.click(screen.getByRole('button', { name: 'Asociar' }))

    expect(await screen.findByText('El artista ya está asociado')).toBeInTheDocument()
  })
})
