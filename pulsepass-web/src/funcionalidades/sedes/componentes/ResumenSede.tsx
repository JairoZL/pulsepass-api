import { Link } from 'react-router'
import { IconoEdificio } from '../../../componentes/Iconos'
import { RUTAS } from '../../../lib/rutas'
import { useSede } from '../hooks'

// Bloque "Sede" del detalle de evento. GAP-04: se muestra la capacidad de la sede (la API no informa
// entradas disponibles). Mientras carga o si falla, se muestra al menos el nombre que trae el evento.
export function ResumenSede({ venueCode, venueName }: { venueCode: string; venueName: string }) {
  const sede = useSede(venueCode)
  return (
    <Link
      to={RUTAS.sede(venueCode)}
      className="flex items-start gap-3 rounded-lg border border-slate-200 p-4 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-marca-600 dark:border-slate-800 dark:hover:bg-slate-800"
    >
      <IconoEdificio className="mt-0.5 shrink-0 text-marca-600" />
      <span>
        <span className="block font-semibold">{venueName}</span>
        {sede.data ? (
          <span className="block text-sm text-slate-600 dark:text-slate-400">
            {sede.data.city} · Capacidad {sede.data.capacity}
          </span>
        ) : null}
      </span>
    </Link>
  )
}
