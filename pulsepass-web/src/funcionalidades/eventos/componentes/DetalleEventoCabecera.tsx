import { clsx } from 'clsx'
import { Badge } from '../../../componentes/Badge'
import { IconoCalendario, IconoUsuario } from '../../../componentes/Iconos'
import { formatearFechaHora } from '../../../lib/formato'
import type { EventResponse } from '../tipos'
import { APARIENCIA_CATEGORIA } from './aparienciaCategoria'

// Banner de color + categoría, estado, nombre, fecha y edad mínima (DetalleEvento.png)
export function DetalleEventoCabecera({ evento }: { evento: EventResponse }) {
  const { panel, texto, Icono } = APARIENCIA_CATEGORIA[evento.category]
  return (
    <>
      <div className={clsx('flex h-56 items-center justify-center', panel, texto)}>
        <Icono tamano={72} />
      </div>
      <div className="flex flex-col gap-3 px-6 pt-6">
        <div className="flex items-center gap-3">
          <span className={clsx('text-xs font-bold tracking-widest uppercase', texto)}>{evento.category}</span>
          <Badge tono={evento.status} />
        </div>
        <h1 className="text-3xl font-bold sm:text-4xl">{evento.name}</h1>
        <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-slate-600 dark:text-slate-400">
          <span className="flex items-center gap-2">
            <IconoCalendario tamano={16} />
            {formatearFechaHora(evento.eventDate)}
          </span>
          {/* La edad mínima se informa; validarla es regla del backend (§24.1) */}
          <span className="flex items-center gap-2">
            <IconoUsuario tamano={16} />
            Edad mínima: {evento.minimumAge}
          </span>
        </div>
      </div>
    </>
  )
}
