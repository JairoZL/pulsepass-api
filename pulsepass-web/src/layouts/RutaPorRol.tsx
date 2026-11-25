import { Navigate, Outlet, useLocation } from 'react-router'
import { Cargando } from '../componentes/Estados'
import { useAuth } from '../funcionalidades/auth/contexto'
import type { Rol } from '../funcionalidades/auth/tipos'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §12.3: anónimo -> /login; rol insuficiente -> /403; si no, muestra la página.
// Un 403 del servidor siempre prevalece sobre esta guarda.
export default function RutaPorRol({ roles }: { roles: Rol[] }) {
  const { sesion } = useAuth()
  const location = useLocation()

  if (sesion.estado === 'cargando') return <Cargando etiqueta="Verificando sesión…" />
  if (sesion.estado === 'anonimo') {
    return <Navigate to={RUTAS.login} replace state={{ desde: location.pathname, motivo: sesion.motivo }} />
  }
  if (!roles.some((rol) => sesion.usuario.roles.includes(rol))) {
    return <Navigate to={RUTAS.sinPermisos} replace />
  }
  return <Outlet />
}
