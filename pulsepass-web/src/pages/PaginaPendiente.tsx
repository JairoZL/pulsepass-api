import { Contenedor } from '../componentes/Contenedor'
import { EstadoVacio } from '../componentes/Estados'

// Marcador temporal del bloque A: la ruta existe (lazy verificado, §29 etapa 4) y la página
// real llega en un bloque siguiente. Se elimina cuando todas las páginas estén implementadas.
export function PaginaPendiente({ titulo, bloque }: { titulo: string; bloque: 'B' | 'C' | 'D' }) {
  return (
    <Contenedor>
      <h1 className="mb-6 text-3xl font-bold">{titulo}</h1>
      <EstadoVacio mensaje={`Esta página se implementa en el bloque ${bloque}.`} />
    </Contenedor>
  )
}
