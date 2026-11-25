import { Boton } from '../../../componentes/Boton'
import { useAvisos } from '../../../componentes/avisosContexto'
import { IconoCopiar } from '../../../componentes/Iconos'

// Copia el código del ticket (el QR está fuera de alcance, §9.12)
export function BotonCopiar({ texto }: { texto: string }) {
  const { mostrarAviso } = useAvisos()
  async function copiar() {
    try {
      await navigator.clipboard.writeText(texto)
      mostrarAviso('exito', 'Código copiado.')
    } catch {
      mostrarAviso('error', 'No se pudo copiar. Selecciona el código y cópialo a mano.')
    }
  }
  return (
    <Boton variante="fantasma" onClick={() => void copiar()} aria-label={`Copiar código ${texto}`}>
      <IconoCopiar tamano={16} />
      Copiar
    </Boton>
  )
}
