import { clsx } from 'clsx'
import { useId, type ComponentProps, type ReactNode } from 'react'
import { clasesControl, describedBy } from './estilos'
import { IconoAlerta } from './Iconos'

// PRD Frontend §9.1 y FE-010: cada campo tiene <label> asociado; el error se anuncia con
// aria-invalid + aria-describedby. React 19 pasa `ref` como prop normal, así register() funciona.

interface PropsEnvoltura {
  id: string
  etiqueta: string
  error?: string
  ayuda?: string
  children: ReactNode
  className?: string
}

// Etiqueta + control + mensaje de error o ayuda. La usan Campo, Selector y AreaTexto
export function EnvolturaCampo({ id, etiqueta, error, ayuda, children, className }: PropsEnvoltura) {
  return (
    <div className={clsx('flex flex-col gap-1.5', className)}>
      <label htmlFor={id} className="text-sm font-medium text-slate-700 dark:text-slate-300">
        {etiqueta}
      </label>
      {children}
      {error ? (
        <p id={`${id}-error`} className="flex items-center gap-1.5 text-sm text-red-600 dark:text-red-400">
          <IconoAlerta tamano={14} />
          {error}
        </p>
      ) : ayuda ? (
        <p id={`${id}-ayuda`} className="text-sm text-slate-500 dark:text-slate-400">
          {ayuda}
        </p>
      ) : null}
    </div>
  )
}

interface PropsCampo extends ComponentProps<'input'> {
  etiqueta: string
  error?: string
  ayuda?: string
  envolturaClassName?: string
  // Contenido a la derecha dentro del campo (por ejemplo, mostrar/ocultar contraseña)
  accesorio?: ReactNode
  // Ícono decorativo a la izquierda (por ejemplo, la lupa del buscador)
  iconoInicio?: ReactNode
}

export function Campo({
  etiqueta,
  error,
  ayuda,
  id,
  envolturaClassName,
  accesorio,
  iconoInicio,
  className,
  ...props
}: PropsCampo) {
  const idGenerado = useId()
  const idCampo = id ?? idGenerado
  return (
    <EnvolturaCampo id={idCampo} etiqueta={etiqueta} error={error} ayuda={ayuda} className={envolturaClassName}>
      <div className="relative">
        {iconoInicio ? (
          <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
            {iconoInicio}
          </div>
        ) : null}
        <input
          id={idCampo}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(idCampo, error, ayuda)}
          className={clsx(clasesControl(Boolean(error)), accesorio ? 'pr-12' : undefined, iconoInicio ? 'pl-10' : undefined, className)}
          {...props}
        />
        {accesorio ? <div className="absolute inset-y-0 right-0 flex items-center pr-2">{accesorio}</div> : null}
      </div>
    </EnvolturaCampo>
  )
}
