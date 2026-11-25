import { z } from 'zod'
import { solicitar } from '../../lib/http'
import { TicketResponseSchema, type TicketType } from './tipos'

const enc = encodeURIComponent

export interface DatosCompra {
  userEmail: string // siempre el de la sesión, nunca de un campo editable (§24.3)
  eventCode: string
  type: TicketType
}

// Operaciones 15..20 de la tabla PRD Frontend §10.3
export const comprarTicket = (datos: DatosCompra) =>
  solicitar('/tickets', TicketResponseSchema, { metodo: 'POST', cuerpo: datos })

export const obtenerTicket = (ticketCode: string) => solicitar(`/tickets/${enc(ticketCode)}`, TicketResponseSchema)

export const obtenerTicketsPorEmail = (email: string) =>
  solicitar(`/tickets/by-user?${new URLSearchParams({ email })}`, z.array(TicketResponseSchema))

export const obtenerTicketsPagados = (eventCode: string) =>
  solicitar(`/events/${enc(eventCode)}/tickets/paid`, z.array(TicketResponseSchema))

export const cancelarTicket = (ticketCode: string) =>
  solicitar(`/tickets/${enc(ticketCode)}/cancel`, TicketResponseSchema, { metodo: 'PATCH' })

export const usarTicket = (ticketCode: string) =>
  solicitar(`/tickets/${enc(ticketCode)}/use`, TicketResponseSchema, { metodo: 'PATCH' })
