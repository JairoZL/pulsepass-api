import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { Contenedor } from '../componentes/Contenedor'
import { CargandoTarjetas, EstadoError, EstadoVacio } from '../componentes/Estados'
import { IconoBuscar } from '../componentes/Iconos'
import { Selector } from '../componentes/Selector'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { GrillaEventos } from '../funcionalidades/eventos/componentes/GrillaEventos'
import { useEventosPorArtista, useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { EventCategorySchema } from '../funcionalidades/eventos/tipos'
import { mensajeDeError } from '../lib/errores'

// PRD Frontend §9.4 y FR-FE-EVT-002: los filtros viven en la URL (?q=&categoria=&artista=), así
// sobreviven a recargar y se pueden compartir. Texto y categoría filtran en el cliente; el artista
// usa GET /api/events/by-artist (la API no tiene búsqueda en servidor).
export default function CatalogoEventos() {
  const [parametros, setParametros] = useSearchParams()
  // El texto vive también en un estado local: si el input dependiera solo de la URL, cada tecla
  // esperaría a la navegación y el cursor saltaría. La URL se actualiza igual en cada cambio.
  const [texto, setTexto] = useState(parametros.get('q') ?? '')
  // FE-002: el parámetro de la URL se valida; un valor inventado se ignora
  const categoria = EventCategorySchema.safeParse(parametros.get('categoria')).data
  const artista = parametros.get('artista') ?? undefined

  const publicados = useEventosPublicados()
  const porArtista = useEventosPorArtista(artista)
  const consulta = artista ? porArtista : publicados
  const artistas = useArtistasActivos()

  function cambiar(clave: string, valor: string) {
    const siguientes = new URLSearchParams(parametros)
    if (valor) siguientes.set(clave, valor)
    else siguientes.delete(clave)
    setParametros(siguientes, { replace: true })
  }

  const eventos = (consulta.data ?? []).filter(
    (evento) =>
      evento.name.toLowerCase().includes(texto.trim().toLowerCase()) && (!categoria || evento.category === categoria),
  )

  return (
    <Contenedor>
      <h1 className="mb-6 text-3xl font-bold">Cartelera</h1>

      <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-[2fr_1fr_1fr_auto] md:items-end">
        <Campo
          etiqueta="Buscar por nombre"
          type="search"
          placeholder="Buscar por nombre…"
          value={texto}
          onChange={(evento) => {
            setTexto(evento.target.value)
            cambiar('q', evento.target.value)
          }}
          iconoInicio={<IconoBuscar tamano={18} />}
        />
        <Selector etiqueta="Categoría" value={categoria ?? ''} onChange={(evento) => cambiar('categoria', evento.target.value)}>
          <option value="">Todas</option>
          {EventCategorySchema.options.map((opcion) => (
            <option key={opcion} value={opcion}>
              {opcion}
            </option>
          ))}
        </Selector>
        <Selector etiqueta="Artista" value={artista ?? ''} onChange={(evento) => cambiar('artista', evento.target.value)}>
          <option value="">Todos</option>
          {(artistas.data ?? []).map((opcion) => (
            <option key={opcion.id} value={opcion.stageName}>
              {opcion.stageName}
            </option>
          ))}
        </Selector>
        <Boton
          variante="secundario"
          onClick={() => {
            setTexto('')
            setParametros(new URLSearchParams(), { replace: true })
          }}
        >
          Limpiar
        </Boton>
      </div>

      {consulta.isPending ? (
        <CargandoTarjetas etiqueta="Cargando eventos…" />
      ) : consulta.isError ? (
        <EstadoError mensaje={mensajeDeError(consulta.error)} onReintentar={() => void consulta.refetch()} />
      ) : (
        <>
          <p className="mb-4 text-sm text-slate-600 dark:text-slate-400" aria-live="polite">
            {eventos.length === 1 ? '1 evento' : `${eventos.length} eventos`}
          </p>
          {eventos.length === 0 ? (
            <EstadoVacio
              mensaje={consulta.data.length === 0 ? 'Aún no hay eventos publicados' : 'Sin resultados para tu búsqueda'}
            />
          ) : (
            <GrillaEventos eventos={eventos} />
          )}
        </>
      )}
    </Contenedor>
  )
}
