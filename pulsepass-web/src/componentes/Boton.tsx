import type { ComponentProps } from 'react'
import { clasesBoton, type VarianteBoton } from './estilos'

interface PropsBoton extends ComponentProps<'button'> {
  variante?: VarianteBoton
  cargando?: boolean
}

// PRD Frontend §9.1 y DES-09. cargando = deshabilitado + "Cargando…": evita el doble envío (§9.8)
export function Boton({
  variante = 'primario',
  cargando = false,
  disabled,
  type = 'button',
  className,
  children,
  ...props
}: PropsBoton) {
  return (
    <button
      type={type}
      disabled={disabled || cargando}
      aria-busy={cargando || undefined}
      className={clasesBoton(variante, className)}
      {...props}
    >
      {cargando ? 'Cargando…' : children}
    </button>
  )
}
