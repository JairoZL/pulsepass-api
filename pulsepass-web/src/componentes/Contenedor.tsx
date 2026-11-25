import { clsx } from 'clsx'
import type { ReactNode } from 'react'

// Contenedor de página: max-w-6xl (normal) o columna estrecha centrada para formularios y gestión (DES-04)
const ANCHOS = {
  normal: 'max-w-6xl',
  estrecho: 'max-w-3xl',
} as const

interface PropsContenedor {
  ancho?: keyof typeof ANCHOS
  className?: string
  children: ReactNode
}

export function Contenedor({ ancho = 'normal', className, children }: PropsContenedor) {
  return <div className={clsx('mx-auto w-full px-4 py-8 sm:px-6', ANCHOS[ancho], className)}>{children}</div>
}
