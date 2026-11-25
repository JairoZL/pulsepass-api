// PRD Frontend FE-003: todas las rutas en un solo lugar. Ningún componente escribe una URL suelta.
// Las que llevan parámetros son funciones; encodeURIComponent evita romper la URL con códigos raros.
export const RUTAS = {
  inicio: '/',
  eventos: '/eventos',
  evento: (eventCode: string) => `/eventos/${encodeURIComponent(eventCode)}`,
  artistas: '/artistas',
  artista: (id: number) => `/artistas/${id}`,
  sedes: '/sedes',
  sede: (code: string) => `/sedes/${encodeURIComponent(code)}`,
  login: '/login',
  registro: '/registro',
  cuenta: '/cuenta',
  misTickets: '/mis-tickets',
  ticket: (ticketCode: string) => `/mis-tickets/${encodeURIComponent(ticketCode)}`,
  confirmacionCompra: (ticketCode: string) => `/compra/confirmacion/${encodeURIComponent(ticketCode)}`,
  staffValidacion: '/staff/validacion',
  // Sin código: la página pide el evento. Con código: muestra sus tickets pagados
  staffTicketsPagados: (eventCode?: string) =>
    eventCode ? `/staff/eventos/${encodeURIComponent(eventCode)}/tickets` : '/staff/eventos/tickets',
  adminEventos: '/admin/eventos',
  adminCrearEvento: '/admin/eventos/nuevo',
  adminEvento: (eventCode: string) => `/admin/eventos/${encodeURIComponent(eventCode)}`,
  sinPermisos: '/403',
} as const
