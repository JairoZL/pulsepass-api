import { useQuery } from '@tanstack/react-query'
import { obtenerSede, obtenerSedesActivas } from './api'

const CINCO_MINUTOS = 5 * 60_000

export function useSedesActivas() {
  return useQuery({ queryKey: ['sedes', 'activas'], queryFn: obtenerSedesActivas, staleTime: CINCO_MINUTOS })
}

export function useSede(code: string | undefined) {
  return useQuery({
    queryKey: ['sede', code],
    queryFn: () => obtenerSede(code ?? ''),
    enabled: Boolean(code),
    staleTime: CINCO_MINUTOS,
  })
}
