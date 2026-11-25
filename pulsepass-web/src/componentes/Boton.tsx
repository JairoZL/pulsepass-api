import { clsx } from 'clsx'
import type { ComponentProps } from 'react'

// PRD Frontend §9.1 y DES-09. FE-008: las variantes son un mapa con clases COMPLETAS.
// Nunca `bg-${color}-600`: Tailwind solo genera clases que encuentra escritas tal cual en el código.
export type VarianteBoton = 'primario' | 'secundario' | 'peligro' | 'fantasma' | 'claro'

const VARIANTES: Record<VarianteBoton, string> = {
  primario: 'bg-marca-600 text-white hover:bg-marca-700 shadow-sm',
  secundario:
    'border border-slate-300 bg-white text-slate-900 hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100 dark:hover:bg-slate-800',
  peligro: 'bg-red-600 text-white hover:bg-red-700 shadow-sm',
  fantasma: 'text-marca-600 hover:bg-marca-50 dark:text-marca-500 dark:hover:bg-slate-800',
  // Sobre fondos de color (hero): blanco con texto de marca
  claro: 'bg-white text-marca-700 hover:bg-marca-50 shadow-sm',
}

const BASE =
  'inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-5 py-2.5 text-sm font-semibold transition-colors ' +
  'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 ' +
  'disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500 disabled:shadow-none disabled:hover:bg-slate-200'

// Para aplicar el mismo estilo a un <Link> (por ejemplo los botones del hero)
export function clasesBoton(variante: VarianteBoton = 'primario', className?: string): string {
  return clsx(BASE, VARIANTES[variante], className)
}

interface PropsBoton extends ComponentProps<'button'> {
  variante?: VarianteBoton
  cargando?: boolean
}

// cargando = deshabilitado + texto "Cargando…": evita el doble envío (PRD §9.8)
export function Boton({
  variante = 'primario',
  cargando = false,
  disabled,
  type = 'button',
  className,
  children,
  ...props
}: PropsBoton) {
  return (
    <button
      type={type}
      disabled={disabled || cargando}
      aria-busy={cargando || undefined}
      className={clasesBoton(variante, className)}
      {...props}
    >
      {cargando ? 'Cargando…' : children}
    </button>
  )
}
