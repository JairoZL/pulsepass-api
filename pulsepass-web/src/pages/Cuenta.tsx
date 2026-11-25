import { Link } from 'react-router'
import { Avatar } from '../componentes/Avatar'
import { Badge } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Contenedor } from '../componentes/Contenedor'
import { Cargando, EstadoError } from '../componentes/Estados'
import { clasesBoton } from '../componentes/estilos'
import { Tarjeta } from '../componentes/Tarjeta'
import { useAuth } from '../funcionalidades/auth/contexto'
import { useSalir } from '../funcionalidades/auth/useSalir'
import { useUsuarioPorEmail } from '../funcionalidades/usuarios/hooks'
import { mensajeDeError } from '../lib/errores'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §9.10 (MiCuenta.png). GAP-01: UserResponse no trae nombres ni fecha de nacimiento,
// así que se muestra usuario, correo, roles y estado (documentado en el README).
export default function Cuenta() {
  const { sesion } = useAuth()
  const salir = useSalir()
  const sesionUsuario = sesion.estado === 'autenticado' ? sesion.usuario : null
  const usuario = useUsuarioPorEmail(sesionUsuario?.email)

  if (!sesionUsuario) return null // RutaProtegida ya garantiza la sesión

  return (
    <Contenedor ancho="estrecho">
      <h1 className="mb-6 text-3xl font-bold">Mi cuenta</h1>
      {usuario.isPending ? (
        <Cargando etiqueta="Cargando cuenta…" />
      ) : usuario.isError ? (
        <EstadoError mensaje={mensajeDeError(usuario.error)} onReintentar={() => void usuario.refetch()} />
      ) : (
        <Tarjeta className="flex flex-col gap-6 p-6">
          <div className="flex items-center gap-4">
            <Avatar nombre={usuario.data.username} tamano="lg" />
            <div>
              <p className="text-xl font-semibold">{usuario.data.username}</p>
              <p className="text-slate-600 dark:text-slate-400">{usuario.data.email}</p>
            </div>
          </div>
          <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-[auto_1fr] sm:gap-x-6">
            <dt className="text-slate-600 dark:text-slate-400">Roles</dt>
            <dd className="flex flex-wrap gap-2">
              {sesionUsuario.roles.map((rol) => (
                <Badge key={rol} tono="ROL">
                  {rol}
                </Badge>
              ))}
            </dd>
            <dt className="text-slate-600 dark:text-slate-400">Estado</dt>
            <dd>
              <Badge tono={usuario.data.active ? 'ACTIVA' : 'INACTIVA'}>{usuario.data.active ? 'Activa' : 'Inactiva'}</Badge>
            </dd>
          </dl>
          <div className="flex flex-col gap-3 sm:flex-row">
            {sesionUsuario.roles.includes('USER') ? (
              <Link to={RUTAS.misTickets} className={clasesBoton('primario')}>
                Ver mis tickets
              </Link>
            ) : null}
            <Boton variante="secundario" onClick={() => void salir()}>
              Cerrar sesión
            </Boton>
          </div>
        </Tarjeta>
      )}
    </Contenedor>
  )
}
