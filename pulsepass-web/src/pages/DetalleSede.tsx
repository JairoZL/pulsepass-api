import { useParams } from 'react-router'
import { Badge } from '../componentes/Badge'
import { CodigoTexto } from '../componentes/CodigoTexto'
import { Contenedor } from '../componentes/Contenedor'
import { EnlaceVolver } from '../componentes/EnlaceVolver'
import { Cargando, EstadoError } from '../componentes/Estados'
import { IconoUbicacion } from '../componentes/Iconos'
import { Tarjeta } from '../componentes/Tarjeta'
import { useSede } from '../funcionalidades/sedes/hooks'
import { esApiError, mensajeDeError } from '../lib/errores'
import { RUTAS } from '../lib/rutas'
import { PaginaCodigo } from './PaginaCodigo'

// PRD Frontend §9.7 (DetalleSede.png), FR-FE-VEN-002
export default function DetalleSede() {
  const { code } = useParams()
  const sede = useSede(code)

  if (sede.isPending) {
    return (
      <Contenedor ancho="estrecho">
        <Cargando etiqueta="Cargando sede…" />
      </Contenedor>
    )
  }
  if (sede.isError) {
    if (esApiError(sede.error, 404)) return <PaginaCodigo codigo="404" mensaje="No encontramos esta página." />
    return (
      <Contenedor ancho="estrecho">
        <EstadoError mensaje={mensajeDeError(sede.error)} onReintentar={() => void sede.refetch()} />
      </Contenedor>
    )
  }

  const datos = sede.data
  return (
    <Contenedor ancho="estrecho">
      <EnlaceVolver a={RUTAS.sedes} texto="Sedes" />
      <Tarjeta className="flex flex-col gap-4 p-6">
        <h1 className="text-3xl font-bold">{datos.name}</h1>
        <div className="flex items-center gap-3 text-sm">
          <CodigoTexto className="text-slate-600 dark:text-slate-400">{datos.code}</CodigoTexto>
          <Badge tono={datos.active ? 'ACTIVA' : 'INACTIVA'}>{datos.active ? 'Activa' : 'Inactiva'}</Badge>
        </div>
        <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-[auto_1fr] sm:gap-x-6">
          <dt className="text-slate-600 dark:text-slate-400">Ciudad</dt>
          <dd className="flex items-center gap-2">
            <IconoUbicacion tamano={16} />
            {datos.city}
          </dd>
          <dt className="text-slate-600 dark:text-slate-400">Capacidad</dt>
          <dd>{datos.capacity} personas</dd>
          <dt className="text-slate-600 dark:text-slate-400">Dirección</dt>
          <dd>{datos.address}</dd>
        </dl>
      </Tarjeta>
    </Contenedor>
  )
}
