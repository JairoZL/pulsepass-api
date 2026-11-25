import { ApiError, MENSAJES_GENERICOS } from './http'

// PRD Frontend §13: qué texto ve la persona según el error. Siempre como texto, nunca HTML.
// 400 y 409 muestran el mensaje del backend (regla de negocio); 500 nunca muestra detalles internos.
export function mensajeDeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 0) return MENSAJES_GENERICOS.red
    if (error.status >= 500) return MENSAJES_GENERICOS.inesperado
    return error.message
  }
  return MENSAJES_GENERICOS.inesperado
}

export function esApiError(error: unknown, ...estados: number[]): error is ApiError {
  return error instanceof ApiError && (estados.length === 0 || estados.includes(error.status))
}
