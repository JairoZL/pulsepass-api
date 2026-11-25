import { useState } from 'react'
import { IconoLuna, IconoSol } from '../componentes/Iconos'
import { aplicarTema, temaActual, type Tema } from '../lib/tema'

// FR-FE-GEN-004: alterna entre modo claro y oscuro
export function BotonTema() {
  const [tema, setTema] = useState<Tema>(temaActual)

  function alternar() {
    const siguiente: Tema = tema === 'oscuro' ? 'claro' : 'oscuro'
    aplicarTema(siguiente)
    setTema(siguiente)
  }

  return (
    <button
      type="button"
      onClick={alternar}
      aria-label={tema === 'oscuro' ? 'Activar modo claro' : 'Activar modo oscuro'}
      className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-marca-600 dark:text-slate-300 dark:hover:bg-slate-800"
    >
      {tema === 'oscuro' ? <IconoSol /> : <IconoLuna />}
    </button>
  )
}
