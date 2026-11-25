import { Link } from 'react-router'
import { clasesBoton } from '../componentes/Boton'
import { Contenedor } from '../componentes/Contenedor'
import { Tarjeta } from '../componentes/Tarjeta'
import { RUTAS } from '../lib/rutas'

// Diseño de imagenes/Errores.png: número grande + mensaje + "Volver al inicio"
export function PaginaCodigo({ codigo, mensaje }: { codigo: string; mensaje: string }) {
  return (
    <Contenedor ancho="estrecho">
      <Tarjeta className="flex flex-col items-center gap-6 px-6 py-12 text-center">
        <p aria-hidden="true" className="font-titulo text-6xl font-bold text-marca-700 dark:text-marca-500">
          {codigo}
        </p>
        <h1 className="font-cuerpo text-base font-normal">{mensaje}</h1>
        <Link to={RUTAS.inicio} className={clasesBoton('primario')}>
          Volver al inicio
        </Link>
      </Tarjeta>
    </Contenedor>
  )
}
