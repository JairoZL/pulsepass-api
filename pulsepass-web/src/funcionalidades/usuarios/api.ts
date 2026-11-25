import { z } from 'zod'
import { solicitar } from '../../lib/http'
import { UserResponseSchema, type RegistroForm } from './tipos'

// 12 / A3: POST /api/users (registro público con contraseña). Responde 201 con UserResponse
export function registrarUsuario(datos: RegistroForm) {
  return solicitar('/users', UserResponseSchema, { metodo: 'POST', cuerpo: datos })
}

// 13: GET /api/users/by-email?email= (solo el propio usuario o ADMIN)
export function obtenerUsuarioPorEmail(email: string) {
  return solicitar(`/users/by-email?${new URLSearchParams({ email })}`, UserResponseSchema)
}

// 14: GET /api/users/by-username?username= (solo el propio usuario o ADMIN)
export function obtenerUsuarioPorUsername(username: string) {
  return solicitar(`/users/by-username?${new URLSearchParams({ username })}`, UserResponseSchema)
}

export const UsuariosSchema = z.array(UserResponseSchema)
