import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../lib/http'
import { crearClientePruebas, renderApp } from '../test/utils'
import ErrorPagina from './ErrorPagina'

function renderConError(error: unknown) {
  function Lanzador(): never {
    throw error
  }
  const router = createMemoryRouter([{ path: '/', Component: Lanzador, errorElement: <ErrorPagina /> }])
  render(
    <QueryClientProvider client={crearClientePruebas()}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe('Páginas de error (PRD Frontend §9.19)', () => {
  it('TEST-FE-GEN-001: ruta inexistente -> 404', async () => {
    renderApp('/esta/ruta/no/existe')

    expect(await screen.findByText('No encontramos esta página.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toBeInTheDocument()
  })

  it('TEST-FE-GEN-002: errorElement muestra un mensaje seguro, sin el detalle técnico', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {}) // React registra el error lanzado
    renderConError(new Error('detalle interno secreto en Service.java:42'))

    expect(await screen.findByRole('heading', { name: '¡Ups! Algo salió mal.' })).toBeInTheDocument()
    expect(screen.queryByText(/secreto/)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument()
  })

  it('un 403 de la API al cargar la página muestra la pantalla 403', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    renderConError(new ApiError(403, 'Forbidden'))

    expect(await screen.findByText('No tienes permisos para acceder a esta sección.')).toBeInTheDocument()
  })

  it('la ruta /403 existe y es accesible para cualquiera', async () => {
    renderApp('/403')
    expect(await screen.findByText('No tienes permisos para acceder a esta sección.')).toBeInTheDocument()
  })
})
