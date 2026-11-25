import type { EventSummaryResponse } from '../tipos'
import { TarjetaEvento } from './TarjetaEvento'

// PRD Frontend §9.4: grid-cols-1 sm:grid-cols-2 lg:grid-cols-3
export function GrillaEventos({ eventos }: { eventos: EventSummaryResponse[] }) {
  return (
    <ul className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
      {eventos.map((evento) => (
        <li key={evento.eventCode} className="flex">
          <TarjetaEvento evento={evento} />
        </li>
      ))}
    </ul>
  )
}
