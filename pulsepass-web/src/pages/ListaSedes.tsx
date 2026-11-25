import { Contenedor } from '../componentes/Contenedor'
import { CargandoTarjetas, EstadoError, EstadoVacio } from '../componentes/Estados'
import { TarjetaSede } from '../funcionalidades/sedes/componentes/TarjetaSede'
import { useSedesActivas } from '../funcionalidades/sedes/hooks'
import { mensajeDeError } from '../lib/errores'

// PRD Frontend §9.7, FR-FE-VEN-001
export default function ListaSedes() {
  const sedes = useSedesActivas()
  return (
    <Contenedor>
      <h1 className="mb-6 text-3xl font-bold">Sedes</h1>
      {sedes.isPending ? (
        <CargandoTarjetas etiqueta="Cargando sedes…" />
      ) : sedes.isError ? (
        <EstadoError mensaje={mensajeDeError(sedes.error)} onReintentar={() => void sedes.refetch()} />
      ) : sedes.data.length === 0 ? (
        <EstadoVacio mensaje="Aún no hay sedes activas" />
      ) : (
        <ul className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {sedes.data.map((sede) => (
            <li key={sede.code} className="flex">
              <TarjetaSede sede={sede} />
            </li>
          ))}
        </ul>
      )}
    </Contenedor>
  )
}
