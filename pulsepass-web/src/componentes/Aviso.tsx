import { clsx } from 'clsx'
import type { ReactNode } from 'react'
import { IconoAlerta, IconoCerrar, IconoCheck, IconoRayo } from './Iconos'

// PRD Frontend §9.20 y DES-10: mensajes de éxito, error, información y advertencia.
// error -> role="alert" (se anuncia de inmediato); los demás -> role="status" (aria-live polite)
export type TonoAviso = 'exito' | 'error' | 'info' | 'advertencia'

const TONOS: Record<TonoAviso, string> = {
  exito: 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-200',
  error: 'border-red-200 bg-red-50 text-red-800 dark:border-red-800 dark:bg-red-950 dark:text-red-200',
  info: 'border-marca-100 bg-marca-50 text-marca-700 dark:border-slate-700 dark:bg-slate-900 dark:text-marca-500',
  advertencia: 'border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-200',
}

const ICONOS: Record<TonoAviso, ReactNode> = {
  exito: <IconoCheck tamano={18} />,
  error: <IconoAlerta tamano={18} />,
  info: <IconoRayo tamano={18} />,
  advertencia: <IconoAlerta tamano={18} />,
}

interface PropsAviso {
  tono: TonoAviso
  children: ReactNode
  onCerrar?: () => void
  className?: string
}

export function Aviso({ tono, children, onCerrar, className }: PropsAviso) {
  return (
    <div
      role={tono === 'error' ? 'alert' : 'status'}
      className={clsx('flex items-start gap-3 rounded-lg border px-4 py-3 text-sm', TONOS[tono], className)}
    >
      <span className="mt-0.5 shrink-0">{ICONOS[tono]}</span>
      {/* Texto plano: React lo escapa. Nunca dangerouslySetInnerHTML con mensajes de la API (§24.10) */}
      <div className="flex-1">{children}</div>
      {onCerrar ? (
        <button
          type="button"
          onClick={onCerrar}
          aria-label="Cerrar aviso"
          className="shrink-0 rounded p-0.5 opacity-70 hover:opacity-100 focus-visible:outline-2 focus-visible:outline-current"
        >
          <IconoCerrar tamano={16} />
        </button>
      ) : null}
    </div>
  )
}
