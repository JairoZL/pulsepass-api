import type { ReactNode } from 'react'
import { IconoMusica } from '../../../componentes/Iconos'
import { Tarjeta } from '../../../componentes/Tarjeta'

// Marco común de Login y Registro (Login.png, Registro.png): logo, título y formulario
export function TarjetaAcceso({ titulo, children, ancho = 'max-w-md' }: { titulo: string; children: ReactNode; ancho?: 'max-w-md' | 'max-w-xl' }) {
  return (
    <div className="px-4 py-10">
      <Tarjeta className={`mx-auto flex w-full flex-col gap-6 px-6 py-8 sm:px-8 ${ancho}`}>
        <div className="flex flex-col items-center gap-4">
          <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-marca-600 text-white">
            <IconoMusica tamano={26} />
          </span>
          <h1 className="text-3xl font-bold">{titulo}</h1>
        </div>
        {children}
      </Tarjeta>
    </div>
  )
}
