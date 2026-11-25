import { Navigate, Outlet, useLocation } from 'react-router'
import { Cargando } from '../componentes/Estados'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { rutaDeRetorno } from '../lib/navegacion'

// /login y /registro: si ya hay sesión no tiene sentido mostrarlos (PRD Frontend §9.8).
// Redirige a la ruta de origen (state.desde) para que, justo después del login, este componente
// y la página de login lleven a la persona al mismo lugar.
export default function SoloAnonimo() {
  const { sesion } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <Cargando etiqueta="Verificando sesión…" />
  if (sesion.estado === 'autenticado') return <Navigate to={rutaDeRetorno(location.state)} replace />
  return <Outlet />
}
