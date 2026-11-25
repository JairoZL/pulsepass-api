import { z } from 'zod'

// PRD Frontend §12.2: el token vive en memoria y en sessionStorage (se borra al cerrar la pestaña).
// Nunca en localStorage, cookies ni en la URL (FE-013, §24.6).
const CLAVE = 'pulsepass.sesion'

// FE-002: lo que se lee de sessionStorage también es dato externo y se valida con zod
const SesionGuardadaSchema = z.object({
  token: z.string().min(1),
  expiraEn: z.number(), // milisegundos desde 1970 (Date.now())
})
export type SesionGuardada = z.infer<typeof SesionGuardadaSchema>

let tokenEnMemoria: string | null = null

export function obtenerToken(): string | null {
  return tokenEnMemoria
}

export function guardarSesion(sesion: SesionGuardada): void {
  tokenEnMemoria = sesion.token
  sessionStorage.setItem(CLAVE, JSON.stringify(sesion))
}

export function borrarSesion(): void {
  tokenEnMemoria = null
  sessionStorage.removeItem(CLAVE)
}

// Al recargar la página: devuelve la sesión guardada solo si es válida y no expiró (TEST-FE-AUTH-011)
export function leerSesionGuardada(ahora: number = Date.now()): SesionGuardada | null {
  try {
    const crudo = sessionStorage.getItem(CLAVE)
    if (!crudo) return null
    const resultado = SesionGuardadaSchema.safeParse(JSON.parse(crudo))
    if (!resultado.success || resultado.data.expiraEn <= ahora) {
      borrarSesion()
      return null
    }
    tokenEnMemoria = resultado.data.token
    return resultado.data
  } catch {
    // JSON corrupto: se descarta
    borrarSesion()
    return null
  }
}
