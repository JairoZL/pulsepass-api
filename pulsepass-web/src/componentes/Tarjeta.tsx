import { clsx } from 'clsx'
import type { ComponentProps } from 'react'

// Superficie: blanco + sombra suave en claro, slate-900 en oscuro (PRD Frontend §9.1)
export function Tarjeta({ className, children, ...props }: ComponentProps<'div'>) {
  return (
    <div
      className={clsx(
        'rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900',
        className,
      )}
      {...props}
    >
      {children}
    </div>
  )
}
