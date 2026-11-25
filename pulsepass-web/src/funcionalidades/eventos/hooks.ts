import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  asociarArtista,
  crearEvento,
  obtenerEvento,
  obtenerEventosPorArtista,
  obtenerEventosPublicados,
  publicarEvento,
} from './api'
import type { CrearEventoForm } from './tipos'

// PRD Frontend §14.1: claves de caché en un solo lugar (las usan también otras funcionalidades al invalidar)
export const clavesEventos = {
  publicados: ['eventos', 'publicados'] as const,
  evento: (eventCode: string) => ['evento', eventCode] as const,
  porArtista: (stageName: string) => ['eventos', 'artista', stageName] as const,
}

export function useEventosPublicados() {
  return useQuery({
    queryKey: clavesEventos.publicados,
    queryFn: obtenerEventosPublicados,
    staleTime: 60_000,
  })
}

export function useEvento(eventCode: string | undefined) {
  return useQuery({
    queryKey: clavesEventos.evento(eventCode ?? ''),
    queryFn: () => obtenerEvento(eventCode ?? ''),
    enabled: Boolean(eventCode),
    staleTime: 30_000,
  })
}

export function useEventosPorArtista(stageName: string | undefined) {
  return useQuery({
    queryKey: clavesEventos.porArtista(stageName ?? ''),
    queryFn: () => obtenerEventosPorArtista(stageName ?? ''),
    enabled: Boolean(stageName),
    staleTime: 60_000,
  })
}

// §14.2: crear evento siembra la caché con el resultado (la página de gestión no vuelve a pedirlo)
export function useCrearEvento() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (datos: CrearEventoForm) => crearEvento(datos),
    onSuccess: (evento) => {
      queryClient.setQueryData(clavesEventos.evento(evento.eventCode), evento)
    },
  })
}

export function usePublicarEvento() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (eventCode: string) => publicarEvento(eventCode),
    onSuccess: (evento) => {
      queryClient.setQueryData(clavesEventos.evento(evento.eventCode), evento)
      void queryClient.invalidateQueries({ queryKey: clavesEventos.publicados })
    },
  })
}

export function useAsociarArtista() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ eventCode, artistId }: { eventCode: string; artistId: number; stageName: string }) =>
      asociarArtista(eventCode, artistId),
    onSuccess: (evento, { stageName }) => {
      queryClient.setQueryData(clavesEventos.evento(evento.eventCode), evento)
      void queryClient.invalidateQueries({ queryKey: clavesEventos.porArtista(stageName) })
    },
  })
}
