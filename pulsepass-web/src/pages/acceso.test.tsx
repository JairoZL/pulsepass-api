import { fireEvent, screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { CONTRASENA_VALIDA } from '../test/mocks/datos'
import { errorApi } from '../test/mocks/handlers'
import { server } from '../test/mocks/servidor'
import { contarPeticiones, renderApp } from '../test/utils'

type Usuario = ReturnType<typeof renderApp>['user']

async function llenarRegistro(user: Usuario, clave = CONTRASENA_VALIDA) {
  await user.type(await screen.findByLabelText('Nombres'), 'Andrea')
  await user.type(screen.getByLabelText('Apellidos'), 'Gómez')
  await user.type(screen.getByLabelText('Usuario'), 'andrea2')
  await user.type(screen.getByLabelText('Correo'), 'andrea2@pulsepass.test')
  // jsdom no simula el selector de fecha: se asigna el valor directamente
  fireEvent.change(screen.getByLabelText('Fecha de nacimiento'), { target: { value: '2000-01-15' } })
  await user.type(screen.getByLabelText('Contraseña'), clave)
}

describe('Registro (FR-FE-AUTH-001)', () => {
  it('TEST-FE-AUTH-001: registro exitoso lleva al login con "Cuenta creada"', async () => {
    let cuerpo: unknown
    server.use(
      http.post('/api/users', async ({ request }) => {
        cuerpo = await request.json()
        return HttpResponse.json({ id: 99, username: 'andrea2', email: 'andrea2@pulsepass.test', active: true }, { status: 201 })
      }),
    )
    const { user, router } = renderApp('/registro')

    await llenarRegistro(user)
    await user.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByText('Cuenta creada, inicia sesión.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(cuerpo).toEqual({
      username: 'andrea2',
      email: 'andrea2@pulsepass.test',
      firstName: 'Andrea',
      lastName: 'Gómez',
      birthDate: '2000-01-15',
      password: CONTRASENA_VALIDA,
    })
  })

  it('TEST-FE-AUTH-002: una contraseña corta se rechaza sin llamar a la API', async () => {
    const registros = contarPeticiones('POST', '/api/users')
    const { user } = renderApp('/registro')

    await llenarRegistro(user, 'corta')
    await user.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByText('La contraseña debe tener al menos 12 caracteres')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('aria-invalid', 'true')
    expect(registros()).toBe(0)
  })

  it('TEST-FE-AUTH-003: 409 muestra "El usuario o correo ya está registrado."', async () => {
    server.use(http.post('/api/users', () => errorApi(409, 'Username already exists: andrea2')))
    const { user } = renderApp('/registro')

    await llenarRegistro(user)
    await user.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByText('El usuario o correo ya está registrado.')).toBeInTheDocument()
  })

  it('un 400 reparte los details en sus campos (passwordWithinBcryptLimit -> contraseña)', async () => {
    server.use(
      http.post('/api/users', () =>
        errorApi(400, 'Validation failed', {
          email: 'must be a well-formed email address',
          passwordWithinBcryptLimit: 'password must not exceed 72 bytes',
        }),
      ),
    )
    const { user } = renderApp('/registro')

    await llenarRegistro(user)
    await user.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(await screen.findByText('must be a well-formed email address')).toBeInTheDocument()
    expect(screen.getByText('password must not exceed 72 bytes')).toBeInTheDocument()
    expect(screen.getByLabelText('Correo')).toHaveAttribute('aria-invalid', 'true')
  })

  it('el indicador de fortaleza cambia al escribir', async () => {
    const { user } = renderApp('/registro')

    expect(await screen.findByText('Fortaleza: baja')).toBeInTheDocument()
    await user.type(screen.getByLabelText('Contraseña'), CONTRASENA_VALIDA)
    expect(screen.getByText('Fortaleza: alta')).toBeInTheDocument()
  })
})

describe('Inicio de sesión (FR-FE-AUTH-002/003)', () => {
  it('TEST-FE-AUTH-004: login correcto vuelve a la ruta de origen', async () => {
    const { user, router } = renderApp('/cuenta')

    await user.type(await screen.findByLabelText('Usuario o correo'), 'andrea')
    await user.type(screen.getByLabelText('Contraseña'), CONTRASENA_VALIDA)
    await user.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('heading', { name: 'Mi cuenta' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/cuenta')
    expect(await screen.findByText('andrea@pulsepass.test')).toBeInTheDocument()
  })

  it('TEST-FE-AUTH-005: credenciales incorrectas -> "Credenciales inválidas." sin pistas', async () => {
    const { user } = renderApp('/login')

    await user.type(await screen.findByLabelText('Usuario o correo'), 'andrea')
    await user.type(screen.getByLabelText('Contraseña'), 'OtraFraseIncorrecta')
    await user.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByText('Credenciales inválidas.')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña')).toHaveValue('OtraFraseIncorrecta')
  })

  it('campos vacíos se validan antes de llamar a la API', async () => {
    const logins = contarPeticiones('POST', '/api/auth/login')
    const { user } = renderApp('/login')

    await user.click(await screen.findByRole('button', { name: 'Ingresar' }))

    expect(screen.getByLabelText('Usuario o correo')).toHaveAttribute('aria-invalid', 'true')
    expect(logins()).toBe(0)
  })

  it('TEST-FE-AUTH-006: un 401 con sesión iniciada la cierra y avisa "Tu sesión expiró"', async () => {
    server.use(http.get('/api/users/by-email', () => errorApi(401, 'Token expirado')))
    const { router } = renderApp('/cuenta', { usuario: 'andrea' })

    expect(await screen.findByText('Tu sesión expiró. Inicia sesión de nuevo.')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(sessionStorage.getItem('pulsepass.sesion')).toBeNull()
  })

  it('con sesión iniciada, /login redirige al inicio', async () => {
    const { router } = renderApp('/login', { usuario: 'andrea' })

    expect(await screen.findByRole('heading', { name: 'Descubre los mejores eventos' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/')
  })
})

describe('Mi cuenta (FR-FE-AUTH-005)', () => {
  it('muestra usuario, correo, roles y estado', async () => {
    renderApp('/cuenta', { usuario: 'laura' })

    expect(await screen.findByText('laura@pulsepass.test')).toBeInTheDocument()
    expect(screen.getByText('USER')).toBeInTheDocument()
    expect(screen.getByText('STAFF')).toBeInTheDocument()
    expect(screen.getByText('Activa')).toBeInTheDocument()
  })

  it('"Cerrar sesión" vuelve al inicio como anónimo', async () => {
    const { user, router } = renderApp('/cuenta', { usuario: 'andrea' })

    await user.click(await screen.findByRole('button', { name: 'Cerrar sesión' }))

    expect(await screen.findByRole('link', { name: 'Crear cuenta' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/')
    expect(sessionStorage.getItem('pulsepass.sesion')).toBeNull()
  })
})
