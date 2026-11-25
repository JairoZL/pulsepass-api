import type { UsuarioAutenticado } from '../../funcionalidades/auth/tipos'
import type { ArtistResponse } from '../../funcionalidades/artistas/tipos'
import type { EventResponse, EventSummaryResponse } from '../../funcionalidades/eventos/tipos'
import type { VenueResponse } from '../../funcionalidades/sedes/tipos'
import type { TicketResponse } from '../../funcionalidades/tickets/tipos'
import type { UserResponse } from '../../funcionalidades/usuarios/tipos'

// Datos del escenario de referencia (PRD Frontend §22): VEN-SMR-01, CMF-2026, Andrea, Carlos, Laura y admin.
// Fechas fijas (QT-FE-009): las pruebas no dependen del reloj real.

export const CONTRASENA_VALIDA = 'UnaFraseSeguraLarga2026!'

export const sede: VenueResponse = {
  id: 1,
  code: 'VEN-SMR-01',
  name: 'Marina Convention Center',
  city: 'Santa Marta',
  address: 'Carrera 1 # 22-58',
  capacity: 3,
  active: true,
}

export const artistas: ArtistResponse[] = [
  { id: 1, stageName: 'Solar Beat', country: 'Colombia', genre: 'Electronic', active: true },
  { id: 2, stageName: 'Neon Waves', country: 'Mexico', genre: 'Synthwave', active: true },
  { id: 3, stageName: 'Caribbean Sound', country: 'Colombia', genre: 'Vallenato', active: true },
]

export const evento: EventResponse = {
  id: 1,
  eventCode: 'CMF-2026',
  name: 'Caribbean Music Fest 2026',
  description: 'El festival de música del Caribe colombiano.',
  category: 'MUSIC',
  status: 'PUBLISHED',
  eventDate: '2026-12-12T20:00:00',
  minimumAge: 18,
  venueCode: 'VEN-SMR-01',
  venueName: 'Marina Convention Center',
  artists: [artistas[0]!, artistas[1]!],
}

export const borrador: EventResponse = {
  ...evento,
  id: 2,
  eventCode: 'BRR-2027',
  name: 'Festival en borrador',
  status: 'DRAFT',
  artists: [],
}

export function resumen(e: EventResponse): EventSummaryResponse {
  return {
    eventCode: e.eventCode,
    name: e.name,
    category: e.category,
    status: e.status,
    eventDate: e.eventDate,
    venueName: e.venueName,
  }
}

export const eventosPublicados: EventSummaryResponse[] = [
  resumen(evento),
  {
    eventCode: 'JAZZ-2026',
    name: 'Noche de Jazz y Cultura',
    category: 'CULTURE',
    status: 'PUBLISHED',
    eventDate: '2026-11-27T19:00:00',
    venueName: 'Teatro Heredia',
  },
  {
    eventCode: 'STAND-2026',
    name: 'Stand-up Costeño',
    category: 'ENTERTAINMENT',
    status: 'SOLD_OUT',
    eventDate: '2026-12-04T21:00:00',
    venueName: 'Marina Convention Center',
  },
]

// Usuarios del escenario. El token de prueba es "token-<username>"
export const usuarios: Record<string, UsuarioAutenticado> = {
  andrea: { id: 1, username: 'andrea', email: 'andrea@pulsepass.test', roles: ['USER'] },
  carlos: { id: 2, username: 'carlos', email: 'carlos@pulsepass.test', roles: ['USER'] },
  laura: { id: 3, username: 'laura', email: 'laura@pulsepass.test', roles: ['USER', 'STAFF'] },
  admin: { id: 4, username: 'admin', email: 'admin@pulsepass.test', roles: ['USER', 'ADMIN'] },
}
export type NombreUsuario = 'andrea' | 'carlos' | 'laura' | 'admin'

export function perfil(nombre: NombreUsuario): UserResponse {
  const usuario = usuarios[nombre]!
  return { id: usuario.id, username: usuario.username, email: usuario.email, active: true }
}

export const tickets: TicketResponse[] = [
  {
    id: 1,
    ticketCode: 'TKT-8F2A1C',
    type: 'VIP',
    price: 250000,
    status: 'PAID',
    purchaseDate: '2026-11-05T14:32:00',
    userEmail: 'andrea@pulsepass.test',
    eventCode: 'CMF-2026',
    eventName: 'Caribbean Music Fest 2026',
  },
  {
    id: 2,
    ticketCode: 'TKT-5D0E77',
    type: 'GENERAL',
    price: 120000,
    status: 'USED',
    purchaseDate: '2026-10-01T10:00:00',
    userEmail: 'andrea@pulsepass.test',
    eventCode: 'JAZZ-2026',
    eventName: 'Noche de Jazz y Cultura',
  },
  {
    id: 3,
    ticketCode: 'TKT-91B07D',
    type: 'GENERAL',
    price: 120000,
    status: 'PAID',
    purchaseDate: '2026-11-06T09:15:00',
    userEmail: 'carlos@pulsepass.test',
    eventCode: 'CMF-2026',
    eventName: 'Caribbean Music Fest 2026',
  },
]
