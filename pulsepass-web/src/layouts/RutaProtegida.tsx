import { Navigate, Outlet, useLocation } from 'react-router'
import { Cargando } from '../componentes/Estados'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §8.2 y FE-004: exige sesión. Sin sesión -> /login recordando de dónde venía (state.desde).
// Es solo experiencia de usuario: la seguridad real la aplica el servidor en cada endpoint.
export default function RutaProtegida() {
  const { sesion } = useAuth()
  const location = useLocation()

  if (sesion.estado === 'cargando') return <Cargando etiqueta="Verificando sesión…" />
  if (sesion.estado === 'anonimo') {
    return (
      <Navigate
        to={RUTAS.login}
        replace
        state={{ desde: `${location.pathname}${location.search}`, motivo: sesion.motivo }}
      />
    )
  }
  return <Outlet />
}
