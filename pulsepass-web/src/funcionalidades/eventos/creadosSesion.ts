import { z } from 'zod'
import { EventStatusSchema, type EventResponse, type EventStatus } from './tipos'

// PRD Frontend §9.16 y GAP-01: la API no tiene "listar todos los eventos", así que ADMIN ve los que
// creó en esta sesión. Se guardan en sessionStorage: se pierden al cerrar la pestaña o la sesión
// (limitación documentada en el README). Lo leído se valida con zod (FE-002).
const CLAVE = 'pulsepass.eventosCreados'

const EventoCreadoSchema = z.object({ eventCode: z.string(), name: z.string(), status: EventStatusSchema })
export type EventoCreado = z.infer<typeof EventoCreadoSchema>

export function leerEventosCreados(): EventoCreado[] {
  try {
    const resultado = z.array(EventoCreadoSchema).safeParse(JSON.parse(sessionStorage.getItem(CLAVE) ?? '[]'))
    return resultado.success ? resultado.data : []
  } catch {
    return []
  }
}

function guardar(lista: EventoCreado[]) {
  sessionStorage.setItem(CLAVE, JSON.stringify(lista))
}

export function agregarEventoCreado(evento: Pick<EventResponse, 'eventCode' | 'name' | 'status'>) {
  const resto = leerEventosCreados().filter((e) => e.eventCode !== evento.eventCode)
  guardar([{ eventCode: evento.eventCode, name: evento.name, status: evento.status }, ...resto])
}

// Al publicar, el estado de la lista se actualiza (si el evento está en ella)
export function actualizarEstadoCreado(eventCode: string, status: EventStatus) {
  guardar(leerEventosCreados().map((e) => (e.eventCode === eventCode ? { ...e, status } : e)))
}

export function borrarEventosCreados() {
  sessionStorage.removeItem(CLAVE)
}
