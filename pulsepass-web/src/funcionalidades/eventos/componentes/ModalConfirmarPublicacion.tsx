import { Boton } from '../../../componentes/Boton'
import { Modal } from '../../../componentes/Modal'

interface PropsModalConfirmarPublicacion {
  abierto: boolean
  nombreEvento: string
  cargando: boolean
  onConfirmar: () => void
  onCerrar: () => void
}

// §9.18: publicar pide confirmación porque el evento pasa a ser visible para todo el público
export function ModalConfirmarPublicacion({ abierto, nombreEvento, cargando, onConfirmar, onCerrar }: PropsModalConfirmarPublicacion) {
  return (
    <Modal
      abierto={abierto}
      titulo="Publicar evento"
      onCerrar={onCerrar}
      pie={
        <>
          <Boton variante="secundario" onClick={onCerrar}>
            Volver
          </Boton>
          <Boton onClick={onConfirmar} cargando={cargando}>
            Sí, publicar
          </Boton>
        </>
      }
    >
      <p className="text-slate-700 dark:text-slate-300">
        Al publicar «{nombreEvento}» aparecerá en la cartelera y se podrán comprar entradas.
      </p>
    </Modal>
  )
}
