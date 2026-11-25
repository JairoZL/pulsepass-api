import { zodResolver } from '@hookform/resolvers/zod'
import { clsx } from 'clsx'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import { Aviso } from '../../../componentes/Aviso'
import { Boton } from '../../../componentes/Boton'
import { clasesBoton } from '../../../componentes/estilos'
import { Tarjeta } from '../../../componentes/Tarjeta'
import { esApiError, mensajeDeError } from '../../../lib/errores'
import { RUTAS } from '../../../lib/rutas'
import { useAuth } from '../../auth/contexto'
import { rolPrincipal } from '../../auth/roles'
import type { EventResponse, EventStatus } from '../../eventos/tipos'
import { useComprarTicket } from '../hooks'
import { CompraFormSchema, TicketTypeSchema, type CompraForm } from '../tipos'
import { ModalConfirmarCompra } from './ModalConfirmarCompra'

// Texto informativo de por qué no se puede comprar. NO es una regla de negocio: el estado viene
// del backend y solo se usa para no ofrecer un botón que el servidor va a rechazar (FR-FE-EVT-005)
const MOTIVO_NO_DISPONIBLE: Record<EventStatus, string> = {
  DRAFT: 'El evento aún no está publicado.',
  PUBLISHED: '',
  SOLD_OUT: 'Las entradas de este evento están agotadas.',
  CANCELLED: 'Este evento fue cancelado.',
  FINISHED: 'Este evento ya terminó.',
}

// PRD Frontend §9.5: panel de compra. Anónimo -> inicia sesión; STAFF/ADMIN -> oculto (la API
// solo deja comprar a USER, §12.4); USER -> elige tipo y confirma en un modal
export function PanelCompra({ evento }: { evento: EventResponse }) {
  const { sesion } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [modalAbierto, setModalAbierto] = useState(false)
  const compra = useComprarTicket()
  const { register, handleSubmit, watch } = useForm<CompraForm>({
    resolver: zodResolver(CompraFormSchema),
    defaultValues: { type: 'GENERAL' },
  })

  if (sesion.estado === 'cargando') return null
  if (sesion.estado === 'autenticado' && rolPrincipal(sesion.usuario.roles) !== 'USER') return null

  const disponible = evento.status === 'PUBLISHED'
  const tipo = watch('type')

  // mutateAsync (y no callbacks de mutate): si llega un 401, lib/http cierra la sesión y la caché se
  // limpia; la promesa igual se rechaza aquí y podemos llevar a la persona al login
  async function confirmar() {
    if (sesion.estado !== 'autenticado') return
    try {
      const ticket = await compra.mutateAsync({ userEmail: sesion.usuario.email, eventCode: evento.eventCode, type: tipo })
      navigate(RUTAS.confirmacionCompra(ticket.ticketCode))
    } catch (error) {
      // 401: se vuelve a este evento tras iniciar sesión (UC-FE-11). Otros errores se ven en el modal
      if (esApiError(error, 401)) {
        navigate(RUTAS.login, { state: { desde: location.pathname, motivo: 'expirada' } })
      }
    }
  }

  return (
    <Tarjeta className="flex flex-col gap-4 p-6 lg:sticky lg:top-24">
      <h2 className="text-lg font-semibold">Entradas</h2>

      {!disponible ? <Aviso tono="advertencia">{MOTIVO_NO_DISPONIBLE[evento.status]}</Aviso> : null}

      {sesion.estado === 'anonimo' ? (
        disponible ? (
          <>
            <p className="text-sm text-slate-600 dark:text-slate-400">Necesitas una cuenta para comprar entradas.</p>
            <Link
              to={RUTAS.login}
              state={{ desde: location.pathname }}
              className={clasesBoton('primario', 'w-full')}
            >
              Inicia sesión para comprar
            </Link>
          </>
        ) : null
      ) : (
        <form
          id="form-compra"
          className="flex flex-col gap-4"
          onSubmit={handleSubmit(() => {
            compra.reset()
            setModalAbierto(true)
          })}
        >
          <fieldset className="flex flex-col gap-2" disabled={!disponible}>
            <legend className="mb-2 text-sm text-slate-600 dark:text-slate-400">Tipo de entrada</legend>
            {TicketTypeSchema.options.map((opcion) => (
              <label
                key={opcion}
                className={clsx(
                  'flex cursor-pointer items-center gap-3 rounded-lg border border-slate-300 px-4 py-3 font-medium dark:border-slate-700',
                  'has-checked:border-marca-600 has-checked:bg-marca-50 dark:has-checked:bg-slate-800',
                  'has-focus-visible:outline-2 has-focus-visible:outline-marca-600',
                )}
              >
                <input type="radio" value={opcion} {...register('type')} className="h-4 w-4 accent-marca-600" />
                {opcion}
              </label>
            ))}
          </fieldset>
          <Boton type="submit" disabled={!disponible} className="w-full">
            Comprar entrada
          </Boton>
          <p className="text-xs text-slate-500 dark:text-slate-400">El precio lo define el sistema al confirmar.</p>

          {/* DES-07: en móvil la acción queda fija abajo. Envía el mismo formulario */}
          {disponible ? (
            <div className="fixed inset-x-0 bottom-0 z-20 border-t border-slate-200 bg-white p-4 md:hidden dark:border-slate-800 dark:bg-slate-950">
              <Boton type="submit" className="w-full">
                Comprar
              </Boton>
            </div>
          ) : null}

          <ModalConfirmarCompra
            abierto={modalAbierto}
            nombreEvento={evento.name}
            tipo={tipo}
            email={sesion.usuario.email}
            cargando={compra.isPending}
            error={compra.isError && !esApiError(compra.error, 401) ? mensajeDeError(compra.error) : undefined}
            onConfirmar={() => void confirmar()}
            onCerrar={() => setModalAbierto(false)}
          />
        </form>
      )}
    </Tarjeta>
  )
}
