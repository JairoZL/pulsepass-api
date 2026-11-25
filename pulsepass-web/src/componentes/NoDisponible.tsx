import { Link } from 'react-router'
import { Contenedor } from './Contenedor'
import { clasesBoton } from './estilos'
import { IconoBandeja } from './Iconos'
import { Tarjeta } from './Tarjeta'

// "Evento no disponible" / "Ticket no disponible": se usa para 404 y 403 sin revelar si el
// recurso existe (GAP-02, AC-FE-003, AC-FE-014). Nunca muestra datos del recurso.
interface PropsNoDisponible {
  titulo: string
  mensaje: string
  volverA: string
  textoVolver: string
}

export function NoDisponible({ titulo, mensaje, volverA, textoVolver }: PropsNoDisponible) {
  return (
    <Contenedor ancho="estrecho">
      <Tarjeta className="flex flex-col items-center gap-4 px-6 py-12 text-center">
        <IconoBandeja tamano={36} className="text-slate-500" />
        <h1 className="text-2xl font-bold">{titulo}</h1>
        <p className="text-slate-600 dark:text-slate-400">{mensaje}</p>
        <Link to={volverA} className={clasesBoton('primario')}>
          {textoVolver}
        </Link>
      </Tarjeta>
    </Contenedor>
  )
}
