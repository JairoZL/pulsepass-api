import { z } from 'zod'

export const RolSchema = z.enum(['USER', 'STAFF', 'ADMIN'])
export type Rol = z.infer<typeof RolSchema>

// POST /api/auth/login
export const AuthResponseSchema = z.object({
  accessToken: z.string().min(1),
  tokenType: z.literal('Bearer'),
  expiresIn: z.number().int().positive(), // segundos (900)
})
export type AuthResponse = z.infer<typeof AuthResponseSchema>

// GET /api/auth/me (real): id, username, roles. No trae email (R-FE-002, GAP-03):
// el email se obtiene con GET /api/users/by-username del propio usuario
export const AuthenticatedUserResponseSchema = z.object({
  id: z.number(),
  username: z.string(),
  roles: z.array(RolSchema),
})
export type AuthenticatedUserResponse = z.infer<typeof AuthenticatedUserResponseSchema>

// Lo que guarda la sesión del cliente
export interface UsuarioAutenticado {
  id: number
  username: string
  email: string
  roles: Rol[]
}

// FE-006: unión discriminada en vez de varios booleanos (isLoading, isLogged...)
export type Sesion =
  | { estado: 'cargando' }
  | { estado: 'anonimo'; motivo?: 'expirada' }
  | { estado: 'autenticado'; token: string; expiraEn: number; usuario: UsuarioAutenticado }

// Formulario de login (PRD §11)
export const LoginFormSchema = z.object({
  identifier: z.string().trim().min(1, 'Ingresa tu usuario o correo'),
  password: z.string().min(1, 'Ingresa tu contraseña'),
})
export type LoginForm = z.infer<typeof LoginFormSchema>
