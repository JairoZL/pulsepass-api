import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Aviso } from '../componentes/Aviso'
import { Badge } from '../componentes/Badge'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { CodigoTexto } from '../componentes/CodigoTexto'
import { Contenedor } from '../componentes/Contenedor'
import { Cargando } from '../componentes/Estados'
import { Tarjeta } from '../componentes/Tarjeta'
import { useTicket, useUsarTicket } from '../funcionalidades/tickets/hooks'
import { BuscarTicketFormSchema, type BuscarTicketForm } from '../funcionalidades/tickets/tipos'
import { esApiError, mensajeDeError } from '../lib/errores'

// Texto del resultado de la búsqueda cuando falla (§9.14): 404 y 403 tienen su propio mensaje
function mensajeBusqueda(error: unknown): string {
  if (esApiError(error, 404)) return 'Ticket no encontrado.'
  if (esApiError(error, 403)) return 'Sin permisos para consultar este ticket.'
  return mensajeDeError(error)
}

// PRD Frontend §9.14 (Validacion.png, ValidacionExito.png, ValidacionYaUsado.png). FR-FE-STF-001.
// Pensada para la puerta: foco automático, Enter busca, resultado grande y el campo se limpia tras el ingreso.
export default function ValidacionIngreso() {
  const [codigoBuscado, setCodigoBuscado] = useState<string>()
  const ticket = useTicket(codigoBuscado) // consulta bajo demanda: solo corre con un código
  const usar = useUsarTicket()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<BuscarTicketForm>({
    resolver: zodResolver(BuscarTicketFormSchema),
    defaultValues: { ticketCode: '' },
  })

  function buscar({ ticketCode }: BuscarTicketForm) {
    usar.reset()
    // En la puerta el estado cambia rápido: si repiten el mismo código, se vuelve a consultar
    if (ticketCode === codigoBuscado) void ticket.refetch()
    else setCodigoBuscado(ticketCode)
  }

  function marcarUsado(ticketCode: string) {
    usar.mutate(ticketCode, { onSuccess: () => reset({ ticketCode: '' }) })
  }

  return (
    <Contenedor ancho="estrecho">
      <h1 className="mb-6 text-3xl font-bold">Validación de ingreso</h1>

      <form className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-start" onSubmit={handleSubmit(buscar)} noValidate>
        <Campo
          etiqueta="Código de ticket"
          placeholder="TKT-XXXXXX"
          autoComplete="off"
          autoFocus
          envolturaClassName="flex-1"
          className="font-mono text-lg"
          error={errors.ticketCode?.message}
          {...register('ticketCode')}
        />
        <Boton type="submit" className="sm:mt-7" cargando={ticket.isFetching}>
          Buscar
        </Boton>
      </form>

      {/* aria-live: el resultado se anuncia sin mover el foco del campo */}
      <div aria-live="polite" className="flex flex-col gap-4">
        {codigoBuscado === undefined ? null : ticket.isPending ? (
          <Cargando etiqueta="Buscando ticket…" />
        ) : ticket.isError ? (
          <Aviso tono="error" className="text-base">
            {mensajeBusqueda(ticket.error)}
          </Aviso>
        ) : (
          <Tarjeta className="flex flex-col gap-3 border-2 p-6">
            <div className="flex items-start justify-between gap-3">
              <h2 className="text-2xl font-bold">{ticket.data.eventName}</h2>
              <Badge tono={ticket.data.status} className="text-sm" />
            </div>
            <p className="text-lg">
              <CodigoTexto>{ticket.data.ticketCode}</CodigoTexto> · {ticket.data.type} · Titular: {ticket.data.userEmail}
            </p>
            {ticket.data.status === 'PAID' ? (
              <div className="flex justify-end">
                <Boton
                  className="px-6 py-3 text-base"
                  cargando={usar.isPending}
                  onClick={() => marcarUsado(ticket.data.ticketCode)}
                >
                  Marcar como usado
                </Boton>
              </div>
            ) : usar.isSuccess ? null : (
              <Aviso tono="advertencia">Este ticket no está habilitado para ingresar (estado {ticket.data.status}).</Aviso>
            )}
          </Tarjeta>
        )}

        {usar.isSuccess ? (
          <Aviso tono="exito" className="text-base">
            Ingreso registrado.
          </Aviso>
        ) : null}
        {/* 409: ya usado o no válido. Se muestra el mensaje del backend en tono de advertencia */}
        {usar.isError && !esApiError(usar.error, 401) ? (
          <Aviso tono={esApiError(usar.error, 409) ? 'advertencia' : 'error'} className="text-base">
            {mensajeDeError(usar.error)}
          </Aviso>
        ) : null}
      </div>
    </Contenedor>
  )
}
