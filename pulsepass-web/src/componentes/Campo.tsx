import { clsx } from 'clsx'
import { useId, type ComponentProps, type ReactNode } from 'react'
import { IconoAlerta } from './Iconos'

// PRD Frontend §9.1 y FE-010: cada campo tiene <label> asociado; el error se anuncia con
// aria-invalid + aria-describedby. React 19 pasa `ref` como prop normal, así register() funciona.

export const CLASES_CONTROL =
  'block w-full rounded-lg border bg-white px-3.5 py-2.5 text-base text-slate-900 placeholder:text-slate-400 ' +
  'focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-marca-600 ' +
  'dark:bg-slate-900 dark:text-slate-100'

export function clasesControl(conError: boolean): string {
  return clsx(CLASES_CONTROL, conError ? 'border-red-500' : 'border-slate-300 dark:border-slate-600')
}

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

export function describedBy(id: string, error?: string, ayuda?: string): string | undefined {
  if (error) return `${id}-error`
  if (ayuda) return `${id}-ayuda`
  return undefined
}

interface PropsCampo extends ComponentProps<'input'> {
  etiqueta: string
  error?: string
  ayuda?: string
  envolturaClassName?: string
  // Contenido a la derecha dentro del campo (por ejemplo, mostrar/ocultar contraseña)
  accesorio?: ReactNode
}

export function Campo({ etiqueta, error, ayuda, id, envolturaClassName, accesorio, className, ...props }: PropsCampo) {
  const idGenerado = useId()
  const idCampo = id ?? idGenerado
  return (
    <EnvolturaCampo id={idCampo} etiqueta={etiqueta} error={error} ayuda={ayuda} className={envolturaClassName}>
      <div className="relative">
        <input
          id={idCampo}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(idCampo, error, ayuda)}
          className={clsx(clasesControl(Boolean(error)), accesorio ? 'pr-12' : undefined, className)}
          {...props}
        />
        {accesorio ? <div className="absolute inset-y-0 right-0 flex items-center pr-2">{accesorio}</div> : null}
      </div>
    </EnvolturaCampo>
  )
}
