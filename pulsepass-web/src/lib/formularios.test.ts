import { describe, expect, it, vi } from 'vitest'
import { calcularFortaleza } from '../funcionalidades/usuarios/fortaleza'
import { aplicarErroresServidor } from './formularios'

describe('aplicarErroresServidor (PRD Frontend §11.2)', () => {
  it('asigna cada detail a su campo y devuelve los que no tienen campo', () => {
    const setError = vi.fn()
    const resto = aplicarErroresServidor(
      { email: 'correo inválido', passwordWithinBcryptLimit: 'muy larga', otro: 'regla general' },
      setError,
      ['email', 'password'],
      { passwordWithinBcryptLimit: 'password' },
    )

    expect(setError).toHaveBeenCalledWith('email', { type: 'server', message: 'correo inválido' })
    expect(setError).toHaveBeenCalledWith('password', { type: 'server', message: 'muy larga' })
    expect(resto).toBe('regla general')
  })

  it('sin details sueltos devuelve null', () => {
    expect(aplicarErroresServidor({ email: 'x' }, vi.fn(), ['email'])).toBeNull()
  })
})

describe('calcularFortaleza', () => {
  it('es baja con menos de 12 caracteres y alta con una frase larga variada', () => {
    expect(calcularFortaleza('abc').etiqueta).toBe('baja')
    expect(calcularFortaleza('UnaFraseSeguraLarga2026!').etiqueta).toBe('alta')
  })
})
