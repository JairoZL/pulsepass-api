import { useNavigate } from 'react-router'
import { RUTAS } from '../../lib/rutas'
import { useAuth } from './contexto'

// FR-FE-AUTH-006: salir = ir al inicio y LUEGO cerrar la sesión.
// El orden importa: si se cierra primero estando en una ruta protegida (/cuenta), RutaProtegida
// ve una sesión anónima y redirige a /login antes de que la navegación al inicio termine.
// flushSync hace que el cambio de ruta se pinte de inmediato, antes de cerrar la sesión.
export function useSalir(): () => Promise<void> {
  const { cerrarSesion } = useAuth()
  const navigate = useNavigate()
  return async () => {
    await navigate(RUTAS.inicio, { flushSync: true })
    cerrarSesion()
  }
}
