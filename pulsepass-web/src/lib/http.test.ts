import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { z } from 'zod'
import { errorApi } from '../test/mocks/handlers'
import { server } from '../test/mocks/servidor'
import { ApiError, configurarAlNoAutorizado, MENSAJES_GENERICOS, solicitar } from './http'
import { guardarSesion } from './sesionAlmacen'

const OkSchema = z.object({ ok: z.boolean() })
const enUnMinuto = () => Date.now() + 60_000

// Captura la petición que llega a /api/prueba
function capturarPeticion(respuesta: () => Response = () => HttpResponse.json({ ok: true })) {
  const capturadas: Request[] = []
  server.use(
    http.get('/api/prueba', ({ request }) => {
      capturadas.push(request)
      return respuesta()
    }),
  )
  return capturadas
}

describe('lib/http', () => {
  it('TEST-FE-LIB-001: con sesión agrega Authorization Bearer y nunca pone el token en la URL', async () => {
    guardarSesion({ token: 'abc123', expiraEn: enUnMinuto() })
    const capturadas = capturarPeticion()

    await solicitar('/prueba', OkSchema)

    expect(capturadas[0]?.headers.get('Authorization')).toBe('Bearer abc123')
    expect(capturadas[0]?.url).not.toContain('abc123') // QT-FE-007
  })

  it('sin sesión no envía Authorization', async () => {
    const capturadas = capturarPeticion()

    await solicitar('/prueba', OkSchema)

    expect(capturadas[0]?.headers.has('Authorization')).toBe(false)
  })

  it('TEST-FE-LIB-002: ErrorResponse válido -> ApiError con status, message y details', async () => {
    capturarPeticion(() => errorApi(400, 'Validation failed', { email: 'Email must be valid' }))

    const error = await solicitar('/prueba', OkSchema).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 400, message: 'Validation failed', details: { email: 'Email must be valid' } })
  })

  it('TEST-FE-LIB-003: cuerpo de error que no cumple el contrato -> ApiError genérico', async () => {
    capturarPeticion(() => new HttpResponse('<html>Proxy error</html>', { status: 500 }))

    const error = await solicitar('/prueba', OkSchema).catch((e: unknown) => e)

    expect(error).toMatchObject({ status: 500, message: MENSAJES_GENERICOS.inesperado })
  })

  it('TEST-FE-LIB-004: un 401 con sesión dispara el cierre de sesión', async () => {
    const alNoAutorizado = vi.fn()
    configurarAlNoAutorizado(alNoAutorizado)
    guardarSesion({ token: 'vencido', expiraEn: enUnMinuto() })
    capturarPeticion(() => errorApi(401, 'Se requiere autenticación'))

    await expect(solicitar('/prueba', OkSchema)).rejects.toMatchObject({ status: 401 })
    expect(alNoAutorizado).toHaveBeenCalledTimes(1)
  })

  it('un 401 del login (cerrarSesionSi401: false) no cierra la sesión', async () => {
    const alNoAutorizado = vi.fn()
    configurarAlNoAutorizado(alNoAutorizado)
    guardarSesion({ token: 'algo', expiraEn: enUnMinuto() })
    capturarPeticion(() => errorApi(401, 'Credenciales inválidas'))

    await expect(solicitar('/prueba', OkSchema, { cerrarSesionSi401: false })).rejects.toMatchObject({ status: 401 })
    expect(alNoAutorizado).not.toHaveBeenCalled()
  })

  it('error de red -> ApiError con status 0 y mensaje de conexión', async () => {
    capturarPeticion(() => HttpResponse.error())

    await expect(solicitar('/prueba', OkSchema)).rejects.toMatchObject({
      status: 0,
      message: MENSAJES_GENERICOS.red,
    })
  })

  it('FE-002: respuesta 200 que no cumple el esquema zod -> ApiError (no se usan datos inválidos)', async () => {
    capturarPeticion(() => HttpResponse.json({ ok: 'no es booleano' }))

    await expect(solicitar('/prueba', OkSchema)).rejects.toMatchObject({
      message: MENSAJES_GENERICOS.respuestaInvalida,
    })
  })
})
