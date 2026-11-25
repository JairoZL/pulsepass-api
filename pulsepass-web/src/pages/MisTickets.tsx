import { Link, useSearchParams } from 'react-router'
import { Contenedor } from '../componentes/Contenedor'
import { CargandoTarjetas, EstadoError, EstadoVacio } from '../componentes/Estados'
import { clasesBoton } from '../componentes/estilos'
import { FiltroEstadoTicket, type FiltroEstado } from '../funcionalidades/tickets/componentes/FiltroEstadoTicket'
import { ModalConfirmarCancelacion } from '../funcionalidades/tickets/componentes/ModalConfirmarCancelacion'
import { TarjetaTicket } from '../funcionalidades/tickets/componentes/TarjetaTicket'
import { useCancelacion } from '../funcionalidades/tickets/componentes/useCancelacion'
import { useMisTickets } from '../funcionalidades/tickets/hooks'
import { TicketStatusSchema } from '../funcionalidades/tickets/tipos'
import { mensajeDeError } from '../lib/errores'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §9.11 (MisTickets.png, ModalCancelar.png). FR-FE-TKT-003 y FR-FE-TKT-005.
// El filtro va en la URL (?estado=PAID) y se valida con zod como cualquier dato externo (FE-002)
export default function MisTickets() {
  const [parametros, setParametros] = useSearchParams()
  const filtro: FiltroEstado = TicketStatusSchema.safeParse(parametros.get('estado')).data ?? 'TODOS'
  const tickets = useMisTickets()
  const cancelacion = useCancelacion()

  function cambiarFiltro(valor: FiltroEstado) {
    setParametros(valor === 'TODOS' ? {} : { estado: valor }, { replace: true })
  }

  const visibles = (tickets.data ?? []).filter((ticket) => filtro === 'TODOS' || ticket.status === filtro)

  return (
    <Contenedor ancho="estrecho">
      <h1 className="mb-6 text-3xl font-bold">Mis tickets</h1>

      {tickets.isPending ? (
        <CargandoTarjetas cantidad={2} etiqueta="Cargando tickets…" />
      ) : tickets.isError ? (
        <EstadoError mensaje={mensajeDeError(tickets.error)} onReintentar={() => void tickets.refetch()} />
      ) : tickets.data.length === 0 ? (
        <EstadoVacio
          mensaje="Aún no tienes tickets"
          accion={
            <Link to={RUTAS.eventos} className={clasesBoton('primario')}>
              Ver cartelera
            </Link>
          }
        />
      ) : (
        <div className="flex flex-col gap-6">
          <FiltroEstadoTicket valor={filtro} onCambiar={cambiarFiltro} />
          {visibles.length === 0 ? (
            <EstadoVacio mensaje="No tienes tickets con este estado" />
          ) : (
            <ul className="flex flex-col gap-4">
              {visibles.map((ticket) => (
                <li key={ticket.ticketCode}>
                  <TarjetaTicket ticket={ticket} onCancelar={cancelacion.pedir} />
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

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
