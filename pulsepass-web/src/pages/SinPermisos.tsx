import { PaginaCodigo } from './PaginaCodigo'

// /403: rol insuficiente (RutaPorRol) o 403 del servidor al cargar una página (PRD §9.19)
export default function SinPermisos() {
  return <PaginaCodigo codigo="403" mensaje="No tienes permisos para acceder a esta sección." />
}
