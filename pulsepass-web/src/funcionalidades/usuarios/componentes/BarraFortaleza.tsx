import { clsx } from 'clsx'
import { calcularFortaleza } from '../fortaleza'

const SEGMENTOS = 8

export function BarraFortaleza({ clave }: { clave: string }) {
  const { puntos, etiqueta } = calcularFortaleza(clave)
  return (
    <div className="flex flex-col gap-1.5">
      <div className="grid grid-cols-8 gap-1.5" aria-hidden="true">
        {Array.from({ length: SEGMENTOS }, (_, indice) => (
          <span
            key={indice}
            className={clsx('h-1.5 rounded-full', indice < puntos ? 'bg-marca-600' : 'bg-slate-200 dark:bg-slate-700')}
          />
        ))}
      </div>
      <p className="text-xs text-slate-500 dark:text-slate-400">Fortaleza: {etiqueta}</p>
    </div>
  )
}
