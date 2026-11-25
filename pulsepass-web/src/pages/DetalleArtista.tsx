import { useParams } from 'react-router'
import { Avatar } from '../componentes/Avatar'
import { Badge } from '../componentes/Badge'
import { Contenedor } from '../componentes/Contenedor'
import { EnlaceVolver } from '../componentes/EnlaceVolver'
import { Cargando, CargandoTarjetas, EstadoError, EstadoVacio } from '../componentes/Estados'
import { Tarjeta } from '../componentes/Tarjeta'
import { useArtista } from '../funcionalidades/artistas/hooks'
import { GrillaEventos } from '../funcionalidades/eventos/componentes/GrillaEventos'
import { useEventosPorArtista } from '../funcionalidades/eventos/hooks'
import { esApiError, mensajeDeError } from '../lib/errores'
import { RUTAS } from '../lib/rutas'
import { PaginaCodigo } from './PaginaCodigo'

// "12" -> 12; "abc", "1.5", "-3" -> null (FE-002: el parámetro de URL es dato externo)
function idValido(texto: string | undefined): number | null {
  const id = Number(texto)
  return Number.isInteger(id) && id > 0 ? id : null
}

// PRD Frontend §9.6 (DetalleArtista.png)
export default function DetalleArtista() {
  const params = useParams()
  const id = idValido(params.id)
  const artista = useArtista(id) // con id null no llama a la API (TEST-FE-ART-003)
  const eventos = useEventosPorArtista(artista.data?.stageName)

  if (id === null) return <PaginaCodigo codigo="404" mensaje="No encontramos esta página." />
  if (artista.isPending) {
    return (
      <Contenedor>
        <Cargando etiqueta="Cargando artista…" />
      </Contenedor>
    )
  }
  if (artista.isError) {
    if (esApiError(artista.error, 404)) return <PaginaCodigo codigo="404" mensaje="No encontramos esta página." />
    return (
      <Contenedor>
        <EstadoError mensaje={mensajeDeError(artista.error)} onReintentar={() => void artista.refetch()} />
      </Contenedor>
    )
  }

  const { stageName, genre, country, active } = artista.data
  return (
    <Contenedor>
      <EnlaceVolver a={RUTAS.artistas} texto="Artistas" />
      <Tarjeta className="mb-8 flex flex-col items-center gap-6 p-6 text-center sm:flex-row sm:text-left">
        <Avatar nombre={stageName} tamano="xl" />
        <div className="flex flex-col gap-2">
          <h1 className="text-3xl font-bold sm:text-4xl">{stageName}</h1>
          <p className="text-slate-600 dark:text-slate-400">
            {genre} · {country}
          </p>
          <div>
            <Badge tono={active ? 'ACTIVA' : 'INACTIVA'}>{active ? 'Activo' : 'Inactivo'}</Badge>
          </div>
        </div>
      </Tarjeta>

      <h2 className="mb-6 text-xl font-semibold">Próximos eventos de {stageName}</h2>
      {eventos.isPending ? (
        <CargandoTarjetas etiqueta="Cargando eventos…" />
      ) : eventos.isError ? (
        <EstadoError mensaje={mensajeDeError(eventos.error)} onReintentar={() => void eventos.refetch()} />
      ) : eventos.data.length === 0 ? (
        <EstadoVacio mensaje="Sin eventos próximos" />
      ) : (
        <GrillaEventos eventos={eventos.data} />
      )}
    </Contenedor>
  )
}
