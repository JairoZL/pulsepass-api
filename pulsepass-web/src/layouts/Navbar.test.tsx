import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { NombreUsuario } from '../test/mocks/datos'
import { renderApp } from '../test/utils'

async function enlacesDeNavegacion(usuario?: NombreUsuario) {
  renderApp('/', usuario ? { usuario } : {})
  if (usuario) {
    // Espera a que la sesión se restaure (aparece el menú del usuario)
    await within(screen.getByRole('banner')).findByRole('button', { name: new RegExp(usuario) })
  }
  const nav = screen.getByRole('navigation', { name: 'Principal' })
  return within(nav)
    .getAllByRole('link')
    .map((enlace) => enlace.textContent)
}

describe('Navbar por rol (PRD Frontend §8.4, TEST-FE-GEN-003)', () => {
  it('anónimo: Eventos, Artistas, Sedes + Ingresar y Registrarse', async () => {
    expect(await enlacesDeNavegacion()).toEqual(['Eventos', 'Artistas', 'Sedes'])
    const encabezado = screen.getByRole('banner')
    expect(within(encabezado).getByRole('link', { name: 'Ingresar' })).toBeInTheDocument()
    expect(within(encabezado).getByRole('link', { name: 'Registrarse' })).toBeInTheDocument()
  })

  it('USER: agrega Mis tickets', async () => {
    expect(await enlacesDeNavegacion('andrea')).toEqual(['Eventos', 'Artistas', 'Sedes', 'Mis tickets'])
  })

  it('STAFF: Validar ingreso y Tickets pagados, sin Administrar eventos', async () => {
    expect(await enlacesDeNavegacion('laura')).toEqual([
      'Eventos',
      'Artistas',
      'Sedes',
      'Validar ingreso',
      'Tickets pagados',
    ])
  })

  it('ADMIN: además Administrar eventos', async () => {
    expect(await enlacesDeNavegacion('admin')).toEqual([
      'Eventos',
      'Artistas',
      'Sedes',
      'Validar ingreso',
      'Tickets pagados',
      'Administrar eventos',
    ])
  })
})

describe('Menú móvil (TEST-FE-GEN-004)', () => {
  it('abre con Enter y cierra con Escape, usando solo el teclado', async () => {
    const { user } = renderApp('/')
    const boton = screen.getByRole('button', { name: 'Abrir menú' })

    boton.focus()
    await user.keyboard('{Enter}')
    expect(screen.getByRole('navigation', { name: 'Principal móvil' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cerrar menú' })).toHaveAttribute('aria-expanded', 'true')

    await user.keyboard('{Escape}')
    expect(screen.queryByRole('navigation', { name: 'Principal móvil' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Abrir menú' })).toHaveAttribute('aria-expanded', 'false')
  })
})
