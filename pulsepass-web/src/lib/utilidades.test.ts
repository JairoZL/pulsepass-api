import { describe, expect, it } from 'vitest'
import { mensajeDeError } from './errores'
import { formatearPrecio, iniciales } from './formato'
import { ApiError, MENSAJES_GENERICOS } from './http'
import { leerEstadoNavegacion, rutaDeRetorno } from './navegacion'
import { debeReintentar } from './queryClient'
import { borrarSesion, leerSesionGuardada } from './sesionAlmacen'

describe('lib/formato', () => {
  it('TEST-FE-LIB-005: PrecioCOP formatea 250000 como pesos colombianos', () => {
    // \s acepta el espacio no separable que pone Intl entre "$" y el número
    expect(formatearPrecio(250000)).toMatch(/^\$\s?250\.000$/)
  })

  it('iniciales de una o dos palabras', () => {
    expect(iniciales('Solar Beat')).toBe('SB')
    expect(iniciales('andrea')).toBe('AN')
  })
})

describe('lib/errores (§13)', () => {
  it('409 y 400 muestran el mensaje del backend; 500 y red muestran textos genéricos', () => {
    expect(mensajeDeError(new ApiError(409, 'Event has no capacity available'))).toBe('Event has no capacity available')
    expect(mensajeDeError(new ApiError(500, 'NullPointerException en línea 42'))).toBe(MENSAJES_GENERICOS.inesperado)
    expect(mensajeDeError(new ApiError(0, 'x'))).toBe(MENSAJES_GENERICOS.red)
    expect(mensajeDeError(new Error('cualquier cosa'))).toBe(MENSAJES_GENERICOS.inesperado)
  })
})

describe('lib/queryClient (§14.3)', () => {
  it('no reintenta 4xx; reintenta una vez red y 5xx', () => {
    expect(debeReintentar(0, new ApiError(404, 'x'))).toBe(false)
    expect(debeReintentar(0, new ApiError(409, 'x'))).toBe(false)
    expect(debeReintentar(0, new ApiError(500, 'x'))).toBe(true)
    expect(debeReintentar(1, new ApiError(500, 'x'))).toBe(false)
    expect(debeReintentar(0, new ApiError(0, 'x'))).toBe(true)
  })
})

describe('lib/navegacion', () => {
  it('solo acepta rutas internas como destino de regreso (evita redirección abierta)', () => {
    expect(rutaDeRetorno({ desde: '/eventos/CMF-2026' })).toBe('/eventos/CMF-2026')
    expect(rutaDeRetorno({ desde: '//sitio-malicioso.com' })).toBe('/')
    expect(rutaDeRetorno({ desde: 'https://sitio-malicioso.com' })).toBe('/')
    expect(rutaDeRetorno(null)).toBe('/')
    expect(leerEstadoNavegacion({ motivo: 'expirada' }).motivo).toBe('expirada')
  })
})

describe('lib/sesionAlmacen (§12.2)', () => {
  it('descarta una sesión guardada vencida o con formato inválido', () => {
    sessionStorage.setItem('pulsepass.sesion', JSON.stringify({ token: 't', expiraEn: Date.now() - 1 }))
    expect(leerSesionGuardada()).toBeNull()
    expect(sessionStorage.getItem('pulsepass.sesion')).toBeNull()

    sessionStorage.setItem('pulsepass.sesion', '{no es json')
    expect(leerSesionGuardada()).toBeNull()

    borrarSesion()
  })
})
