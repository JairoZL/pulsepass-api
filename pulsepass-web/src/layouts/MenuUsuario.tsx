import { clsx } from 'clsx'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { Avatar } from '../componentes/Avatar'
import { IconoChevronAbajo } from '../componentes/Iconos'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { rolPrincipal } from '../funcionalidades/auth/roles'
import type { UsuarioAutenticado } from '../funcionalidades/auth/tipos'
import { RUTAS } from '../lib/rutas'

interface PropsMenuUsuario {
  usuario: UsuarioAutenticado
  enLinea?: boolean // en el menú móvil las opciones se muestran directamente, sin desplegable
}

const CLASE_OPCION =
  'block w-full rounded-md px-3 py-2 text-left text-sm hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-marca-600 dark:hover:bg-slate-800'

// PRD Frontend §9.2: con sesión, [ Andrea ▾ ] -> Mi cuenta · Mis tickets · Salir
export function MenuUsuario({ usuario, enLinea = false }: PropsMenuUsuario) {
  const { cerrarSesion } = useAuth()
  const navigate = useNavigate()
  const [abierto, setAbierto] = useState(false)
  const contenedor = useRef<HTMLDivElement>(null)
  const esUser = rolPrincipal(usuario.roles) === 'USER'

  // Se cierra con clic afuera
  useEffect(() => {
    if (!abierto) return
    function alHacerClic(evento: MouseEvent) {
      if (evento.target instanceof Node && !contenedor.current?.contains(evento.target)) setAbierto(false)
    }
    document.addEventListener('mousedown', alHacerClic)
    return () => document.removeEventListener('mousedown', alHacerClic)
  }, [abierto])

  // FR-FE-AUTH-006: salir borra token y caché, y vuelve al inicio
  function salir() {
    cerrarSesion()
    navigate(RUTAS.inicio)
  }

  const opciones = (
    <>
      <Link to={RUTAS.cuenta} className={CLASE_OPCION} onClick={() => setAbierto(false)}>
        Mi cuenta
      </Link>
      {esUser ? (
        <Link to={RUTAS.misTickets} className={CLASE_OPCION} onClick={() => setAbierto(false)}>
          Mis tickets
        </Link>
      ) : null}
      <button type="button" className={clsx(CLASE_OPCION, 'text-red-600 dark:text-red-400')} onClick={salir}>
        Salir
      </button>
    </>
  )

  if (enLinea) {
    return <div className="flex w-full flex-col">{opciones}</div>
  }

  return (
    <div
      ref={contenedor}
      className="relative"
      onKeyDown={(evento) => {
        if (evento.key === 'Escape') setAbierto(false)
      }}
    >
      <button
        type="button"
        aria-expanded={abierto}
        aria-controls="menu-usuario"
        onClick={() => setAbierto((valor) => !valor)}
        className="flex items-center gap-2 rounded-full border border-slate-200 py-1 pr-3 pl-1 text-sm font-medium hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-marca-600 dark:border-slate-700 dark:hover:bg-slate-800"
      >
        <Avatar nombre={usuario.username} tamano="sm" />
        <span>{usuario.username}</span>
        <IconoChevronAbajo tamano={16} />
      </button>
      {abierto ? (
        <div
          id="menu-usuario"
          className="absolute right-0 mt-2 w-48 rounded-lg border border-slate-200 bg-white p-1 shadow-lg dark:border-slate-700 dark:bg-slate-900"
        >
          {opciones}
        </div>
      ) : null}
    </div>
  )
}
