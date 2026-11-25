import { clsx } from 'clsx'
import { useId, type ComponentProps } from 'react'
import { clasesControl, describedBy, EnvolturaCampo } from './Campo'

interface PropsAreaTexto extends ComponentProps<'textarea'> {
  etiqueta: string
  error?: string
  ayuda?: string
}

export function AreaTexto({ etiqueta, error, ayuda, id, className, ...props }: PropsAreaTexto) {
  const idGenerado = useId()
  const idCampo = id ?? idGenerado
  return (
    <EnvolturaCampo id={idCampo} etiqueta={etiqueta} error={error} ayuda={ayuda}>
      <textarea
        id={idCampo}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(idCampo, error, ayuda)}
        className={clsx(clasesControl(Boolean(error)), 'min-h-28', className)}
        {...props}
      />
    </EnvolturaCampo>
  )
}
