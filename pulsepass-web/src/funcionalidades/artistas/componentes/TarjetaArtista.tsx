import { Link } from 'react-router'
import { Avatar } from '../../../componentes/Avatar'
import { RUTAS } from '../../../lib/rutas'
import type { ArtistResponse } from '../tipos'

export function TarjetaArtista({ artista }: { artista: ArtistResponse }) {
  return (
    <Link
      to={RUTAS.artista(artista.id)}
      className="flex w-full flex-col items-center gap-3 rounded-xl border border-slate-200 bg-white p-6 text-center shadow-sm transition-shadow hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 dark:border-slate-800 dark:bg-slate-900"
    >
      <Avatar nombre={artista.stageName} tamano="lg" />
      <h3 className="text-lg font-semibold">{artista.stageName}</h3>
      <p className="text-sm text-slate-600 dark:text-slate-400">
        {artista.genre} · {artista.country}
      </p>
    </Link>
  )
}
