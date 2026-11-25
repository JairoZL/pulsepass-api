import { z } from 'zod'

// UserResponse real del backend: solo id, username, email, active (sin nombres ni fecha de nacimiento).
// Brecha documentada en el README: la página "Mi cuenta" muestra lo que el backend expone.
export const UserResponseSchema = z.object({
  id: z.number(),
  username: z.string(),
  email: z.string(),
  active: z.boolean(),
})
export type UserResponse = z.infer<typeof UserResponseSchema>

// BCrypt solo usa 72 bytes (PRD Seguridad §9.3). Una "ñ" ocupa 2 bytes: por eso se cuenta en bytes
const bytesUtf8 = (texto: string) => new TextEncoder().encode(texto).length

function hoyIso(): string {
  const hoy = new Date()
  const mes = String(hoy.getMonth() + 1).padStart(2, '0')
  const dia = String(hoy.getDate()).padStart(2, '0')
  return `${hoy.getFullYear()}-${mes}-${dia}`
}

// Formulario de registro (PRD §11, FR-FE-AUTH-001). La edad mínima NO se valida aquí: es regla del backend
export const RegistroFormSchema = z.object({
  username: z.string().trim().min(1, 'Ingresa un nombre de usuario').max(50, 'Máximo 50 caracteres'),
  email: z.email('Ingresa un correo válido').max(180, 'Máximo 180 caracteres'),
  firstName: z.string().trim().min(1, 'Ingresa tus nombres').max(80, 'Máximo 80 caracteres'),
  lastName: z.string().trim().min(1, 'Ingresa tus apellidos').max(80, 'Máximo 80 caracteres'),
  birthDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Ingresa tu fecha de nacimiento')
    .refine((fecha) => fecha <= hoyIso(), 'La fecha no puede ser futura'),
  password: z
    .string()
    .min(12, 'La contraseña debe tener al menos 12 caracteres')
    .refine((clave) => bytesUtf8(clave) <= 72, 'La contraseña es demasiado larga (máximo 72 bytes)'),
})
export type RegistroForm = z.infer<typeof RegistroFormSchema>
