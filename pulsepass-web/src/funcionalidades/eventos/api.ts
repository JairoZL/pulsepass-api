import { z } from 'zod'
import { solicitar } from '../../lib/http'
import { EventResponseSchema, EventSummaryResponseSchema, type CrearEventoForm } from './tipos'

const enc = encodeURIComponent

// Operaciones 3..8 de la tabla PRD Frontend §10.3
export const obtenerEventosPublicados = () =>
  solicitar('/events/published', z.array(EventSummaryResponseSchema))

export const obtenerEvento = (eventCode: string) => solicitar(`/events/${enc(eventCode)}`, EventResponseSchema)

export const obtenerEventosPorArtista = (stageName: string) =>
  solicitar(`/events/by-artist?${new URLSearchParams({ stageName })}`, z.array(EventSummaryResponseSchema))

export const crearEvento = (datos: CrearEventoForm) =>
  solicitar('/events', EventResponseSchema, { metodo: 'POST', cuerpo: datos })

export const publicarEvento = (eventCode: string) =>
  solicitar(`/events/${enc(eventCode)}/publish`, EventResponseSchema, { metodo: 'PATCH' })

export const asociarArtista = (eventCode: string, artistId: number) =>
  solicitar(`/events/${enc(eventCode)}/artists/${artistId}`, EventResponseSchema, { metodo: 'POST' })
