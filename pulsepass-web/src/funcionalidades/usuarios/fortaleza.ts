// Indicador visual de fortaleza de la contraseña (Registro.png). Es solo una ayuda de interfaz:
// la regla real (mínimo 12 caracteres, máximo 72 bytes) la valida zod y luego el backend.
export interface Fortaleza {
  puntos: number // 0..8, uno por segmento de la barra
  etiqueta: 'baja' | 'media' | 'alta'
}

export function calcularFortaleza(clave: string): Fortaleza {
  let puntos = 0
  if (clave.length >= 12) puntos += 2
  if (clave.length >= 16) puntos += 1
  if (clave.length >= 20) puntos += 1
  if (/[a-z]/.test(clave)) puntos += 1
  if (/[A-Z]/.test(clave)) puntos += 1
  if (/\d/.test(clave)) puntos += 1
  if (/[^A-Za-z0-9]/.test(clave)) puntos += 1
  const etiqueta = puntos >= 7 ? 'alta' : puntos >= 4 ? 'media' : 'baja'
  return { puntos, etiqueta }
}
