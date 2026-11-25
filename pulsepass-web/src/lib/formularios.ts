import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'

// PRD Frontend §11.2: un 400 con details { campo: mensaje } se reparte en los campos del formulario.
// Las claves que no son campos (o que el backend nombra distinto) se devuelven para un aviso general.
export function aplicarErroresServidor<T extends FieldValues>(
  details: Record<string, string>,
  setError: UseFormSetError<T>,
  campos: readonly Path<T>[],
  equivalencias: Partial<Record<string, Path<T>>> = {},
): string | null {
  const sinCampo: string[] = []
  for (const [clave, mensaje] of Object.entries(details)) {
    const campo = equivalencias[clave] ?? campos.find((nombre) => nombre === clave)
    if (campo) setError(campo, { type: 'server', message: mensaje })
    else sinCampo.push(mensaje)
  }
  return sinCampo.length > 0 ? sinCampo.join(' ') : null
}
