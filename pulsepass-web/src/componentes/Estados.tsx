import type { ReactNode } from 'react'
import { Boton } from './Boton'
import { IconoAlerta, IconoBandeja } from './Iconos'
import { Tarjeta } from './Tarjeta'

// PRD Frontend FE-014 y §9.20: cada pantalla con datos define sus estados cargando, vacío y error.

// Cargando: esqueletos con la forma del contenido (animate-pulse) y aria-busy
export function Cargando({ etiqueta = 'Cargando…' }: { etiqueta?: string }) {
  return (
    <div role="status" aria-busy="true" aria-label={etiqueta} className="flex flex-col gap-3 py-4">
      <div className="h-6 w-1/3 animate-pulse rounded bg-slate-200 dark:bg-slate-800" />
      <div className="h-4 w-2/3 animate-pulse rounded bg-slate-200 dark:bg-slate-800" />
      <div className="h-4 w-1/2 animate-pulse rounded bg-slate-200 dark:bg-slate-800" />
    </div>
  )
}

// Esqueleto de una grilla de tarjetas (cartelera, artistas, sedes)
export function CargandoTarjetas({ cantidad = 3, etiqueta = 'Cargando…' }: { cantidad?: number; etiqueta?: string }) {
  return (
    <div
      role="status"
      aria-busy="true"
      aria-label={etiqueta}
      className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3"
    >
      {Array.from({ length: cantidad }, (_, indice) => (
        <Tarjeta key={indice} className="overflow-hidden">
          <div className="h-36 animate-pulse bg-slate-100 dark:bg-slate-800" />
          <div className="flex flex-col gap-2 p-4">
            <div className="h-3 w-1/3 animate-pulse rounded bg-slate-200 dark:bg-slate-800" />
            <div className="h-5 w-3/4 animate-pulse rounded bg-slate-200 dark:bg-slate-800" />
            <div className="h-3 w-1/2 animate-pulse rounded bg-slate-200 dark:bg-slate-800" />
          </div>
        </Tarjeta>
      ))}
    </div>
  )
}

interface PropsEstadoVacio {
  mensaje: string
  accion?: ReactNode
}

export function EstadoVacio({ mensaje, accion }: PropsEstadoVacio) {
  return (
    <Tarjeta className="flex flex-col items-center gap-4 px-6 py-10 text-center">
      <IconoBandeja tamano={32} className="text-slate-500" />
      <p className="font-semibold">{mensaje}</p>
      {accion}
    </Tarjeta>
  )
}

interface PropsEstadoError {
  mensaje: string
  onReintentar?: () => void
}

export function EstadoError({ mensaje, onReintentar }: PropsEstadoError) {
  return (
    <Tarjeta className="flex flex-col items-center gap-4 px-6 py-8 text-center">
      <div
        role="alert"
        className="flex w-full max-w-md items-start justify-center gap-2 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-800 dark:bg-red-950 dark:text-red-200"
      >
        <IconoAlerta tamano={18} className="shrink-0" />
        <span>{mensaje}</span>
      </div>
      {onReintentar ? <Boton onClick={onReintentar}>Reintentar</Boton> : null}
    </Tarjeta>
  )
}
