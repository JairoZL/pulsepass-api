import { createContext, use, useCallback, useMemo, useRef, useState, type ReactNode } from 'react'
import { Aviso, type TonoAviso } from './Aviso'

// FR-FE-GEN-003: avisos flotantes (toast) tras las mutaciones.
// Éxito e info desaparecen a los 4 s; error y advertencia quedan hasta que se cierran (§9.20).
interface AvisoFlotante {
  id: number
  tono: TonoAviso
  mensaje: string
}

interface ValorAvisos {
  mostrarAviso: (tono: TonoAviso, mensaje: string) => void
}

const AvisosContext = createContext<ValorAvisos | null>(null)

const DURACION_MS = 4000

export function AvisosProvider({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<AvisoFlotante[]>([])
  const siguienteId = useRef(1)

  const cerrar = useCallback((id: number) => {
    setAvisos((actuales) => actuales.filter((aviso) => aviso.id !== id))
  }, [])

  const mostrarAviso = useCallback(
    (tono: TonoAviso, mensaje: string) => {
      const id = siguienteId.current++
      setAvisos((actuales) => [...actuales, { id, tono, mensaje }])
      if (tono === 'exito' || tono === 'info') {
        window.setTimeout(() => cerrar(id), DURACION_MS)
      }
    },
    [cerrar],
  )

  const valor = useMemo(() => ({ mostrarAviso }), [mostrarAviso])

  return (
    <AvisosContext value={valor}>
      {children}
      {/* Región siempre presente para que los lectores de pantalla anuncien los avisos nuevos */}
      <div
        aria-live="polite"
        className="pointer-events-none fixed inset-x-0 bottom-4 z-50 flex flex-col items-center gap-2 px-4 sm:items-end sm:pr-6"
      >
        {avisos.map((aviso) => (
          <Aviso
            key={aviso.id}
            tono={aviso.tono}
            onCerrar={() => cerrar(aviso.id)}
            className="pointer-events-auto w-full max-w-sm shadow-lg"
          >
            {aviso.mensaje}
          </Aviso>
        ))}
      </div>
    </AvisosContext>
  )
}

export function useAvisos(): ValorAvisos {
  const valor = use(AvisosContext)
  if (!valor) throw new Error('useAvisos debe usarse dentro de <AvisosProvider>')
  return valor
}
