import { Contenedor } from '../componentes/Contenedor'
import { CargandoTarjetas, EstadoError, EstadoVacio } from '../componentes/Estados'
import { TarjetaArtista } from '../funcionalidades/artistas/componentes/TarjetaArtista'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { mensajeDeError } from '../lib/errores'

// PRD Frontend §9.6, FR-FE-ART-001
export default function ListaArtistas() {
  const artistas = useArtistasActivos()
  return (
    <Contenedor>
      <h1 className="mb-6 text-3xl font-bold">Artistas</h1>
      {artistas.isPending ? (
        <CargandoTarjetas etiqueta="Cargando artistas…" />
      ) : artistas.isError ? (
        <EstadoError mensaje={mensajeDeError(artistas.error)} onReintentar={() => void artistas.refetch()} />
      ) : artistas.data.length === 0 ? (
        <EstadoVacio mensaje="Aún no hay artistas activos" />
      ) : (
        <ul className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {artistas.data.map((artista) => (
            <li key={artista.id} className="flex">
              <TarjetaArtista artista={artista} />
            </li>
          ))}
        </ul>
      )}
    </Contenedor>
  )
}
