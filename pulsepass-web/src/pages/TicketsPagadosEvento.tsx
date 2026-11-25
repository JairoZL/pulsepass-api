import { useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'
import { Badge } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { CodigoTexto } from '../componentes/CodigoTexto'
import { Contenedor } from '../componentes/Contenedor'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/Estados'
import { PrecioCOP } from '../componentes/PrecioCOP'
import { Tabla, type ColumnaTabla } from '../componentes/Tabla'
import { useAvisos } from '../componentes/avisosContexto'
import { useTicketsPagadosPorEvento, useUsarTicket } from '../funcionalidades/tickets/hooks'
import type { TicketResponse } from '../funcionalidades/tickets/tipos'
import { esApiError, mensajeDeError } from '../lib/errores'
import { RUTAS } from '../lib/rutas'

// PRD Frontend §9.15 (TicketsPagados.png, MovilPagados.png). FR-FE-STF-002.
// Sin :eventCode la página pide el código; con él, lista los tickets pagados del evento.
export default function TicketsPagadosEvento() {
  const { eventCode } = useParams()
  const navigate = useNavigate()
  const [codigo, setCodigo] = useState(eventCode ?? '')
  const pagados = useTicketsPagadosPorEvento(eventCode)
  const usar = useUsarTicket()
  const { mostrarAviso } = useAvisos()

  function irAEvento(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const limpio = codigo.trim().toUpperCase()
    if (limpio) navigate(RUTAS.staffTicketsPagados(limpio))
  }

  function marcarUsado(ticket: TicketResponse) {
    usar.mutate(ticket.ticketCode, {
      onSuccess: () => mostrarAviso('exito', `Ingreso registrado: ${ticket.ticketCode}.`),
      onError: (error) => {
        if (!esApiError(error, 401)) mostrarAviso('error', mensajeDeError(error))
      },
    })
  }

  const columnas: ColumnaTabla<TicketResponse>[] = [
    { encabezado: 'Código', celda: (t) => <CodigoTexto>{t.ticketCode}</CodigoTexto> },
    { encabezado: 'Tipo', celda: (t) => t.type },
    { encabezado: 'Titular', celda: (t) => t.userEmail }, // la API no trae el nombre (brecha documentada)
    { encabezado: 'Precio', celda: (t) => <PrecioCOP valor={t.price} /> },
    { encabezado: 'Estado', celda: (t) => <Badge tono={t.status} /> },
    {
      encabezado: 'Acción',
      celda: (t) => (
        <Boton
          variante="secundario"
          className="px-3 py-1.5"
          aria-label={`Usar ticket ${t.ticketCode}`}
          disabled={usar.isPending}
          onClick={() => marcarUsado(t)}
        >
          Usar
        </Boton>
      ),
    },
  ]

  return (
    <Contenedor>
      <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <h1 className="text-3xl font-bold">
          Tickets pagados{eventCode ? <CodigoTexto className="text-slate-500"> · {eventCode}</CodigoTexto> : null}
        </h1>
        <form className="flex items-end gap-2" onSubmit={irAEvento}>
          <Campo
            etiqueta="Evento"
            placeholder="CMF-2026"
            className="font-mono"
            value={codigo}
            onChange={(evento) => setCodigo(evento.target.value)}
          />
          <Boton type="submit">Ir</Boton>
        </form>
      </div>

      {!eventCode ? (
        <EstadoVacio mensaje="Ingresa el código de un evento para ver sus tickets pagados." />
      ) : pagados.isPending ? (
        <Cargando etiqueta="Cargando tickets…" />
      ) : pagados.isError ? (
        esApiError(pagados.error, 404) ? (
          <EstadoVacio mensaje={`No encontramos el evento ${eventCode}.`} />
        ) : (
          <EstadoError mensaje={mensajeDeError(pagados.error)} onReintentar={() => void pagados.refetch()} />
        )
      ) : pagados.data.length === 0 ? (
        <EstadoVacio mensaje="No hay tickets pagados" />
      ) : (
        <>
          <p className="mb-4 text-sm text-slate-600 dark:text-slate-400">Total: {pagados.data.length}</p>
          <Tabla
            columnas={columnas}
            filas={pagados.data}
            claveFila={(t) => t.ticketCode}
            etiqueta={`Tickets pagados de ${eventCode}`}
          />
        </>
      )}
    </Contenedor>
  )
}
