import { Aviso } from '../../../componentes/Aviso'
import { Boton } from '../../../componentes/Boton'
import { CodigoTexto } from '../../../componentes/CodigoTexto'
import { Modal } from '../../../componentes/Modal'
import type { TicketResponse } from '../tipos'

interface PropsModalConfirmarCancelacion {
  ticket: TicketResponse | null // null = cerrado
  cargando: boolean
  error?: string
  onConfirmar: () => void
  onCerrar: () => void
}

// PRD Frontend §9.11 (ModalCancelar.png). Un 409 se muestra aquí mismo sin cerrar el modal
export function ModalConfirmarCancelacion({ ticket, cargando, error, onConfirmar, onCerrar }: PropsModalConfirmarCancelacion) {
  return (
    <Modal
      abierto={ticket !== null}
      titulo="Cancelar ticket"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Volver
          </Boton>
          <Boton variante="peligro" onClick={onConfirmar} cargando={cargando}>
            Sí, cancelar
          </Boton>
        </>
      }
    >
      {ticket ? (
        <p className="text-slate-700 dark:text-slate-300">
          ¿Seguro que quieres cancelar el ticket <CodigoTexto className="font-semibold">{ticket.ticketCode}</CodigoTexto>{' '}
          de {ticket.eventName}? Esta acción no se puede deshacer.
        </p>
      ) : null}
      {error ? (
        <Aviso tono="error" className="mt-4">
          {error}
        </Aviso>
      ) : null}
    </Modal>
  )
}
