import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Badge } from './Badge'
import { Boton } from './Boton'
import { Campo } from './Campo'
import { Modal } from './Modal'
import { Tabla } from './Tabla'

describe('Componentes base (PRD Frontend §9.1)', () => {
  it('Boton cargando: deshabilitado y con texto "Cargando…" (evita doble envío)', () => {
    render(<Boton cargando>Ingresar</Boton>)

    const boton = screen.getByRole('button', { name: 'Cargando…' })
    expect(boton).toBeDisabled()
    expect(boton).toHaveAttribute('aria-busy', 'true')
  })

  it('Campo con error: etiqueta asociada, aria-invalid y descripción accesible', () => {
    render(<Campo etiqueta="Correo electrónico" error="Ingresa un correo válido" />)

    const campo = screen.getByLabelText('Correo electrónico')
    expect(campo).toHaveAttribute('aria-invalid', 'true')
    expect(campo).toHaveAccessibleDescription('Ingresa un correo válido')
  })

  it('Modal: pone el foco adentro y se cierra con Escape', async () => {
    const onCerrar = vi.fn()
    render(
      <Modal abierto titulo="Confirmar compra" onCerrar={onCerrar}>
        <p>Contenido</p>
      </Modal>,
    )

    const dialogo = screen.getByRole('dialog', { name: 'Confirmar compra' })
    expect(dialogo).toContainElement(document.activeElement as HTMLElement)

    await userEvent.keyboard('{Escape}')
    expect(onCerrar).toHaveBeenCalledTimes(1)
  })

  it('Modal cerrado no renderiza nada', () => {
    render(
      <Modal abierto={false} titulo="Oculto" onCerrar={() => {}}>
        x
      </Modal>,
    )
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('Tabla genérica: encabezados y una fila por elemento', () => {
    const filas = [
      { codigo: 'TKT-1', tipo: 'VIP' },
      { codigo: 'TKT-2', tipo: 'GENERAL' },
    ]
    render(
      <Tabla
        etiqueta="Tickets"
        filas={filas}
        claveFila={(fila) => fila.codigo}
        columnas={[
          { encabezado: 'Código', celda: (fila) => fila.codigo },
          { encabezado: 'Tipo', celda: (fila) => fila.tipo },
        ]}
      />,
    )

    const tabla = screen.getByRole('table', { name: 'Tickets' })
    expect(tabla.querySelectorAll('tbody tr')).toHaveLength(2)
    expect(screen.getAllByText('TKT-2').length).toBeGreaterThan(0)
  })

  it('Badge muestra el estado', () => {
    render(<Badge tono="SOLD_OUT" />)
    expect(screen.getByText('SOLD_OUT')).toBeInTheDocument()
  })
})
