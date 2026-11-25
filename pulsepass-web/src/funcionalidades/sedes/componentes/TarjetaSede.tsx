import { Link } from 'react-router'
import { Badge } from '../../../componentes/Badge'
import { IconoUbicacion } from '../../../componentes/Iconos'
import { RUTAS } from '../../../lib/rutas'
import type { VenueResponse } from '../tipos'

export function TarjetaSede({ sede }: { sede: VenueResponse }) {
  return (
    <Link
      to={RUTAS.sede(sede.code)}
      className="flex w-full flex-col gap-2 rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition-shadow hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 dark:border-slate-800 dark:bg-slate-900"
    >
      <h3 className="text-lg font-semibold">{sede.name}</h3>
      <p className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
        <IconoUbicacion tamano={16} />
        {sede.city} · Cap. {sede.capacity}
      </p>
      <div>
        <Badge tono={sede.active ? 'ACTIVA' : 'INACTIVA'}>{sede.active ? 'Activa' : 'Inactiva'}</Badge>
      </div>
    </Link>
  )
}
