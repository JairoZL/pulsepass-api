import { useMutation, useQuery } from '@tanstack/react-query'
import { obtenerUsuarioPorEmail, obtenerUsuarioPorUsername, registrarUsuario } from './api'
import type { RegistroForm } from './tipos'

export function useRegistrarUsuario() {
  return useMutation({ mutationFn: (datos: RegistroForm) => registrarUsuario(datos) })
}

export function useUsuarioPorEmail(email: string | undefined) {
  return useQuery({
    queryKey: ['usuario', email],
    queryFn: () => obtenerUsuarioPorEmail(email ?? ''),
    enabled: Boolean(email),
    staleTime: 60_000,
  })
}

export function useUsuarioPorUsername(username: string | undefined) {
  return useQuery({
    queryKey: ['usuario', 'username', username],
    queryFn: () => obtenerUsuarioPorUsername(username ?? ''),
    enabled: Boolean(username),
    staleTime: 60_000,
  })
}
