import { Link } from 'react-router'
import { Contenedor } from '../componentes/Contenedor'
import { EnlaceVerTodos } from '../componentes/EnlaceVerTodos'
import { CargandoTarjetas, EstadoError, EstadoVacio } from '../componentes/Estados'
import { clasesBoton } from '../componentes/estilos'
import { ChipArtista } from '../funcionalidades/artistas/componentes/ChipArtista'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { useAuth } from '../funcionalidades/auth/contexto'
import { GrillaEventos } from '../funcionalidades/eventos/componentes/GrillaEventos'
import { useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { mensajeDeError } from '../lib/errores'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §9.3 (Main.png). No es lazy (FE-012): es la primera página que se ve
export default function Inicio() {
  const { sesion } = useAuth()
  const eventos = useEventosPublicados()
  const artistas = useArtistasActivos()

  return (
    <>
      {/* DES-11: degradado marca-600 -> acento-500 con dos llamadas a la acción */}
      <section className="bg-linear-to-r from-marca-600 from-40% to-acento-500 text-white">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 md:py-20">
          <h1 className="max-w-xl text-4xl font-bold sm:text-5xl">Descubre los mejores eventos</h1>
          <p className="mt-4 text-lg text-white/90">Música, cultura, deporte y tecnología en un solo lugar.</p>
          <div className="mt-8 flex flex-col gap-3 sm:flex-row">
            <Link to={RUTAS.eventos} className={clasesBoton('claro', 'px-6')}>
              Ver cartelera
            </Link>
            {/* "Crear cuenta" se oculta con sesión (§9.3) */}
            {sesion.estado === 'anonimo' ? (
              <Link to={RUTAS.registro} className={clasesBoton('contornoClaro', 'px-6')}>
                Crear cuenta
              </Link>
            ) : null}
          </div>
        </div>
      </section>

      <Contenedor className="flex flex-col gap-12">
        <section aria-labelledby="titulo-proximos">
          <div className="mb-6 flex items-center justify-between">
            <h2 id="titulo-proximos" className="text-xl font-semibold">
              Próximos eventos
            </h2>
            <EnlaceVerTodos a={RUTAS.eventos} etiqueta="Ver todos los eventos" />
          </div>
          {eventos.isPending ? (
            <CargandoTarjetas etiqueta="Cargando eventos…" />
          ) : eventos.isError ? (
            <EstadoError mensaje={mensajeDeError(eventos.error)} onReintentar={() => void eventos.refetch()} />
          ) : eventos.data.length === 0 ? (
            <EstadoVacio mensaje="Aún no hay eventos publicados" />
          ) : (
            <GrillaEventos eventos={eventos.data.slice(0, 3)} />
          )}
        </section>

        <section aria-labelledby="titulo-artistas">
          <div className="mb-6 flex items-center justify-between">
            <h2 id="titulo-artistas" className="text-xl font-semibold">
              Artistas destacados
            </h2>
            <EnlaceVerTodos a={RUTAS.artistas} etiqueta="Ver todos los artistas" />
          </div>
          {artistas.isPending ? (
            <CargandoTarjetas cantidad={3} etiqueta="Cargando artistas…" />
          ) : artistas.isError ? (
            <EstadoError mensaje={mensajeDeError(artistas.error)} onReintentar={() => void artistas.refetch()} />
          ) : artistas.data.length === 0 ? (
            <EstadoVacio mensaje="Aún no hay artistas activos" />
          ) : (
            <ul className="flex flex-wrap gap-6">
              {artistas.data.slice(0, 6).map((artista) => (
                <li key={artista.id}>
                  <ChipArtista artista={artista} />
                </li>
              ))}
            </ul>
          )}
        </section>
      </Contenedor>
    </>
  )
}
