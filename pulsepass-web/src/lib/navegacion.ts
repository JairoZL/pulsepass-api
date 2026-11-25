import { z } from 'zod'
import { RUTAS } from './rutas'

// location.state también es dato externo (FE-002): se valida antes de usarlo.
// Solo se aceptan rutas internas ("/algo"), nunca "//otro-sitio.com" ni URLs completas (redirección abierta).
const EstadoNavegacionSchema = z.object({
  desde: z
    .string()
    .refine((ruta) => ruta.startsWith('/') && !ruta.startsWith('//'))
    .optional(),
  motivo: z.literal('expirada').optional(),
  registrado: z.boolean().optional(),
})

export type EstadoNavegacion = z.infer<typeof EstadoNavegacionSchema>

export function leerEstadoNavegacion(state: unknown): EstadoNavegacion {
  const resultado = EstadoNavegacionSchema.safeParse(state)
  return resultado.success ? resultado.data : {}
}

// A dónde volver después de iniciar sesión (FR-FE-AUTH-002)
export function rutaDeRetorno(state: unknown): string {
  return leerEstadoNavegacion(state).desde ?? RUTAS.inicio
}
