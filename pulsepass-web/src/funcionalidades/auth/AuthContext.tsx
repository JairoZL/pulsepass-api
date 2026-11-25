import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { configurarAlNoAutorizado } from '../../lib/http'
import { borrarSesion, guardarSesion, leerSesionGuardada } from '../../lib/sesionAlmacen'
import { obtenerUsuarioPorUsername } from '../usuarios/api'
import { iniciarSesionApi, obtenerMe } from './api'
import { AuthContext, type ValorAuth } from './contexto'
import type { LoginForm, Sesion, UsuarioAutenticado } from './tipos'
import { borrarEventosCreados } from '../eventos/creadosSesion'

// PRD Frontend §12.1: el proveedor de la sesión (sesion, iniciarSesion, cerrarSesion, tieneRol).
// Solo guarda la identidad; los demás datos del servidor viven en TanStack Query (FE-005).

// /api/auth/me no trae email (GAP-03): se completa con el perfil propio por username
async function cargarUsuario(): Promise<UsuarioAutenticado> {
  const me = await obtenerMe()
  const perfil = await obtenerUsuarioPorUsername(me.username)
  return { id: me.id, username: me.username, email: perfil.email, roles: me.roles }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()

  // Si hay un token guardado y vigente, arranca "cargando" mientras se confirma con el servidor
  const [sesion, setSesion] = useState<Sesion>(() =>
    leerSesionGuardada() ? { estado: 'cargando' } : { estado: 'anonimo' },
  )

  // FR-FE-AUTH-006: cerrar sesión borra el token y TODA la caché de consultas
  const cerrarSesion = useCallback(
    (motivo?: 'expirada') => {
      borrarSesion()
      borrarEventosCreados() // la lista de ADMIN es de esta sesión (§9.16)
      queryClient.clear()
      setSesion(motivo ? { estado: 'anonimo', motivo } : { estado: 'anonimo' })
    },
    [queryClient],
  )

  // FR-FE-AUTH-004: al recargar, restaura la sesión validándola contra /api/auth/me
  useEffect(() => {
    const guardada = leerSesionGuardada()
    if (!guardada) return
    let cancelado = false
    cargarUsuario()
      .then((usuario) => {
        if (!cancelado) setSesion({ estado: 'autenticado', ...guardada, usuario })
      })
      .catch(() => {
        if (!cancelado) {
          borrarSesion()
          setSesion({ estado: 'anonimo' })
        }
      })
    return () => {
      cancelado = true
    }
  }, [])

  // FR-FE-AUTH-005: cualquier 401 con sesión activa la cierra (lib/http.ts avisa aquí)
  useEffect(() => {
    configurarAlNoAutorizado(() => cerrarSesion('expirada'))
  }, [cerrarSesion])

  // §12.2: cierre local programado para cuando vence el token (15 min)
  useEffect(() => {
    if (sesion.estado !== 'autenticado') return
    const id = window.setTimeout(() => cerrarSesion('expirada'), Math.max(sesion.expiraEn - Date.now(), 0))
    return () => window.clearTimeout(id)
  }, [sesion, cerrarSesion])

  // Login: token -> /me -> perfil. Si algo falla después del token, no queda sesión a medias
  const iniciarSesion = useCallback(async (datos: LoginForm) => {
    const auth = await iniciarSesionApi(datos)
    const expiraEn = Date.now() + auth.expiresIn * 1000
    guardarSesion({ token: auth.accessToken, expiraEn })
    try {
      const usuario = await cargarUsuario()
      setSesion({ estado: 'autenticado', token: auth.accessToken, expiraEn, usuario })
    } catch (error) {
      borrarSesion()
      throw error
    }
  }, [])

  const valor = useMemo<ValorAuth>(
    () => ({
      sesion,
      iniciarSesion,
      cerrarSesion,
      tieneRol: (...roles) =>
        sesion.estado === 'autenticado' && roles.some((rol) => sesion.usuario.roles.includes(rol)),
    }),
    [sesion, iniciarSesion, cerrarSesion],
  )

  return <AuthContext value={valor}>{children}</AuthContext>
}
