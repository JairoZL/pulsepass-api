import { createContext, use } from 'react'
import type { TonoAviso } from './Aviso'

export interface ValorAvisos {
  mostrarAviso: (tono: TonoAviso, mensaje: string) => void
}

export const AvisosContext = createContext<ValorAvisos | null>(null)

// FR-FE-GEN-003: avisos flotantes. Uso: const { mostrarAviso } = useAvisos()
export function useAvisos(): ValorAvisos {
  const valor = use(AvisosContext)
  if (!valor) throw new Error('useAvisos debe usarse dentro de <AvisosProvider>')
  return valor
}
