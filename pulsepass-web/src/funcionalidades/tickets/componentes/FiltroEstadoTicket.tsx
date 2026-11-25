import { clsx } from 'clsx'
import type { TicketStatus } from '../tipos'

export type FiltroEstado = TicketStatus | 'TODOS'

const OPCIONES: { valor: FiltroEstado; etiqueta: string }[] = [
  { valor: 'TODOS', etiqueta: 'Todos' },
  { valor: 'PAID', etiqueta: 'Pagados' },
  { valor: 'USED', etiqueta: 'Usados' },
  { valor: 'CANCELLED', etiqueta: 'Cancelados' },
]

// Chips de filtro (MisTickets.png). Son botones con aria-pressed: el lector de pantalla dice cuál está activo
export function FiltroEstadoTicket({ valor, onCambiar }: { valor: FiltroEstado; onCambiar: (valor: FiltroEstado) => void }) {
  return (
    <div role="group" aria-label="Filtrar por estado" className="flex flex-wrap gap-2">
      {OPCIONES.map((opcion) => (
        <button
          key={opcion.valor}
          type="button"
          aria-pressed={valor === opcion.valor}
          onClick={() => onCambiar(opcion.valor)}
          className={clsx(
            'rounded-full border px-4 py-1.5 text-sm font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600',
            valor === opcion.valor
              ? 'border-marca-600 bg-marca-600 text-white'
              : 'border-slate-300 text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800',
          )}
        >
          {opcion.etiqueta}
        </button>
      ))}
    </div>
  )
}
