import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, vi } from 'vitest'
import { borrarSesion } from '../lib/sesionAlmacen'
import { server } from './mocks/servidor'

// Compatibilidad jsdom + Node: React Router (modo datos) crea un Request por cada navegación con el
// AbortSignal de jsdom, y el Request de Node (undici) lo rechaza: "Expected signal to be an instance
// of AbortSignal". En pruebas se quita esa señal; solo sirve para cancelar navegaciones interrumpidas.
// Va ANTES de server.listen() para que MSW envuelva esta versión de Request.
const RequestNativo = globalThis.Request
globalThis.Request = class RequestSinSenalJsdom extends RequestNativo {
  constructor(input: RequestInfo | URL, init?: RequestInit) {
    const opciones: RequestInit = { ...init }
    delete opciones.signal
    super(input, opciones)
  }
} as typeof Request

// onUnhandledRequest: 'error' -> si una página llama a un endpoint sin handler, la prueba falla (sin red real)
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  cleanup()
  server.resetHandlers()
  borrarSesion() // token en memoria + sessionStorage
  sessionStorage.clear()
  document.documentElement.classList.remove('dark')
})

afterAll(() => server.close())

// jsdom no implementa scrollTo (lo usa ScrollRestoration de React Router)
window.scrollTo = vi.fn() as unknown as typeof window.scrollTo