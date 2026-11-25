import { Link, useParams } from 'react-router'
import { Badge } from '../componentes/Badge'
import { CodigoTexto } from '../componentes/CodigoTexto'
import { Contenedor } from '../componentes/Contenedor'
import { Cargando, EstadoError } from '../componentes/Estados'
import { clasesBoton } from '../componentes/estilos'
import { IconoCheckCirculo } from '../componentes/Iconos'
import { Tarjeta } from '../componentes/Tarjeta'
import { TicketNoDisponible } from '../funcionalidades/tickets/componentes/TicketNoDisponible'
import { useTicket } from '../funcionalidades/tickets/hooks'
import { esApiError, mensajeDeError } from '../lib/errores'
import { formatearPrecio } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §9.13 (Confirmacion.png). Tras comprar, el ticket ya está en caché (useComprarTicket
// lo guarda), así que normalmente se pinta sin esperar a la API.
export default function ConfirmacionCompra() {
  const { ticketCode } = useParams()
  const ticket = useTicket(ticketCode)

  if (ticket.isPending) {
    return (
      <Contenedor ancho="estrecho">
        <Cargando etiqueta="Cargando compra…" />
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
      <Tarjeta className="mx-auto flex max-w-lg flex-col items-center gap-4 px-6 py-10 text-center">
        <IconoCheckCirculo tamano={56} className="text-emerald-600" />
        <h1 className="text-2xl font-bold">¡Compra realizada con éxito!</h1>
        <p className="text-lg font-semibold">{datos.eventName}</p>
        <p className="flex flex-wrap items-center justify-center gap-2 text-sm text-slate-600 dark:text-slate-400">
          <span>
            Ticket <CodigoTexto>{datos.ticketCode}</CodigoTexto> · {datos.type} · {formatearPrecio(datos.price)}
          </span>
          <Badge tono={datos.status} />
        </p>
        <div className="mt-2 flex flex-col gap-3 sm:flex-row">
          <Link to={RUTAS.misTickets} className={clasesBoton('primario')}>
            Ver mis tickets
          </Link>
          <Link to={RUTAS.eventos} className={clasesBoton('secundario')}>
            Seguir explorando
          </Link>
        </div>
      </Tarjeta>
    </Contenedor>
  )
}
