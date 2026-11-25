import { Link } from 'react-router'
import { IconoFlechaIzquierda } from './Iconos'

// "← Volver a la cartelera", "← Artistas"... (todas las páginas de detalle)
export function EnlaceVolver({ a, texto }: { a: string; texto: string }) {
  return (
    <Link
      to={a}
      className="mb-6 inline-flex items-center gap-2 rounded text-sm font-medium text-marca-600 hover:text-marca-700 focus-visible:outline-2 focus-visible:outline-marca-600 dark:text-marca-500"
    >
      <IconoFlechaIzquierda tamano={16} />
      {texto}
    </Link>
  )
}
