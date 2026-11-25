import { http, HttpResponse } from 'msw'
import {
  artistas,
  borrador,
  CONTRASENA_VALIDA,
  evento,
  eventosPublicados,
  perfil,
  resumen,
  sede,
  tickets,
  usuarios,
  type NombreUsuario,
} from './datos'

// PRD Frontend §17 y §20: MSW simula la API a nivel de red. Estos son los 23 endpoints con su
// respuesta "feliz" por defecto. Cada prueba puede reemplazar uno con server.use(...).
// Los handlers no guardan estado: cada prueba empieza igual (QT-FE-009).

const API = '/api'

export function errorApi(status: number, message: string, details: Record<string, string> = {}) {
  const textos: Record<number, string> = {
    400: 'Bad Request',
    401: 'Unauthorized',
    403: 'Forbidden',
    404: 'Not Found',
    409: 'Conflict',
    500: 'Internal Server Error',
  }
  return HttpResponse.json(
    { timestamp: '2026-11-25T12:00:00', status, error: textos[status] ?? 'Error', message, details },
    { status },
  )
}

// El token de prueba es "token-<username>": de ahí se saca quién llama
function usuarioDe(request: Request) {
  const token = request.headers.get('Authorization')?.replace('Bearer token-', '')
  return token && token in usuarios ? usuarios[token as NombreUsuario] : undefined
}

const esPersonal = (request: Request) => {
  const usuario = usuarioDe(request)
  return Boolean(usuario?.roles.some((rol) => rol === 'STAFF' || rol === 'ADMIN'))
}

export const handlers = [
  // ---------- Autenticación (A1, A2) ----------
  http.post(`${API}/auth/login`, async ({ request }) => {
    const cuerpo = (await request.json()) as { identifier?: string; password?: string }
    const nombre = Object.keys(usuarios).find(
      (clave) => clave === cuerpo.identifier || usuarios[clave]?.email === cuerpo.identifier,
    )
    if (!nombre || cuerpo.password !== CONTRASENA_VALIDA) return errorApi(401, 'Credenciales inválidas')
    return HttpResponse.json({ accessToken: `token-${nombre}`, tokenType: 'Bearer', expiresIn: 900 })
  }),
  http.get(`${API}/auth/me`, ({ request }) => {
    const usuario = usuarioDe(request)
    if (!usuario) return errorApi(401, 'Se requiere autenticación')
    return HttpResponse.json({ id: usuario.id, username: usuario.username, roles: usuario.roles })
  }),

  // ---------- Usuarios (12, 13, 14) ----------
  http.post(`${API}/users`, async ({ request }) => {
    const cuerpo = (await request.json()) as { username: string; email: string }
    return HttpResponse.json({ id: 99, username: cuerpo.username, email: cuerpo.email, active: true }, { status: 201 })
  }),
  http.get(`${API}/users/by-email`, ({ request }) => {
    const usuario = usuarioDe(request)
    if (!usuario) return errorApi(401, 'Se requiere autenticación')
    return HttpResponse.json(perfil(usuario.username as NombreUsuario))
  }),
  http.get(`${API}/users/by-username`, ({ request }) => {
    const usuario = usuarioDe(request)
    if (!usuario) return errorApi(401, 'Se requiere autenticación')
    return HttpResponse.json(perfil(usuario.username as NombreUsuario))
  }),

  // ---------- Sedes (1, 2) ----------
  http.get(`${API}/venues/active`, () => HttpResponse.json([sede])),
  http.get(`${API}/venues/:code`, ({ params }) =>
    params.code === sede.code ? HttpResponse.json(sede) : errorApi(404, `Venue not found: ${String(params.code)}`),
  ),

  // ---------- Artistas (9, 10, 11) ----------
  http.get(`${API}/artists/active`, () => HttpResponse.json(artistas)),
  http.get(`${API}/artists/by-stage-name`, ({ request }) => {
    const nombre = new URL(request.url).searchParams.get('stageName')
    const artista = artistas.find((a) => a.stageName === nombre)
    return artista ? HttpResponse.json(artista) : errorApi(404, `Artist not found: ${nombre}`)
  }),
  http.get(`${API}/artists/:id`, ({ params }) => {
    const artista = artistas.find((a) => a.id === Number(params.id))
    return artista ? HttpResponse.json(artista) : errorApi(404, `Artist not found: ${String(params.id)}`)
  }),

  // ---------- Eventos (3..8) y tickets pagados (18) ----------
  http.get(`${API}/events/published`, () => HttpResponse.json(eventosPublicados)),
  http.get(`${API}/events/by-artist`, ({ request }) => {
    const nombre = new URL(request.url).searchParams.get('stageName')
    const lista = [evento, ...(esPersonal(request) ? [borrador] : [])]
      .filter((e) => e.artists.some((a) => a.stageName === nombre))
      .map(resumen)
    return HttpResponse.json(lista)
  }),
  http.get(`${API}/events/:eventCode/tickets/paid`, ({ params }) =>
    HttpResponse.json(tickets.filter((t) => t.eventCode === params.eventCode && t.status === 'PAID')),
  ),
  http.get(`${API}/events/:eventCode`, ({ params, request }) => {
    if (params.eventCode === evento.eventCode) return HttpResponse.json(evento)
    if (params.eventCode === borrador.eventCode && esPersonal(request)) return HttpResponse.json(borrador)
    return errorApi(404, `Event not found: ${String(params.eventCode)}`)
  }),
  http.post(`${API}/events`, async ({ request }) => {
    const cuerpo = (await request.json()) as Record<string, unknown>
    return HttpResponse.json(
      { ...borrador, ...cuerpo, id: 50, status: 'DRAFT', venueName: sede.name, artists: [] },
      { status: 201 },
    )
  }),
  http.patch(`${API}/events/:eventCode/publish`, () => HttpResponse.json({ ...borrador, status: 'PUBLISHED' })),
  http.post(`${API}/events/:eventCode/artists/:artistId`, ({ params }) => {
    const artista = artistas.find((a) => a.id === Number(params.artistId))
    return HttpResponse.json({ ...borrador, artists: artista ? [artista] : [] })
  }),

  // ---------- Tickets (15, 16, 17, 19, 20) ----------
  http.get(`${API}/tickets/by-user`, ({ request }) => {
    const email = new URL(request.url).searchParams.get('email')
    return HttpResponse.json(tickets.filter((t) => t.userEmail === email))
  }),
  http.post(`${API}/tickets`, async ({ request }) => {
    const cuerpo = (await request.json()) as { userEmail: string; eventCode: string; type: string }
    return HttpResponse.json(
      { ...tickets[0]!, id: 10, ticketCode: 'TKT-NUEVO1', type: cuerpo.type, userEmail: cuerpo.userEmail },
      { status: 201 },
    )
  }),
  http.get(`${API}/tickets/:ticketCode`, ({ params, request }) => {
    const ticket = tickets.find((t) => t.ticketCode === params.ticketCode)
    const usuario = usuarioDe(request)
    if (!usuario) return errorApi(401, 'Se requiere autenticación')
    // Igual que el backend: ajeno o inexistente -> 403 para un USER
    if (!ticket || (ticket.userEmail !== usuario.email && !esPersonal(request))) {
      return errorApi(403, 'No tiene permisos para realizar esta operación')
    }
    return HttpResponse.json(ticket)
  }),
  http.patch(`${API}/tickets/:ticketCode/cancel`, ({ params }) => {
    const ticket = tickets.find((t) => t.ticketCode === params.ticketCode)
    return ticket ? HttpResponse.json({ ...ticket, status: 'CANCELLED' }) : errorApi(404, 'Ticket not found')
  }),
  http.patch(`${API}/tickets/:ticketCode/use`, ({ params }) => {
    const ticket = tickets.find((t) => t.ticketCode === params.ticketCode)
    return ticket ? HttpResponse.json({ ...ticket, status: 'USED' }) : errorApi(404, 'Ticket not found')
  }),
]
