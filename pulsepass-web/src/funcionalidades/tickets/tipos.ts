import { z } from 'zod'

export const TicketTypeSchema = z.enum(['GENERAL', 'VIP', 'BACKSTAGE', 'STUDENT'])
export type TicketType = z.infer<typeof TicketTypeSchema>

export const TicketStatusSchema = z.enum(['RESERVED', 'PAID', 'CANCELLED', 'USED'])
export type TicketStatus = z.infer<typeof TicketStatusSchema>

// TicketResponse real del backend. No trae el nombre del titular: se muestra userEmail (brecha documentada)
export const TicketResponseSchema = z.object({
  id: z.number(),
  ticketCode: z.string(),
  type: TicketTypeSchema,
  price: z.number(), // lo fija el backend (GAP-06): la UI nunca lo calcula ni lo envía
  status: TicketStatusSchema,
  purchaseDate: z.string(),
  userEmail: z.string(),
  eventCode: z.string(),
  eventName: z.string(),
})
export type TicketResponse = z.infer<typeof TicketResponseSchema>

// Compra (FR-FE-TKT-001): solo el tipo se elige. El email sale de la sesión, nunca de un campo (§24.3)
export const CompraFormSchema = z.object({ type: TicketTypeSchema })
export type CompraForm = z.infer<typeof CompraFormSchema>

// Validación de ingreso (FR-FE-STF-001)
export const BuscarTicketFormSchema = z.object({
  ticketCode: z.string().trim().min(1, 'Ingresa el código del ticket'),
})
export type BuscarTicketForm = z.infer<typeof BuscarTicketFormSchema>
