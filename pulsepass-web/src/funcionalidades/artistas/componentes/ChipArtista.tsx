import { Link } from 'react-router'
import { Avatar } from '../../../componentes/Avatar'
import { RUTAS } from '../../../lib/rutas'
import type { ArtistResponse } from '../tipos'

// Avatar con iniciales + nombre, enlazado al perfil (Inicio y detalle de evento)
export function ChipArtista({ artista }: { artista: Pick<ArtistResponse, 'id' | 'stageName'> }) {
  return (
    <Link
      to={RUTAS.artista(artista.id)}
      className="flex items-center gap-3 rounded-full pr-3 hover:text-marca-700 focus-visible:outline-2 focus-visible:outline-marca-600 dark:hover:text-marca-500"
    >
      <Avatar nombre={artista.stageName} />
      <span className="font-medium">{artista.stageName}</span>
    </Link>
  )
}
