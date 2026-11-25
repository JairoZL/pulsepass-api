import { useState } from 'react'
import { useAvisos } from '../../../componentes/avisosContexto'
import { esApiError, mensajeDeError } from '../../../lib/errores'
import { useCancelarTicket } from '../hooks'
import type { TicketResponse } from '../tipos'

// Estado compartido por Mis tickets y Detalle de ticket (§9.11 y §9.12): qué ticket se está
// por cancelar, la mutación y qué error mostrar dentro del modal.
export function useCancelacion() {
  const [pendiente, setPendiente] = useState<TicketResponse | null>(null)
  const cancelar = useCancelarTicket()
  const { mostrarAviso } = useAvisos()

  function pedir(ticket: TicketResponse) {
    cancelar.reset()
    setPendiente(ticket)
  }

  function cerrar() {
    setPendiente(null)
  }

  function confirmar() {
    if (!pendiente) return
    cancelar.mutate(pendiente.ticketCode, {
      onSuccess: () => {
        setPendiente(null)
        mostrarAviso('exito', 'Ticket cancelado.')
      },
      onError: (error) => {
        // 409 (usado o no cancelable) queda en el modal; 403/404 cierran el modal con un aviso
        if (esApiError(error, 403, 404)) {
          setPendiente(null)
          mostrarAviso('error', 'No se pudo cancelar: el ticket no está disponible.')
        }
      },
    })
  }

  // 401 ya lo maneja lib/http (cierra la sesión); 403/404 ya cerraron el modal
  const errorModal =
    cancelar.isError && !esApiError(cancelar.error, 401, 403, 404) ? mensajeDeError(cancelar.error) : undefined

  return { pendiente, pedir, cerrar, confirmar, cargando: cancelar.isPending, errorModal }
}
