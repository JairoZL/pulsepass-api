import { Link } from 'react-router'
import { IconoFlechaDerecha } from './Iconos'

export function EnlaceVerTodos({ a, etiqueta }: { a: string; etiqueta: string }) {
  return (
    <Link
      to={a}
      aria-label={etiqueta}
      className="inline-flex items-center gap-1 rounded text-sm font-medium text-marca-600 hover:text-marca-700 focus-visible:outline-2 focus-visible:outline-marca-600 dark:text-marca-500"
    >
      Ver todos <IconoFlechaDerecha tamano={16} />
    </Link>
  )
}
