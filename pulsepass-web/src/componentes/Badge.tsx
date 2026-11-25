import { clsx } from 'clsx'
import type { ReactNode } from 'react'
import type { EventStatus } from '../funcionalidades/eventos/tipos'
import type { TicketStatus } from '../funcionalidades/tickets/tipos'

// PRD Frontend §9.1 "Badges de estado": mapa tipado con clases completas (FE-008)
export type TonoBadge = EventStatus | TicketStatus | 'ACTIVA' | 'INACTIVA' | 'ROL'

const TONOS: Record<TonoBadge, string> = {
  DRAFT: 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200',
  PUBLISHED: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-200',
  SOLD_OUT: 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-200',
  CANCELLED: 'bg-red-100 text-red-800 dark:bg-red-900/60 dark:text-red-200',
  FINISHED: 'bg-slate-300 text-slate-800 dark:bg-slate-600 dark:text-slate-100',
  RESERVED: 'bg-sky-100 text-sky-800 dark:bg-sky-900/60 dark:text-sky-200',
  PAID: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-200',
  USED: 'bg-slate-300 text-slate-800 dark:bg-slate-600 dark:text-slate-100',
  ACTIVA: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-200',
  INACTIVA: 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200',
  ROL: 'bg-marca-50 text-marca-700 dark:bg-slate-800 dark:text-marca-500',
}

interface PropsBadge {
  tono: TonoBadge
  children?: ReactNode
  className?: string
}

// Sin children muestra el valor del estado tal cual (PUBLISHED, PAID...), como en los diseños
export function Badge({ tono, children, className }: PropsBadge) {
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold tracking-wide',
        TONOS[tono],
        className,
      )}
    >
      {children ?? tono}
    </span>
  )
}
