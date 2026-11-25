import { clsx } from 'clsx'

// Clases compartidas por varios componentes. Están aparte (no en un .tsx con componentes) para que
// Fast Refresh de Vite funcione: un archivo de componentes solo debe exportar componentes.

// PRD Frontend §9.1 y DES-09. FE-008: las variantes son un mapa con clases COMPLETAS.
// Nunca `bg-${color}-600`: Tailwind solo genera clases que encuentra escritas tal cual en el código.
export type VarianteBoton = 'primario' | 'secundario' | 'peligro' | 'fantasma' | 'claro' | 'contornoClaro'

const VARIANTES: Record<VarianteBoton, string> = {
  primario: 'bg-marca-600 text-white hover:bg-marca-700 shadow-sm',
  secundario:
    'border border-slate-300 bg-white text-slate-900 hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100 dark:hover:bg-slate-800',
  peligro: 'bg-red-600 text-white hover:bg-red-700 shadow-sm',
  fantasma: 'text-marca-600 hover:bg-marca-50 dark:text-marca-500 dark:hover:bg-slate-800',
  // Sobre fondos de color (hero): relleno blanco o solo borde blanco
  claro: 'bg-white text-marca-700 hover:bg-marca-50 shadow-sm',
  contornoClaro: 'border border-white/80 text-white hover:bg-white/10',
}

const BASE_BOTON =
  'inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-5 py-2.5 text-sm font-semibold transition-colors ' +
  'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 ' +
  'disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500 disabled:shadow-none disabled:hover:bg-slate-200'

// También sirve para dar estilo de botón a un <Link>
export function clasesBoton(variante: VarianteBoton = 'primario', className?: string): string {
  return clsx(BASE_BOTON, VARIANTES[variante], className)
}

// Campos de formulario (input, select, textarea)
const CLASES_CONTROL =
  'block w-full rounded-lg border bg-white px-3.5 py-2.5 text-base text-slate-900 placeholder:text-slate-400 ' +
  'focus-visible:outline-2 focus-visible:outline-offset-0 focus-visible:outline-marca-600 ' +
  'dark:bg-slate-900 dark:text-slate-100'

export function clasesControl(conError: boolean): string {
  return clsx(CLASES_CONTROL, conError ? 'border-red-500' : 'border-slate-300 dark:border-slate-600')
}

// id del texto que describe al campo: el error si hay, si no la ayuda (FE-010, aria-describedby)
export function describedBy(id: string, error?: string, ayuda?: string): string | undefined {
  if (error) return `${id}-error`
  if (ayuda) return `${id}-ayuda`
  return undefined
}
