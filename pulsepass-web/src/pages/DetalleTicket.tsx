import { useParams } from 'react-router'
import { Badge } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { CodigoTexto } from '../componentes/CodigoTexto'
import { Contenedor } from '../componentes/Contenedor'
import { EnlaceVolver } from '../componentes/EnlaceVolver'
import { Cargando, EstadoError } from '../componentes/Estados'
import { PrecioCOP } from '../componentes/PrecioCOP'
import { Tarjeta } from '../componentes/Tarjeta'
import { BotonCopiar } from '../funcionalidades/tickets/componentes/BotonCopiar'
import { ModalConfirmarCancelacion } from '../funcionalidades/tickets/componentes/ModalConfirmarCancelacion'
import { TicketNoDisponible } from '../funcionalidades/tickets/componentes/TicketNoDisponible'
import { useCancelacion } from '../funcionalidades/tickets/componentes/useCancelacion'
import { useTicket } from '../funcionalidades/tickets/hooks'
import { esApiError, mensajeDeError } from '../lib/errores'
import { formatearFechaHora } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §9.12 (DetalleTicket.png), FR-FE-TKT-004
export default function DetalleTicket() {
  const { ticketCode } = useParams()
  const ticket = useTicket(ticketCode)
  const cancelacion = useCancelacion()

  if (ticket.isPending) {
    return (
      <Contenedor ancho="estrecho">
        <Cargando etiqueta="Cargando ticket…" />
      </Contenedor>
    )
  }
  if (ticket.isError) {
    if (esApiError(ticket.error, 403, 404)) return <TicketNoDisponible />
    return (
      <Contenedor ancho="estrecho">
        <EstadoError mensaje={mensajeDeError(ticket.error)} onReintentar={() => void ticket.refetch()} />
      </Contenedor>
    )
  }

  const datos = ticket.data
  return (
    <Contenedor ancho="estrecho">
      <EnlaceVolver a={RUTAS.misTickets} texto="Mis tickets" />
      <Tarjeta className="flex flex-col gap-4 p-6">
        <div className="flex items-start justify-between gap-3 border-b border-slate-200 pb-4 dark:border-slate-800">
          <h1 className="text-2xl font-bold">{datos.eventName}</h1>
          <Badge tono={datos.status} />
        </div>
        <dl className="grid grid-cols-[auto_1fr] items-center gap-x-6 gap-y-3 text-sm">
          <dt className="text-slate-600 dark:text-slate-400">Código</dt>
          <dd className="flex flex-wrap items-center gap-2">
            <CodigoTexto className="text-base font-semibold">{datos.ticketCode}</CodigoTexto>
            <BotonCopiar texto={datos.ticketCode} />
          </dd>
          <dt className="text-slate-600 dark:text-slate-400">Tipo</dt>
          <dd>{datos.type}</dd>
          <dt className="text-slate-600 dark:text-slate-400">Precio</dt>
          <dd>
            <PrecioCOP valor={datos.price} />
          </dd>
          <dt className="text-slate-600 dark:text-slate-400">Comprado el</dt>
          <dd>{formatearFechaHora(datos.purchaseDate)}</dd>
          {/* La API no trae el nombre del titular: se muestra su correo (brecha documentada) */}
          <dt className="text-slate-600 dark:text-slate-400">Titular</dt>
          <dd>{datos.userEmail}</dd>
        </dl>
        <p className="text-sm text-slate-600 dark:text-slate-400">Presenta este código en el ingreso.</p>
      </Tarjeta>

      {datos.status === 'PAID' ? (
        <div className="mt-6 flex justify-end">
          <Boton variante="peligro" onClick={() => cancelacion.pedir(datos)}>
            Cancelar ticket
          </Boton>
        </div>
      ) : null}

      <ModalConfirmarCancelacion
        ticket={cancelacion.pendiente}
        cargando={cancelacion.cargando}
        error={cancelacion.errorModal}
        onConfirmar={cancelacion.confirmar}
        onCerrar={cancelacion.cerrar}
      />
    </Contenedor>
  )
}
