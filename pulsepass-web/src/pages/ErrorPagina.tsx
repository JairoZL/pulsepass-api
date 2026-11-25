import { isRouteErrorResponse, Link, useRouteError } from 'react-router'
import { Boton } from '../componentes/Boton'
import { clasesBoton } from '../componentes/estilos'
import { Contenedor } from '../componentes/Contenedor'
import { Tarjeta } from '../componentes/Tarjeta'
import { esApiError } from '../lib/errores'
import { MENSAJES_GENERICOS } from '../lib/http'
import { RUTAS } from '../lib/rutas'
import NoEncontrada from './NoEncontrada'
import SinPermisos from './SinPermisos'

// errorElement de las rutas (PRD §9.19, TEST-FE-GEN-002). Nunca muestra el stack ni el mensaje
// técnico del error: solo textos seguros. Un 403/404 al cargar la página usa sus pantallas propias.
export default function ErrorPagina() {
  const error = useRouteError()

  if ((isRouteErrorResponse(error) && error.status === 404) || esApiError(error, 404)) {
    return <NoEncontrada />
  }
  if ((isRouteErrorResponse(error) && error.status === 403) || esApiError(error, 403)) {
    return <SinPermisos />
  }

  return (
    <Contenedor>
      <Tarjeta className="flex flex-col gap-4 px-8 py-8">
        <h1 className="text-2xl font-bold">¡Ups! Algo salió mal.</h1>
        <p className="text-slate-600 dark:text-slate-400">
          {esApiError(error, 0) ? MENSAJES_GENERICOS.red : MENSAJES_GENERICOS.inesperado}
        </p>
        <div className="flex flex-wrap gap-3">
          <Boton onClick={() => window.location.reload()}>Reintentar</Boton>
          <Link to={RUTAS.inicio} className={clasesBoton('secundario')}>
            Volver al inicio
          </Link>
        </div>
      </Tarjeta>
    </Contenedor>
  )
}
