import { z } from 'zod'
import { solicitar } from '../../lib/http'
import { ArtistResponseSchema } from './tipos'

// Operaciones 9..11 de la tabla PRD Frontend §10.3
export const obtenerArtistasActivos = () => solicitar('/artists/active', z.array(ArtistResponseSchema))

export const obtenerArtista = (id: number) => solicitar(`/artists/${id}`, ArtistResponseSchema)

export const obtenerArtistaPorNombre = (stageName: string) =>
  solicitar(`/artists/by-stage-name?${new URLSearchParams({ stageName })}`, ArtistResponseSchema)
