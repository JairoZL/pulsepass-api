import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { CampoContrasena } from '../componentes/CampoContrasena'
import { TarjetaAcceso } from '../funcionalidades/auth/componentes/TarjetaAcceso'
import { BarraFortaleza } from '../funcionalidades/usuarios/componentes/BarraFortaleza'
import { useRegistrarUsuario } from '../funcionalidades/usuarios/hooks'
import { RegistroFormSchema, type RegistroForm } from '../funcionalidades/usuarios/tipos'
import { esApiError, mensajeDeError } from '../lib/errores'
import { aplicarErroresServidor } from '../lib/formularios'
import { RUTAS } from '../lib/rutas'

const CAMPOS = ['username', 'email', 'firstName', 'lastName', 'birthDate', 'password'] as const

// PRD Frontend §9.9 (Registro.png). FR-FE-AUTH-001
export default function Registro() {
  const navigate = useNavigate()
  const registro = useRegistrarUsuario()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<RegistroForm>({
    resolver: zodResolver(RegistroFormSchema),
    defaultValues: { username: '', email: '', firstName: '', lastName: '', birthDate: '', password: '' },
  })
  const clave = useWatch({ control, name: 'password' })

  function enviar(datos: RegistroForm) {
    setErrorGeneral(null)
    registro.mutate(datos, {
      // 201: no inicia sesión solo; lleva al login con el aviso "Cuenta creada"
      onSuccess: () => navigate(RUTAS.login, { state: { registrado: true } }),
      onError: (error) => {
        if (esApiError(error, 409)) {
          setErrorGeneral('El usuario o correo ya está registrado.')
        } else if (esApiError(error, 400)) {
          // El backend nombra "passwordWithinBcryptLimit" la regla de 72 bytes: va al campo contraseña.
          // La edad mínima también llega como 400 y se muestra tal cual (§24.1).
          setErrorGeneral(
            aplicarErroresServidor(error.details, setError, CAMPOS, { passwordWithinBcryptLimit: 'password' }) ??
              (Object.keys(error.details).length === 0 ? mensajeDeError(error) : null),
          )
        } else {
          setErrorGeneral(mensajeDeError(error))
        }
      },
    })
  }

  return (
    <TarjetaAcceso titulo="Crea tu cuenta" ancho="max-w-xl">
      <form className="flex flex-col gap-4" onSubmit={handleSubmit(enviar)} noValidate>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Campo etiqueta="Nombres" autoComplete="given-name" error={errors.firstName?.message} {...register('firstName')} />
          <Campo etiqueta="Apellidos" autoComplete="family-name" error={errors.lastName?.message} {...register('lastName')} />
        </div>
        <Campo etiqueta="Usuario" autoComplete="username" error={errors.username?.message} {...register('username')} />
        <Campo etiqueta="Correo" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <Campo
          etiqueta="Fecha de nacimiento"
          type="date"
          autoComplete="bday"
          error={errors.birthDate?.message}
          {...register('birthDate')}
        />
        <div className="flex flex-col gap-2">
          <CampoContrasena
            etiqueta="Contraseña"
            autoComplete="new-password"
            ayuda="Mínimo 12 caracteres. Una frase larga es más segura que una contraseña corta."
            error={errors.password?.message}
            {...register('password')}
          />
          <BarraFortaleza clave={clave} />
        </div>
        {errorGeneral ? <Aviso tono="error">{errorGeneral}</Aviso> : null}
        <Boton type="submit" cargando={registro.isPending} className="w-full">
          Crear cuenta
        </Boton>
      </form>
      <p className="text-center text-sm text-slate-600 dark:text-slate-400">
        ¿Ya tienes cuenta?{' '}
        <Link to={RUTAS.login} className="font-semibold text-marca-600 hover:text-marca-700 dark:text-marca-500">
          Inicia sesión
        </Link>
      </p>
    </TarjetaAcceso>
  )
}
