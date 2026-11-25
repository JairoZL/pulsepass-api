import { useEffect, useId, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { IconoCerrar } from './Iconos'

// PRD Frontend §9.1: Modal accesible. Atrapa el foco (Tab no sale del modal), se cierra con Esc
// y al cerrarse devuelve el foco al botón que lo abrió.
interface PropsModal {
  abierto: boolean
  titulo: string
  onCerrar: () => void
  children: ReactNode
  pie?: ReactNode
}

const ENFOCABLES =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'

export function Modal({ abierto, titulo, onCerrar, children, pie }: PropsModal) {
  const idTitulo = useId()
  const panel = useRef<HTMLDivElement>(null)
  // Se guarda en una ref para que el efecto no se reinicie (y robe el foco) en cada render del padre
  const onCerrarRef = useRef(onCerrar)
  useEffect(() => {
    onCerrarRef.current = onCerrar
  })

  useEffect(() => {
    if (!abierto) return
    const anterior = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const primero = panel.current?.querySelector<HTMLElement>(ENFOCABLES)
    primero?.focus()

    function alPresionarTecla(evento: KeyboardEvent) {
      if (evento.key === 'Escape') {
        evento.preventDefault()
        onCerrarRef.current()
        return
      }
      if (evento.key !== 'Tab' || !panel.current) return
      const elementos = Array.from(panel.current.querySelectorAll<HTMLElement>(ENFOCABLES))
      const inicio = elementos[0]
      const fin = elementos[elementos.length - 1]
      if (!inicio || !fin) return
      if (evento.shiftKey && document.activeElement === inicio) {
        evento.preventDefault()
        fin.focus()
      } else if (!evento.shiftKey && document.activeElement === fin) {
        evento.preventDefault()
        inicio.focus()
      }
    }

    document.addEventListener('keydown', alPresionarTecla)
    return () => {
      document.removeEventListener('keydown', alPresionarTecla)
      anterior?.focus()
    }
  }, [abierto])

  if (!abierto) return null

  return createPortal(
    <div className="fixed inset-0 z-40 flex items-end justify-center bg-slate-900/50 p-4 sm:items-center">
      <div
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitulo}
        className="w-full max-w-md rounded-xl bg-white shadow-xl dark:bg-slate-900"
      >
        <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4 dark:border-slate-800">
          <h2 id={idTitulo} className="text-lg font-semibold">
            {titulo}
          </h2>
          <button
            type="button"
            onClick={onCerrar}
            aria-label="Cerrar"
            className="rounded-md p-1 text-slate-500 hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-marca-600 dark:hover:bg-slate-800"
          >
            <IconoCerrar />
          </button>
        </div>
        <div className="px-5 py-4">{children}</div>
        {pie ? <div className="flex justify-end gap-3 px-5 pb-5">{pie}</div> : null}
      </div>
    </div>,
    document.body,
  )
}
