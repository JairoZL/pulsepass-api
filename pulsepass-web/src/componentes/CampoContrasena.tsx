import { useState, type ComponentProps } from 'react'
import { Campo } from './Campo'
import { IconoOjo, IconoOjoTachado } from './Iconos'

type PropsCampoContrasena = Omit<ComponentProps<typeof Campo>, 'type' | 'accesorio'>

// Campo de contraseña con botón para mostrarla u ocultarla (diseños Login y Registro)
export function CampoContrasena(props: PropsCampoContrasena) {
  const [visible, setVisible] = useState(false)
  return (
    <Campo
      {...props}
      type={visible ? 'text' : 'password'}
      accesorio={
        <button
          type="button"
          onClick={() => setVisible((valor) => !valor)}
          aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          aria-pressed={visible}
          className="rounded-md p-2 text-slate-500 hover:text-slate-800 focus-visible:outline-2 focus-visible:outline-marca-600 dark:hover:text-slate-200"
        >
          {visible ? <IconoOjoTachado tamano={18} /> : <IconoOjo tamano={18} />}
        </button>
      }
    />
  )
}
