import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { CampoContrasena } from '../componentes/CampoContrasena'
import { TarjetaAcceso } from '../funcionalidades/auth/componentes/TarjetaAcceso'
import { useAuth } from '../funcionalidades/auth/contexto'
import { LoginFormSchema, type LoginForm } from '../funcionalidades/auth/tipos'
import { esApiError, mensajeDeError } from '../lib/errores'
import { aplicarErroresServidor } from '../lib/formularios'
import { leerEstadoNavegacion, rutaDeRetorno } from '../lib/navegacion'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §9.8 (Login.png). FR-FE-AUTH-002/003
export default function Login() {
  const { iniciarSesion } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { registrado, motivo } = leerEstadoNavegacion(location.state)
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(LoginFormSchema),
    defaultValues: { identifier: '', password: '' },
  })

  async function enviar(datos: LoginForm) {
    setErrorGeneral(null)
    try {
      await iniciarSesion(datos)
      // Vuelve a la ruta de origen (state.desde) o al inicio
      navigate(rutaDeRetorno(location.state), { replace: true })
    } catch (error) {
      if (esApiError(error, 401)) {
        // FR-FE-AUTH-003: nunca distinguir "usuario no existe" de "contraseña incorrecta"
        setErrorGeneral('Credenciales inválidas.')
      } else if (esApiError(error, 400)) {
        setErrorGeneral(aplicarErroresServidor(error.details, setError, ['identifier', 'password']))
      } else {
        setErrorGeneral(mensajeDeError(error))
      }
    }
  }

  return (
    <TarjetaAcceso titulo="Inicia sesión">
      {registrado ? <Aviso tono="exito">Cuenta creada, inicia sesión.</Aviso> : null}
      {motivo === 'expirada' ? <Aviso tono="info">Tu sesión expiró. Inicia sesión de nuevo.</Aviso> : null}

      <form className="flex flex-col gap-4" onSubmit={handleSubmit(enviar)} noValidate>
        <Campo
          etiqueta="Usuario o correo"
          autoComplete="username"
          error={errors.identifier?.message}
          {...register('identifier')}
        />
        <CampoContrasena
          etiqueta="Contraseña"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />
        {errorGeneral ? <Aviso tono="error">{errorGeneral}</Aviso> : null}
        <Boton type="submit" cargando={isSubmitting} className="w-full">
          Ingresar
        </Boton>
      </form>

      <p className="text-center text-sm text-slate-600 dark:text-slate-400">
        ¿No tienes cuenta?{' '}
        <Link to={RUTAS.registro} className="font-semibold text-marca-600 hover:text-marca-700 dark:text-marca-500">
          Regístrate
        </Link>
      </p>
    </TarjetaAcceso>
  )
}
