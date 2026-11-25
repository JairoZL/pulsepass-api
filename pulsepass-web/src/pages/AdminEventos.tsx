import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { Badge } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { CodigoTexto } from '../componentes/CodigoTexto'
import { Contenedor } from '../componentes/Contenedor'
import { EstadoVacio } from '../componentes/Estados'
import { clasesBoton } from '../componentes/estilos'
import { IconoMas } from '../componentes/Iconos'
import { Tarjeta } from '../componentes/Tarjeta'
import { leerEventosCreados } from '../funcionalidades/eventos/creadosSesion'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §9.16 (AdminEventos.png). FR-FE-ADM-001..003
export default function AdminEventos() {
  const navigate = useNavigate()
  const [codigo, setCodigo] = useState('')
  const creados = leerEventosCreados()

  function abrir(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const limpio = codigo.trim().toUpperCase()
    if (limpio) navigate(RUTAS.adminEvento(limpio))
  }

  return (
    <Contenedor ancho="estrecho">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-bold">Administración de eventos</h1>
        <Link to={RUTAS.adminCrearEvento} className={clasesBoton('primario')}>
          <IconoMas tamano={18} />
          Nuevo evento
        </Link>
      </div>

      <form className="mb-8 flex items-end gap-2" onSubmit={abrir}>
        <Campo
          etiqueta="Buscar evento por código"
          placeholder="CMF-2026"
          className="font-mono"
          envolturaClassName="flex-1"
          value={codigo}
          onChange={(evento) => setCodigo(evento.target.value)}
        />
        <Boton type="submit">Abrir</Boton>
      </form>

      <section aria-labelledby="titulo-creados">
        <h2 id="titulo-creados" className="mb-4 text-lg font-semibold">
          Creados en esta sesión
        </h2>
        {creados.length === 0 ? (
          <EstadoVacio mensaje="Aún no has creado eventos en esta sesión." />
        ) : (
          <ul className="flex flex-col gap-3">
            {creados.map((evento) => (
              <li key={evento.eventCode}>
                <Tarjeta className="flex flex-wrap items-center justify-between gap-3 p-4">
                  <span>
                    <CodigoTexto className="font-semibold">{evento.eventCode}</CodigoTexto> · {evento.name}
                  </span>
                  <span className="flex items-center gap-3">
                    <Badge tono={evento.status} />
                    <Link
                      to={RUTAS.adminEvento(evento.eventCode)}
                      className={clasesBoton('secundario')}
                      aria-label={`Gestionar ${evento.eventCode}`}
                    >
                      Gestionar
                    </Link>
                  </span>
                </Tarjeta>
              </li>
            ))}
          </ul>
        )}
        <p className="mt-4 text-xs text-slate-500 dark:text-slate-400">
          Nota: la API no ofrece un listado completo de eventos. Esta lista se borra al cerrar la pestaña o la sesión.
        </p>
      </section>
    </Contenedor>
  )
}
