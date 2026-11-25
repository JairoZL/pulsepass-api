import { clsx } from 'clsx'
import { iniciales } from '../lib/formato'

// Círculo con iniciales (la API no entrega fotos). El color se elige de un mapa fijo según el texto
const TONOS = [
  'bg-marca-100 text-marca-700',
  'bg-emerald-100 text-emerald-700',
  'bg-sky-100 text-sky-700',
  'bg-rose-100 text-rose-700',
  'bg-amber-100 text-amber-700',
] as const

const TAMANOS = {
  sm: 'h-8 w-8 text-xs',
  md: 'h-12 w-12 text-base',
  lg: 'h-16 w-16 text-xl',
  xl: 'h-28 w-28 text-4xl',
} as const

function tonoPara(texto: string): string {
  let suma = 0
  for (const caracter of texto) suma += caracter.charCodeAt(0)
  return TONOS[suma % TONOS.length] ?? TONOS[0]
}

interface PropsAvatar {
  nombre: string
  tamano?: keyof typeof TAMANOS
  className?: string
}

export function Avatar({ nombre, tamano = 'md', className }: PropsAvatar) {
  return (
    <span
      aria-hidden="true"
      className={clsx(
        'inline-flex shrink-0 items-center justify-center rounded-full font-titulo font-semibold',
        TAMANOS[tamano],
        tonoPara(nombre),
        className,
      )}
    >
      {iniciales(nombre)}
    </span>
  )
}
