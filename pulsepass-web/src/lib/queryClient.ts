import { QueryClient } from '@tanstack/react-query'
import { ApiError } from './http'

// PRD Frontend §14.3: no se reintentan 4xx (401/403/404/409 no cambian al repetir);
// sí 1 vez los errores de red y 5xx. Las mutaciones nunca se reintentan (evita compras duplicadas).
export function debeReintentar(fallos: number, error: unknown): boolean {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) return false
  return fallos < 1
}

export function crearQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: debeReintentar, refetchOnWindowFocus: false },
      mutations: { retry: false },
    },
  })
}
