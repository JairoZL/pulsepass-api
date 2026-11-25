import { z } from 'zod'
import { ArtistResponseSchema } from '../artistas/tipos'

// PRD Frontend §10. Contrato confirmado contra los DTO reales del backend (EventResponse,
// EventSummaryResponse, CreateEventRequest). FE-002: los tipos se DERIVAN del esquema con z.infer.

export const EventCategorySchema = z.enum(['MUSIC', 'SPORTS', 'TECHNOLOGY', 'EDUCATION', 'CULTURE', 'ENTERTAINMENT'])
export type EventCategory = z.infer<typeof EventCategorySchema>

export const EventStatusSchema = z.enum(['DRAFT', 'PUBLISHED', 'SOLD_OUT', 'CANCELLED', 'FINISHED'])
export type EventStatus = z.infer<typeof EventStatusSchema>

// GET /api/events/{eventCode}
export const EventResponseSchema = z.object({
  id: z.number(),
  eventCode: z.string(),
  name: z.string(),
  description: z.string(),
  category: EventCategorySchema,
  status: EventStatusSchema,
  eventDate: z.string(), // LocalDateTime sin zona: "2026-12-12T20:00:00"
  minimumAge: z.number(),
  venueCode: z.string(),
  venueName: z.string(),
  artists: z.array(ArtistResponseSchema),
})
export type EventResponse = z.infer<typeof EventResponseSchema>

// GET /api/events/published y /by-artist. El backend envía venueName (no ciudad)
export const EventSummaryResponseSchema = z.object({
  eventCode: z.string(),
  name: z.string(),
  category: EventCategorySchema,
  status: EventStatusSchema,
  eventDate: z.string(),
  venueName: z.string(),
})
export type EventSummaryResponse = z.infer<typeof EventSummaryResponseSchema>

// Formulario de creación (PRD §11, FR-FE-ADM-001). Límites = columnas de events (V1)
export const CrearEventoFormSchema = z.object({
  eventCode: z
    .string()
    .trim()
    .min(1, 'Ingresa el código del evento')
    .max(30, 'Máximo 30 caracteres'),
  name: z.string().trim().min(1, 'Ingresa el nombre').max(80, 'Máximo 80 caracteres'),
  description: z.string().trim().min(1, 'Ingresa la descripción').max(500, 'Máximo 500 caracteres'),
  category: EventCategorySchema,
  eventDate: z.string().min(1, 'Elige la fecha y hora'),
  minimumAge: z
    .number({ error: 'Ingresa un número' })
    .int('Debe ser un número entero')
    .min(0, 'No puede ser negativa'),
  venueCode: z.string().min(1, 'Elige la sede'),
})
export type CrearEventoForm = z.infer<typeof CrearEventoFormSchema>
