import { Link } from 'react-router'
import { Badge } from '../../../componentes/Badge'
import { Boton } from '../../../componentes/Boton'
import { CodigoTexto } from '../../../componentes/CodigoTexto'
import { clasesBoton } from '../../../componentes/estilos'
import { Tarjeta } from '../../../componentes/Tarjeta'
import { formatearFecha, formatearPrecio } from '../../../lib/formato'
import { RUTAS } from '../../../lib/rutas'
import type { TicketResponse } from '../tipos'

// PRD Frontend §9.11 (MisTickets.png). "Cancelar" solo si está PAID (TEST-FE-TKT-010)
export function TarjetaTicket({ ticket, onCancelar }: { ticket: TicketResponse; onCancelar: (ticket: TicketResponse) => void }) {
  return (
    <Tarjeta className="flex flex-col gap-3 p-5">
      <div className="flex items-start justify-between gap-3">
        <h2 className="text-lg font-semibold">{ticket.eventName}</h2>
        <Badge tono={ticket.status} />
      </div>
      <p className="text-sm text-slate-600 dark:text-slate-400">
        <CodigoTexto>{ticket.ticketCode}</CodigoTexto> · {ticket.type} · {formatearPrecio(ticket.price)} ·{' '}
        {formatearFecha(ticket.purchaseDate)}
      </p>
      <div className="flex flex-wrap justify-end gap-3">
        <Link
          to={RUTAS.ticket(ticket.ticketCode)}
          className={clasesBoton('secundario')}
          aria-label={`Ver detalle del ticket ${ticket.ticketCode}`}
        >
          Ver detalle
        </Link>
        {ticket.status === 'PAID' ? (
          <Boton variante="peligro" onClick={() => onCancelar(ticket)} aria-label={`Cancelar ticket ${ticket.ticketCode}`}>
            Cancelar
          </Boton>
        ) : null}
      </div>
    </Tarjeta>
  )
}
