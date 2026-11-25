import { useQuery } from '@tanstack/react-query'
import { obtenerArtista, obtenerArtistaPorNombre, obtenerArtistasActivos } from './api'

const CINCO_MINUTOS = 5 * 60_000

export function useArtistasActivos() {
  return useQuery({ queryKey: ['artistas', 'activos'], queryFn: obtenerArtistasActivos, staleTime: CINCO_MINUTOS })
}

// id null = parámetro inválido: no se llama a la API (TEST-FE-ART-003)
export function useArtista(id: number | null) {
  return useQuery({
    queryKey: ['artista', id],
    queryFn: () => obtenerArtista(id ?? 0),
    enabled: id !== null,
    staleTime: CINCO_MINUTOS,
  })
}

export function useArtistaPorNombre(stageName: string | undefined) {
  return useQuery({
    queryKey: ['artista', 'nombre', stageName],
    queryFn: () => obtenerArtistaPorNombre(stageName ?? ''),
    enabled: Boolean(stageName),
    staleTime: CINCO_MINUTOS,
  })
}
