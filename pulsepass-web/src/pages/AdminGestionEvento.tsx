import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { Aviso } from '../componentes/Aviso'
import { Badge } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { CodigoTexto } from '../componentes/CodigoTexto'
import { Contenedor } from '../componentes/Contenedor'
import { EnlaceVolver } from '../componentes/EnlaceVolver'
import { Cargando, EstadoError } from '../componentes/Estados'
import { NoDisponible } from '../componentes/NoDisponible'
import { Selector } from '../componentes/Selector'
import { Tarjeta } from '../componentes/Tarjeta'
import { useAvisos } from '../componentes/avisosContexto'
import { ChipArtista } from '../funcionalidades/artistas/componentes/ChipArtista'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { ModalConfirmarPublicacion } from '../funcionalidades/eventos/componentes/ModalConfirmarPublicacion'
import { actualizarEstadoCreado } from '../funcionalidades/eventos/creadosSesion'
import { useAsociarArtista, useEvento, usePublicarEvento } from '../funcionalidades/eventos/hooks'
import { esApiError, mensajeDeError } from '../lib/errores'
import { formatearFechaHora } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §9.18 (AdminGestion.png). FR-FE-ADM-004 (publicar) y FR-FE-ADM-005 (asociar artista).
// La UI evita acciones obviamente inválidas, pero el backend sigue siendo la autoridad (409).
export default function AdminGestionEvento() {
  const { eventCode } = useParams()
  const evento = useEvento(eventCode)
  const artistas = useArtistasActivos()
  const publicar = usePublicarEvento()
  const asociar = useAsociarArtista()
  const { mostrarAviso } = useAvisos()
  const [modalPublicar, setModalPublicar] = useState(false)
  const [errorPublicar, setErrorPublicar] = useState<string | null>(null)
  const [errorAsociar, setErrorAsociar] = useState<string | null>(null)
  const [artistaElegido, setArtistaElegido] = useState('')

  if (evento.isPending) {
    return (
      <Contenedor ancho="estrecho">
        <Cargando etiqueta="Cargando evento…" />
      </Contenedor>
    )
  }
  if (evento.isError) {
    if (esApiError(evento.error, 404)) {
      return (
        <NoDisponible
          titulo="Evento no encontrado"
          mensaje={`No existe un evento con el código ${eventCode ?? ''}.`}
          volverA={RUTAS.adminEventos}
          textoVolver="Volver a eventos"
        />
      )
    }
    return (
      <Contenedor ancho="estrecho">
        <EstadoError mensaje={mensajeDeError(evento.error)} onReintentar={() => void evento.refetch()} />
      </Contenedor>
    )
  }

  const datos = evento.data
  const asociados = new Set(datos.artists.map((a) => a.id))

  function confirmarPublicacion() {
    setErrorPublicar(null)
    publicar.mutate(datos.eventCode, {
      onSuccess: (actualizado) => {
        setModalPublicar(false)
        actualizarEstadoCreado(actualizado.eventCode, actualizado.status)
        mostrarAviso('exito', 'Evento publicado.')
      },
      onError: (error) => {
        setModalPublicar(false)
        if (!esApiError(error, 401)) setErrorPublicar(mensajeDeError(error))
      },
    })
  }

  function asociarArtista() {
    const artista = artistas.data?.find((a) => String(a.id) === artistaElegido)
    if (!artista) return
    setErrorAsociar(null)
    asociar.mutate(
      { eventCode: datos.eventCode, artistId: artista.id, stageName: artista.stageName },
      {
        onSuccess: () => {
          setArtistaElegido('')
          mostrarAviso('exito', `${artista.stageName} asociado al evento.`)
        },
        onError: (error) => {
          if (!esApiError(error, 401)) setErrorAsociar(mensajeDeError(error))
        },
      },
    )
  }

  return (
    <Contenedor ancho="estrecho">
      <EnlaceVolver a={RUTAS.adminEventos} texto="Eventos" />

      <Tarjeta className="mb-6 flex flex-col gap-2 p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <h1 className="text-2xl font-bold">
            {datos.name} <CodigoTexto className="text-base font-normal text-slate-500">· {datos.eventCode}</CodigoTexto>
          </h1>
          <Badge tono={datos.status} />
        </div>
        <p className="text-sm text-slate-600 dark:text-slate-400">
          {datos.category} · {formatearFechaHora(datos.eventDate)} · Mín. {datos.minimumAge} · {datos.venueName}
        </p>
      </Tarjeta>

      <Tarjeta className="mb-6 flex flex-col gap-3 p-6">
        <h2 className="text-lg font-semibold">Publicación</h2>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-slate-600 dark:text-slate-400">
            {datos.status === 'DRAFT'
              ? 'El evento es visible al público una vez publicado.'
              : 'Solo un evento en borrador (DRAFT) se puede publicar.'}
          </p>
          <Boton
            disabled={datos.status !== 'DRAFT'}
            onClick={() => {
              setErrorPublicar(null)
              setModalPublicar(true)
            }}
          >
            Publicar evento
          </Boton>
        </div>
        {errorPublicar ? <Aviso tono="error">{errorPublicar}</Aviso> : null}
      </Tarjeta>

      <Tarjeta className="mb-6 flex flex-col gap-4 p-6">
        <h2 className="text-lg font-semibold">Artistas asociados</h2>
        {datos.artists.length === 0 ? (
          <p className="text-sm text-slate-600 dark:text-slate-400">Este evento aún no tiene artistas.</p>
        ) : (
          <ul aria-label="Artistas del evento" className="flex flex-wrap gap-4">
            {datos.artists.map((artista) => (
              <li key={artista.id}>
                <ChipArtista artista={artista} />
              </li>
            ))}
          </ul>
        )}
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end">
          <Selector
            etiqueta="Agregar artista"
            envolturaClassName="flex-1"
            value={artistaElegido}
            onChange={(e) => setArtistaElegido(e.target.value)}
            disabled={artistas.isPending}
          >
            <option value="">Elige un artista</option>
            {/* Los ya asociados se muestran deshabilitados: la UI evita el duplicado obvio */}
            {(artistas.data ?? []).map((artista) => (
              <option key={artista.id} value={String(artista.id)} disabled={asociados.has(artista.id)}>
                {artista.stageName}
                {asociados.has(artista.id) ? ' (ya asociado)' : ''}
              </option>
            ))}
          </Selector>
          <Boton onClick={asociarArtista} disabled={!artistaElegido} cargando={asociar.isPending}>
            Asociar
          </Boton>
        </div>
        {errorAsociar ? <Aviso tono="error">{errorAsociar}</Aviso> : null}
      </Tarjeta>

      <Link
        to={RUTAS.staffTicketsPagados(datos.eventCode)}
        className="text-sm font-medium text-marca-600 hover:text-marca-700 dark:text-marca-500"
      >
        Ver tickets pagados de este evento →
      </Link>

      <ModalConfirmarPublicacion
        abierto={modalPublicar}
        nombreEvento={datos.name}
        cargando={publicar.isPending}
        onConfirmar={confirmarPublicacion}
        onCerrar={() => setModalPublicar(false)}
      />
    </Contenedor>
  )
}
