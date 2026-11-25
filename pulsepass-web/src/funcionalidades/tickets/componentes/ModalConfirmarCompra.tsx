import { Aviso } from '../../../componentes/Aviso'
import { Boton } from '../../../componentes/Boton'
import { Modal } from '../../../componentes/Modal'
import type { TicketType } from '../tipos'

interface PropsModalConfirmarCompra {
  abierto: boolean
  nombreEvento: string
  tipo: TicketType
  email: string
  cargando: boolean
  error?: string
  onConfirmar: () => void
  onCerrar: () => void
}

// PRD Frontend §9.5 y DES-10 (ModalCompra.png): resumen + error 409 DENTRO del modal, sin cerrarlo
export function ModalConfirmarCompra({
  abierto,
  nombreEvento,
  tipo,
  email,
  cargando,
  error,
  onConfirmar,
  onCerrar,
}: PropsModalConfirmarCompra) {
  const filas: [string, string][] = [
    ['Evento', nombreEvento],
    ['Tipo', tipo],
    ['Cuenta', email],
  ]
  return (
    <Modal
      abierto={abierto}
      titulo="Confirmar compra"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Cancelar
          </Boton>
          <Boton onClick={onConfirmar} cargando={cargando}>
            Confirmar
          </Boton>
        </>
      }
    >
      <dl className="divide-y divide-slate-200 text-sm dark:divide-slate-800">
        {filas.map(([etiqueta, valor]) => (
          <div key={etiqueta} className="flex justify-between gap-4 py-3">
            <dt className="text-slate-600 dark:text-slate-400">{etiqueta}</dt>
            <dd className="text-right font-medium">{valor}</dd>
          </div>
        ))}
      </dl>
      {error ? (
        <Aviso tono="error" className="mt-4">
          {error}
        </Aviso>
      ) : null}
    </Modal>
  )
}
