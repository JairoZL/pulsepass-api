import { clsx } from 'clsx'
import { useId, type ComponentProps } from 'react'
import { EnvolturaCampo } from './Campo'
import { clasesControl, describedBy } from './estilos'

interface PropsSelector extends ComponentProps<'select'> {
  etiqueta: string
  error?: string
  ayuda?: string
  envolturaClassName?: string
}

// Igual que Campo, pero para <select> (PRD Frontend §9.1)
export function Selector({ etiqueta, error, ayuda, id, envolturaClassName, className, children, ...props }: PropsSelector) {
  const idGenerado = useId()
  const idCampo = id ?? idGenerado
  return (
    <EnvolturaCampo id={idCampo} etiqueta={etiqueta} error={error} ayuda={ayuda} className={envolturaClassName}>
      <select
        id={idCampo}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(idCampo, error, ayuda)}
        className={clsx(clasesControl(Boolean(error)), className)}
        {...props}
      >
        {children}
      </select>
    </EnvolturaCampo>
  )
}
