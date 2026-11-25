import { clsx } from 'clsx'
import { Link } from 'react-router'
import { Badge } from '../../../componentes/Badge'
import { IconoCalendario, IconoUbicacion } from '../../../componentes/Iconos'
import { formatearFecha } from '../../../lib/formato'
import { RUTAS } from '../../../lib/rutas'
import type { EventSummaryResponse } from '../tipos'
import { APARIENCIA_CATEGORIA } from './aparienciaCategoria'

// PRD Frontend §9.4: la tarjeta completa es un enlace al detalle del evento.
// El backend entrega el nombre de la sede (venueName), no la ciudad (brecha documentada en el README)
export function TarjetaEvento({ evento }: { evento: EventSummaryResponse }) {
  const { panel, texto, Icono } = APARIENCIA_CATEGORIA[evento.category]
  return (
    <Link
      to={RUTAS.evento(evento.eventCode)}
      className="group flex flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm transition-shadow hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 dark:border-slate-800 dark:bg-slate-900"
    >
      <div className={clsx('flex h-36 items-center justify-center', panel, texto)}>
        <Icono tamano={44} />
      </div>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <p className={clsx('text-xs font-bold tracking-widest uppercase', texto)}>{evento.category}</p>
        <h3 className="text-lg font-semibold group-hover:text-marca-700 dark:group-hover:text-marca-500">
          {evento.name}
        </h3>
        <p className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
          <IconoCalendario tamano={16} />
          {formatearFecha(evento.eventDate)}
        </p>
        <p className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
          <IconoUbicacion tamano={16} />
          {evento.venueName}
        </p>
        <div className="mt-1">
          <Badge tono={evento.status} />
        </div>
      </div>
    </Link>
  )
}
