import { clsx } from 'clsx'
import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router'
import { clasesBoton } from '../componentes/Boton'
import { IconoCerrar, IconoMenu, IconoMusica } from '../componentes/Iconos'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { rolPrincipal } from '../funcionalidades/auth/roles'
import { RUTAS } from '../lib/rutas'
import { BotonTema } from './BotonTema'
import { enlacesPara } from './enlaces'
import { MenuUsuario } from './MenuUsuario'

const claseEnlace = ({ isActive }: { isActive: boolean }) =>
  clsx(
    'rounded-lg px-3 py-2 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-marca-600',
    isActive
      ? 'bg-marca-50 text-marca-700 dark:bg-slate-800 dark:text-marca-500'
      : 'text-slate-600 hover:text-slate-900 dark:text-slate-300 dark:hover:text-white',
  )

// PRD Frontend §9.2 y FR-FE-GEN-001: navbar adaptada al rol, con menú hamburguesa en móvil (< md)
export function Navbar() {
  const { sesion } = useAuth()
  const location = useLocation()
  const [menuAbierto, setMenuAbierto] = useState(false)

  const rol = sesion.estado === 'autenticado' ? rolPrincipal(sesion.usuario.roles) : null
  const enlaces = enlacesPara(rol)

  // Al cambiar de página el menú móvil se cierra solo
  useEffect(() => {
    setMenuAbierto(false)
  }, [location.pathname])

  return (
    <header
      className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur dark:border-slate-800 dark:bg-slate-950/95"
      onKeyDown={(evento) => {
        // Esc cierra el menú móvil desde cualquier elemento del encabezado (TEST-FE-GEN-004)
        if (evento.key === 'Escape') setMenuAbierto(false)
      }}
    >
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-4 sm:px-6">
        <Link
          to={RUTAS.inicio}
          className="flex items-center gap-2 rounded-lg focus-visible:outline-2 focus-visible:outline-marca-600"
        >
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-marca-600 text-white">
            <IconoMusica tamano={18} />
          </span>
          <span className="font-titulo text-xl font-bold">PulsePass</span>
        </Link>

        <nav aria-label="Principal" className="hidden flex-1 items-center gap-1 md:flex">
          {enlaces.map((enlace) => (
            <NavLink key={enlace.ruta} to={enlace.ruta} className={claseEnlace}>
              {enlace.etiqueta}
            </NavLink>
          ))}
        </nav>

        <div className="ml-auto hidden items-center gap-3 md:flex">
          <BotonTema />
          {sesion.estado === 'autenticado' ? (
            <MenuUsuario usuario={sesion.usuario} />
          ) : sesion.estado === 'anonimo' ? (
            <>
              <Link to={RUTAS.login} className={clasesBoton('fantasma')}>
                Ingresar
              </Link>
              <Link to={RUTAS.registro} className={clasesBoton('primario')}>
                Registrarse
              </Link>
            </>
          ) : null}
        </div>

        <button
          type="button"
          className="ml-auto rounded-lg p-2 focus-visible:outline-2 focus-visible:outline-marca-600 md:hidden"
          aria-expanded={menuAbierto}
          aria-controls="menu-movil"
          aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
          onClick={() => setMenuAbierto((abierto) => !abierto)}
        >
          {menuAbierto ? <IconoCerrar tamano={24} /> : <IconoMenu tamano={24} />}
        </button>
      </div>

      {menuAbierto ? (
        <div
          id="menu-movil"
          className="border-t border-slate-200 px-4 pb-4 md:hidden dark:border-slate-800"
        >
          <nav aria-label="Principal móvil" className="flex flex-col py-2">
            {enlaces.map((enlace) => (
              <NavLink
                key={enlace.ruta}
                to={enlace.ruta}
                className={claseEnlace}
                onClick={() => setMenuAbierto(false)}
              >
                {enlace.etiqueta}
              </NavLink>
            ))}
          </nav>
          <div className="flex flex-wrap items-center gap-3 border-t border-slate-200 pt-4 dark:border-slate-800">
            {sesion.estado === 'autenticado' ? (
              <MenuUsuario usuario={sesion.usuario} enLinea />
            ) : (
              <>
                <Link to={RUTAS.login} className={clasesBoton('secundario')}>
                  Ingresar
                </Link>
                <Link to={RUTAS.registro} className={clasesBoton('primario')}>
                  Registrarse
                </Link>
              </>
            )}
            <BotonTema />
          </div>
        </div>
      ) : null}
    </header>
  )
}
