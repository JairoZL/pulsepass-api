import { NoDisponible } from '../../../componentes/NoDisponible'
import { RUTAS } from '../../../lib/rutas'

// AC-FE-014: un ticket ajeno (403) o inexistente (404) se ve igual, sin revelar si existe
export function TicketNoDisponible() {
  return (
    <NoDisponible
      titulo="Ticket no disponible"
      mensaje="No encontramos este ticket en tu cuenta."
      volverA={RUTAS.misTickets}
      textoVolver="Ir a mis tickets"
    />
  )
}
