import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../auth/contexto'
import { clavesEventos } from '../eventos/hooks'
import {
  cancelarTicket,
  comprarTicket,
  obtenerTicket,
  obtenerTicketsPagados,
  obtenerTicketsPorEmail,
  usarTicket,
  type DatosCompra,
} from './api'
import type { TicketResponse } from './tipos'

const QUINCE_SEGUNDOS = 15_000

// PRD Frontend §14.1
export const clavesTickets = {
  usuario: (email: string) => ['tickets', 'usuario', email] as const,
  ticket: (ticketCode: string) => ['ticket', ticketCode] as const,
  pagados: (eventCode: string) => ['tickets', 'pagados', eventCode] as const,
}

// El email sale SIEMPRE de la sesión (GAP-03 y §24.3)
function useEmailSesion(): string | undefined {
  const { sesion } = useAuth()
  return sesion.estado === 'autenticado' ? sesion.usuario.email : undefined
}

export function useMisTickets() {
  const email = useEmailSesion()
  return useQuery({
    queryKey: clavesTickets.usuario(email ?? ''),
    queryFn: () => obtenerTicketsPorEmail(email ?? ''),
    enabled: Boolean(email),
    staleTime: QUINCE_SEGUNDOS,
  })
}

export function useTicket(ticketCode: string | undefined) {
  return useQuery({
    queryKey: clavesTickets.ticket(ticketCode ?? ''),
    queryFn: () => obtenerTicket(ticketCode ?? ''),
    enabled: Boolean(ticketCode),
    staleTime: QUINCE_SEGUNDOS,
  })
}

export function useTicketsPagadosPorEvento(eventCode: string | undefined) {
  return useQuery({
    queryKey: clavesTickets.pagados(eventCode ?? ''),
    queryFn: () => obtenerTicketsPagados(eventCode ?? ''),
    enabled: Boolean(eventCode),
    staleTime: QUINCE_SEGUNDOS,
  })
}

// §14.2: comprar invalida mis tickets y el evento. Nunca se reintenta (evita compras duplicadas)
export function useComprarTicket() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (datos: DatosCompra) => comprarTicket(datos),
    onSuccess: (ticket) => {
      queryClient.setQueryData(clavesTickets.ticket(ticket.ticketCode), ticket)
      void queryClient.invalidateQueries({ queryKey: clavesTickets.usuario(ticket.userEmail) })
      void queryClient.invalidateQueries({ queryKey: clavesEventos.evento(ticket.eventCode) })
    },
  })
}

// Tras cancelar o usar: el ticket actualizado reemplaza al de la caché y se refrescan las listas
function alActualizarTicket(queryClient: ReturnType<typeof useQueryClient>, ticket: TicketResponse) {
  queryClient.setQueryData(clavesTickets.ticket(ticket.ticketCode), ticket)
  void queryClient.invalidateQueries({ queryKey: clavesTickets.usuario(ticket.userEmail) })
  void queryClient.invalidateQueries({ queryKey: clavesTickets.pagados(ticket.eventCode) })
}

export function useCancelarTicket() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (ticketCode: string) => cancelarTicket(ticketCode),
    onSuccess: (ticket) => alActualizarTicket(queryClient, ticket),
  })
}

export function useUsarTicket() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (ticketCode: string) => usarTicket(ticketCode),
    onSuccess: (ticket) => alActualizarTicket(queryClient, ticket),
  })
}
