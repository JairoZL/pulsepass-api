import { PaginaCodigo } from './PaginaCodigo'

// Ruta inexistente (*) o recurso que no existe (PRD §9.19, TEST-FE-GEN-001)
export default function NoEncontrada() {
  return <PaginaCodigo codigo="404" mensaje="No encontramos esta página." />
}
