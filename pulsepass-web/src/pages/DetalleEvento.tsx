import { useParams } from 'react-router'
import { Contenedor } from '../componentes/Contenedor'
import { EnlaceVolver } from '../componentes/EnlaceVolver'
import { Cargando, EstadoError } from '../componentes/Estados'
import { NoDisponible } from '../componentes/NoDisponible'
import { Tarjeta } from '../componentes/Tarjeta'
import { ChipArtista } from '../funcionalidades/artistas/componentes/ChipArtista'
import { DetalleEventoCabecera } from '../funcionalidades/eventos/componentes/DetalleEventoCabecera'
import { useEvento } from '../funcionalidades/eventos/hooks'
import { ResumenSede } from '../funcionalidades/sedes/componentes/ResumenSede'
import { PanelCompra } from '../funcionalidades/tickets/componentes/PanelCompra'
import { esApiError, mensajeDeError } from '../lib/errores'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §9.5 (DetalleEvento.png, ModalCompra.png, MovilEvento.png)
export default function DetalleEvento() {
  const { eventCode } = useParams()
  const evento = useEvento(eventCode)

  if (evento.isPending) {
    return (
      <Contenedor>
        <Cargando etiqueta="Cargando evento…" />
      </Contenedor>
    )
  }

  // GAP-02 / AC-FE-003: inexistente o borrador (404/403) -> "no disponible", sin datos del borrador
  if (evento.isError) {
    if (esApiError(evento.error, 404, 403)) {
      return (
        <NoDisponible
          titulo="Evento no disponible"
          mensaje="Este evento no existe o todavía no está publicado."
          volverA={RUTAS.eventos}
          textoVolver="Volver a la cartelera"
        />
      )
    }
    return (
      <Contenedor>
        <EstadoError mensaje={mensajeDeError(evento.error)} onReintentar={() => void evento.refetch()} />
      </Contenedor>
    )
  }

  const datos = evento.data
  return (
    // pb-28 en móvil: deja espacio para la barra de compra fija (DES-07)
    <Contenedor className="pb-28 md:pb-8">
      <EnlaceVolver a={RUTAS.eventos} texto="Volver a la cartelera" />
      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[1fr_380px]">
        <Tarjeta className="overflow-hidden pb-6">
          <DetalleEventoCabecera evento={datos} />
          <div className="flex flex-col gap-6 px-6 pt-4">
            <p className="leading-relaxed text-slate-700 dark:text-slate-300">{datos.description}</p>

            <section aria-labelledby="titulo-artistas-evento">
              <h2 id="titulo-artistas-evento" className="mb-3 text-lg font-semibold">
                Artistas
              </h2>
              {datos.artists.length === 0 ? (
                <p className="text-sm text-slate-600 dark:text-slate-400">Aún no hay artistas anunciados.</p>
              ) : (
                <ul className="flex flex-wrap gap-4">
                  {datos.artists.map((artista) => (
                    <li key={artista.id}>
                      <ChipArtista artista={artista} />
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section aria-labelledby="titulo-sede-evento">
              <h2 id="titulo-sede-evento" className="mb-3 text-lg font-semibold">
                Sede
              </h2>
              <ResumenSede venueCode={datos.venueCode} venueName={datos.venueName} />
            </section>
          </div>
        </Tarjeta>

        <PanelCompra evento={datos} />
      </div>
    </Contenedor>
  )
}
