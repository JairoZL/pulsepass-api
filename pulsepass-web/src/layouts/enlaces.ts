import type { Rol } from '../funcionalidades/auth/tipos'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §8.4 (Navbar por rol). Se calcula desde RUTAS y el rol principal: nunca strings sueltos
export interface EnlaceNavegacion {
  etiqueta: string
  ruta: string
}

const PUBLICOS: EnlaceNavegacion[] = [
  { etiqueta: 'Eventos', ruta: RUTAS.eventos },
  { etiqueta: 'Artistas', ruta: RUTAS.artistas },
  { etiqueta: 'Sedes', ruta: RUTAS.sedes },
]

const POR_ROL: Record<Rol, EnlaceNavegacion[]> = {
  USER: [{ etiqueta: 'Mis tickets', ruta: RUTAS.misTickets }],
  STAFF: [
    { etiqueta: 'Validar ingreso', ruta: RUTAS.staffValidacion },
    { etiqueta: 'Tickets pagados', ruta: RUTAS.staffTicketsPagados() },
  ],
  ADMIN: [
    { etiqueta: 'Validar ingreso', ruta: RUTAS.staffValidacion },
    { etiqueta: 'Tickets pagados', ruta: RUTAS.staffTicketsPagados() },
    { etiqueta: 'Administrar eventos', ruta: RUTAS.adminEventos },
  ],
}

export function enlacesPara(rol: Rol | null): EnlaceNavegacion[] {
  return rol ? [...PUBLICOS, ...POR_ROL[rol]] : PUBLICOS
}
