import { z } from 'zod'
import { obtenerToken } from './sesionAlmacen'

// PRD Frontend §5 y §13: ÚNICO lugar que hace fetch. Agrega el Bearer, valida la respuesta con zod
// y convierte cualquier error en ApiError. Las páginas nunca llaman fetch directamente.

// Contrato de errores del backend (Fase 3): { timestamp, status, error, message, details }
export const ErrorResponseSchema = z.object({
  timestamp: z.string().optional(),
  status: z.number(),
  error: z.string().optional(),
  message: z.string(),
  details: z.record(z.string(), z.string()).optional(),
})

// status 0 = no hubo respuesta (sin red o API caída)
export class ApiError extends Error {
  readonly status: number
  readonly details: Record<string, string>

  constructor(status: number, message: string, details: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

export const MENSAJES_GENERICOS = {
  red: 'No pudimos conectar con el servidor.',
  noAutorizado: 'Tu sesión expiró. Inicia sesión de nuevo.',
  prohibido: 'No tienes permisos para realizar esta operación.',
  noEncontrado: 'No encontramos lo que buscas.',
  inesperado: 'Ocurrió un error inesperado. Inténtalo más tarde.',
  respuestaInvalida: 'El servidor respondió con datos inesperados.',
} as const

function mensajeGenerico(status: number): string {
  if (status === 401) return MENSAJES_GENERICOS.noAutorizado
  if (status === 403) return MENSAJES_GENERICOS.prohibido
  if (status === 404) return MENSAJES_GENERICOS.noEncontrado
  return MENSAJES_GENERICOS.inesperado
}

// AuthContext se registra aquí para enterarse de un 401 (sesión vencida o token inválido)
let alNoAutorizado: () => void = () => {}
export function configurarAlNoAutorizado(funcion: () => void): void {
  alNoAutorizado = funcion
}

type Metodo = 'GET' | 'POST' | 'PATCH'

interface OpcionesSolicitud {
  metodo?: Metodo
  cuerpo?: unknown
  // false solo en el login: ahí un 401 significa "credenciales inválidas", no "sesión vencida"
  cerrarSesionSi401?: boolean
}

const BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'

// URL absoluta: el navegador resuelve rutas relativas solo, pero las pruebas (Node) no
function construirUrl(ruta: string): string {
  return new URL(`${BASE}${ruta}`, window.location.origin).toString()
}

export async function solicitar<T>(
  ruta: string,
  esquema: z.ZodType<T>,
  { metodo = 'GET', cuerpo, cerrarSesionSi401 = true }: OpcionesSolicitud = {},
): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (cuerpo !== undefined) headers['Content-Type'] = 'application/json'

  // FE-013: el token SOLO viaja en este header (nunca en la URL)
  const token = obtenerToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let respuesta: Response
  try {
    respuesta = await fetch(construirUrl(ruta), {
      method: metodo,
      headers,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    })
  } catch {
    throw new ApiError(0, MENSAJES_GENERICOS.red)
  }

  if (!respuesta.ok) {
    if (respuesta.status === 401 && token && cerrarSesionSi401) {
      alNoAutorizado()
    }
    throw await convertirEnApiError(respuesta)
  }

  const datos: unknown = await respuesta.json().catch(() => undefined)
  const resultado = esquema.safeParse(datos)
  if (!resultado.success) {
    throw new ApiError(respuesta.status, MENSAJES_GENERICOS.respuestaInvalida)
  }
  return resultado.data
}

// Si el cuerpo cumple ErrorResponse se usa su mensaje; si no, un mensaje genérico según el código
async function convertirEnApiError(respuesta: Response): Promise<ApiError> {
  const cuerpo: unknown = await respuesta.json().catch(() => undefined)
  const resultado = ErrorResponseSchema.safeParse(cuerpo)
  if (resultado.success) {
    return new ApiError(respuesta.status, resultado.data.message, resultado.data.details ?? {})
  }
  return new ApiError(respuesta.status, mensajeGenerico(respuesta.status))
}
