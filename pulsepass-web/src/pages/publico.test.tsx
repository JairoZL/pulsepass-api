import { screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { errorApi } from '../test/mocks/handlers'
import { evento } from '../test/mocks/datos'
import { server } from '../test/mocks/servidor'
import { contarPeticiones, renderApp } from '../test/utils'

describe('Inicio (PRD Frontend §9.3)', () => {
  it('muestra próximos eventos y artistas; "Crear cuenta" solo para anónimos', async () => {
    renderApp('/')

    expect(await screen.findByRole('heading', { name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Solar Beat' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Crear cuenta' })).toBeInTheDocument()
  })

  it('con sesión iniciada no ofrece "Crear cuenta"', async () => {
    renderApp('/', { usuario: 'andrea' })

    expect(await screen.findByRole('button', { name: /andrea/ })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Crear cuenta' })).not.toBeInTheDocument()
  })
})

describe('Cartelera (FR-FE-EVT-001/002)', () => {
  it('TEST-FE-EVT-001: lista los eventos publicados con su contador', async () => {
    renderApp('/eventos')

    expect(await screen.findByText('3 eventos')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Noche de Jazz y Cultura' })).toBeInTheDocument()
  })

  it('TEST-FE-EVT-002: buscar por nombre filtra y guarda el texto en la URL', async () => {
    const { user, router } = renderApp('/eventos')
    await screen.findByText('3 eventos')

    await user.type(screen.getByRole('searchbox', { name: 'Buscar por nombre' }), 'jazz')

    expect(await screen.findByText('1 evento')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Caribbean Music Fest 2026' })).not.toBeInTheDocument()
    await waitFor(() => expect(router.state.location.search).toBe('?q=jazz'))
  })

  it('la categoría de la URL filtra; un valor inventado se ignora (FE-002)', async () => {
    const { unmount } = renderApp('/eventos?categoria=CULTURE')
    expect(await screen.findByText('1 evento')).toBeInTheDocument()
    unmount()

    renderApp('/eventos?categoria=HACKEO')
    expect(await screen.findByText('3 eventos')).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Categoría' })).toHaveValue('')
  })

  it('el filtro por artista usa /api/events/by-artist', async () => {
    const porArtista = contarPeticiones('GET', '/api/events/by-artist')
    renderApp('/eventos?artista=Solar%20Beat')

    expect(await screen.findByText('1 evento')).toBeInTheDocument()
    expect(porArtista()).toBe(1)
  })

  it('"Limpiar" quita todos los filtros', async () => {
    const { user, router } = renderApp('/eventos?q=nada&categoria=SPORTS')
    expect(await screen.findByText('Sin resultados para tu búsqueda')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Limpiar' }))

    expect(await screen.findByText('3 eventos')).toBeInTheDocument()
    expect(router.state.location.search).toBe('')
    expect(screen.getByRole('searchbox', { name: 'Buscar por nombre' })).toHaveValue('')
  })

  it('TEST-FE-EVT-003: sin eventos publicados muestra el estado vacío', async () => {
    server.use(http.get('/api/events/published', () => HttpResponse.json([])))
    renderApp('/eventos')

    expect(await screen.findByText('Aún no hay eventos publicados')).toBeInTheDocument()
  })

  it('un 500 muestra un mensaje genérico con "Reintentar"', async () => {
    server.use(http.get('/api/events/published', () => errorApi(500, 'NullPointerException en EventServiceImpl')))
    renderApp('/eventos')

    expect(await screen.findByText('Ocurrió un error inesperado. Inténtalo más tarde.')).toBeInTheDocument()
    expect(screen.queryByText(/NullPointer/)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })
})

describe('Detalle de evento (FR-FE-EVT-003/004)', () => {
  it('TEST-FE-EVT-004: muestra datos, artistas y sede con su capacidad', async () => {
    renderApp('/eventos/CMF-2026')

    expect(await screen.findByRole('heading', { level: 1, name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
    expect(screen.getByText('Edad mínima: 18')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Solar Beat' })).toHaveAttribute('href', '/artistas/1')
    expect(screen.getByRole('link', { name: 'Neon Waves' })).toBeInTheDocument()
    expect(await screen.findByText('Santa Marta · Capacidad 3')).toBeInTheDocument()
  })

  it('TEST-FE-EVT-005: un borrador (404 para anónimos) se muestra como "no disponible", sin datos', async () => {
    renderApp('/eventos/BRR-2027')

    expect(await screen.findByRole('heading', { name: 'Evento no disponible' })).toBeInTheDocument()
    expect(screen.queryByText('Festival en borrador')).not.toBeInTheDocument()
  })

  it('un 403 también se muestra como "no disponible" (GAP-02)', async () => {
    server.use(http.get('/api/events/:eventCode', () => errorApi(403, 'Forbidden')))
    renderApp('/eventos/CMF-2026')

    expect(await screen.findByRole('heading', { name: 'Evento no disponible' })).toBeInTheDocument()
  })

  it('TEST-FE-EVT-006: anónimo ve "Inicia sesión para comprar" y vuelve al evento tras ingresar', async () => {
    const { user, router } = renderApp('/eventos/CMF-2026')

    await user.click(await screen.findByRole('link', { name: 'Inicia sesión para comprar' }))

    expect(await screen.findByRole('heading', { name: 'Inicia sesión' })).toBeInTheDocument()
    expect(router.state.location.state).toMatchObject({ desde: '/eventos/CMF-2026' })
  })

  it('STAFF no ve el panel de compra (solo USER compra, §12.4)', async () => {
    renderApp('/eventos/CMF-2026', { usuario: 'laura' })

    expect(await screen.findByRole('heading', { level: 1, name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
    await screen.findByRole('button', { name: /laura/ })
    expect(screen.queryByRole('button', { name: 'Comprar entrada' })).not.toBeInTheDocument()
  })

  it('evento agotado: explica el motivo y deshabilita la compra', async () => {
    server.use(http.get('/api/events/:eventCode', () => HttpResponse.json({ ...evento, status: 'SOLD_OUT' })))
    renderApp('/eventos/CMF-2026', { usuario: 'andrea' })

    expect(await screen.findByText('Las entradas de este evento están agotadas.')).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'Comprar entrada' })).toBeDisabled()
  })
})

describe('Artistas (FR-FE-ART-001..003)', () => {
  it('TEST-FE-ART-001: lista los artistas activos', async () => {
    renderApp('/artistas')

    expect(await screen.findByRole('link', { name: /Caribbean Sound/ })).toHaveAttribute('href', '/artistas/3')
    expect(screen.getByRole('link', { name: /Solar Beat/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Neon Waves/ })).toBeInTheDocument()
  })

  it('TEST-FE-ART-002: el detalle muestra el perfil y sus eventos', async () => {
    renderApp('/artistas/1')

    expect(await screen.findByRole('heading', { level: 1, name: 'Solar Beat' })).toBeInTheDocument()
    expect(screen.getByText('Electronic · Colombia')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Caribbean Music Fest 2026' })).toBeInTheDocument()
  })

  it('TEST-FE-ART-003: un id no numérico muestra 404 sin llamar a la API', async () => {
    const peticiones = contarPeticiones('GET', '/api/artists')
    renderApp('/artistas/abc')

    expect(await screen.findByText('No encontramos esta página.')).toBeInTheDocument()
    expect(peticiones()).toBe(0)
  })

  it('un artista inexistente (404) muestra la página no encontrada', async () => {
    renderApp('/artistas/99')

    expect(await screen.findByText('No encontramos esta página.')).toBeInTheDocument()
  })
})

describe('Sedes (FR-FE-VEN-001/002)', () => {
  it('TEST-FE-VEN-001: lista y detalle de la sede', async () => {
    const { user } = renderApp('/sedes')

    await user.click(await screen.findByRole('link', { name: /Marina Convention Center/ }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Marina Convention Center' })).toBeInTheDocument()
    expect(screen.getByText('Carrera 1 # 22-58')).toBeInTheDocument()
    expect(screen.getByText('3 personas')).toBeInTheDocument()
  })

  it('una sede inexistente muestra la página no encontrada', async () => {
    renderApp('/sedes/NO-EXISTE')

    expect(await screen.findByText('No encontramos esta página.')).toBeInTheDocument()
  })
})
