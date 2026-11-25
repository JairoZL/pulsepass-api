import { z } from 'zod'
import { solicitar } from '../../lib/http'
import { VenueResponseSchema } from './tipos'

// Operaciones 1 y 2 de la tabla PRD Frontend §10.3
export const obtenerSedesActivas = () => solicitar('/venues/active', z.array(VenueResponseSchema))

export const obtenerSede = (code: string) => solicitar(`/venues/${encodeURIComponent(code)}`, VenueResponseSchema)
