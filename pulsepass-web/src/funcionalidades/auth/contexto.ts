import { createContext, use } from 'react'
import type { LoginForm, Rol, Sesion } from './tipos'

// PRD Frontend §12.1: lo que expone la sesión a toda la app. El proveedor está en AuthContext.tsx
export interface ValorAuth {
  sesion: Sesion
  iniciarSesion: (datos: LoginForm) => Promise<void>
  cerrarSesion: (motivo?: 'expirada') => void
  tieneRol: (...roles: Rol[]) => boolean
}

export const AuthContext = createContext<ValorAuth | null>(null)

export function useAuth(): ValorAuth {
  const valor = use(AuthContext)
  if (!valor) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return valor
}
