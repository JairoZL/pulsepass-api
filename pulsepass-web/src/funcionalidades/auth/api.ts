import { solicitar } from '../../lib/http'
import { AuthenticatedUserResponseSchema, AuthResponseSchema, type LoginForm } from './tipos'

// A1: POST /api/auth/login. Un 401 aquí es "credenciales inválidas", no "sesión vencida"
export function iniciarSesionApi(datos: LoginForm) {
  return solicitar('/auth/login', AuthResponseSchema, {
    metodo: 'POST',
    cuerpo: datos,
    cerrarSesionSi401: false,
  })
}

// A2: GET /api/auth/me (requiere Bearer)
export function obtenerMe() {
  return solicitar('/auth/me', AuthenticatedUserResponseSchema)
}
