import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { AreaTexto } from '../componentes/AreaTexto'
import { Aviso } from '../componentes/Aviso'
import { Boton } from '../componentes/Boton'
import { Campo } from '../componentes/Campo'
import { Contenedor } from '../componentes/Contenedor'
import { EnlaceVolver } from '../componentes/EnlaceVolver'
import { clasesBoton } from '../componentes/estilos'
import { Selector } from '../componentes/Selector'
import { Tarjeta } from '../componentes/Tarjeta'
import { useAvisos } from '../componentes/avisosContexto'
import { agregarEventoCreado } from '../funcionalidades/eventos/creadosSesion'
import { useCrearEvento } from '../funcionalidades/eventos/hooks'
import { CrearEventoFormSchema, EventCategorySchema, type CrearEventoForm } from '../funcionalidades/eventos/tipos'
import { useSedesActivas } from '../funcionalidades/sedes/hooks'
import { esApiError, mensajeDeError } from '../lib/errores'
import { aplicarErroresServidor } from '../lib/formularios'
import { RUTAS } from '../lib/rutas'

const CAMPOS = ['eventCode', 'name', 'description', 'category', 'eventDate', 'minimumAge', 'venueCode'] as const
const MAXIMO_DESCRIPCION = 500

// PRD Frontend §9.17 (AdminCrear.png). FR-FE-ADM-002
export default function AdminCrearEvento() {
  const navigate = useNavigate()
  const { mostrarAviso } = useAvisos()
  const crear = useCrearEvento()
  const sedes = useSedesActivas()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<CrearEventoForm>({
    resolver: zodResolver(CrearEventoFormSchema),
    defaultValues: {
      eventCode: '',
      name: '',
      description: '',
      category: 'MUSIC',
      eventDate: '',
      minimumAge: 0,
      venueCode: '',
    },
  })
  const descripcion = useWatch({ control, name: 'description' })

  function enviar(datos: CrearEventoForm) {
    setErrorGeneral(null)
    // El código se guarda en mayúsculas, como los del escenario (CMF-2026)
    crear.mutate(
      { ...datos, eventCode: datos.eventCode.toUpperCase() },
      {
        onSuccess: (evento) => {
          agregarEventoCreado(evento)
          mostrarAviso('exito', 'Evento creado como borrador.')
          navigate(RUTAS.adminEvento(evento.eventCode))
        },
        onError: (error) => {
          if (esApiError(error, 403)) navigate(RUTAS.sinPermisos)
          else if (esApiError(error, 400)) setErrorGeneral(aplicarErroresServidor(error.details, setError, CAMPOS))
          else if (!esApiError(error, 401)) setErrorGeneral(mensajeDeError(error)) // 409: código duplicado, fecha pasada…
        },
      },
    )
  }

  return (
    <Contenedor ancho="estrecho">
      <EnlaceVolver a={RUTAS.adminEventos} texto="Eventos" />
      <h1 className="mb-6 text-3xl font-bold">Nuevo evento</h1>
      <Tarjeta className="p-6">
        <form className="flex flex-col gap-4" onSubmit={handleSubmit(enviar)} noValidate>
          {errorGeneral ? <Aviso tono="error">{errorGeneral}</Aviso> : null}
          <Campo
            etiqueta="Código del evento"
            placeholder="CMF-2026"
            className="font-mono uppercase"
            error={errors.eventCode?.message}
            {...register('eventCode')}
          />
          <Campo etiqueta="Nombre" error={errors.name?.message} {...register('name')} />
          <AreaTexto
            etiqueta="Descripción"
            rows={4}
            maxLength={MAXIMO_DESCRIPCION}
            ayuda={`${descripcion.length}/${MAXIMO_DESCRIPCION}`}
            error={errors.description?.message}
            {...register('description')}
          />
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Selector etiqueta="Categoría" error={errors.category?.message} {...register('category')}>
              {EventCategorySchema.options.map((categoria) => (
                <option key={categoria} value={categoria}>
                  {categoria}
                </option>
              ))}
            </Selector>
            <Campo
              etiqueta="Edad mínima"
              type="number"
              min={0}
              inputMode="numeric"
              error={errors.minimumAge?.message}
              {...register('minimumAge', { valueAsNumber: true })}
            />
          </div>
          {/* datetime-local entrega "2026-12-12T20:00", el formato LocalDateTime que espera el backend */}
          <Campo etiqueta="Fecha y hora" type="datetime-local" error={errors.eventDate?.message} {...register('eventDate')} />
          <Selector
            etiqueta="Sede"
            error={errors.venueCode?.message}
            {...register('venueCode')}
          >
            <option value="">{sedes.isPending ? 'Cargando sedes…' : 'Elige una sede'}</option>
            {(sedes.data ?? []).map((sede) => (
              <option key={sede.code} value={sede.code}>
                {sede.name} ({sede.code})
              </option>
            ))}
          </Selector>
          {sedes.isError ? <Aviso tono="error">{mensajeDeError(sedes.error)}</Aviso> : null}

          <div className="flex justify-end gap-3 pt-2">
            <Link to={RUTAS.adminEventos} className={clasesBoton('secundario')}>
              Cancelar
            </Link>
            <Boton type="submit" cargando={crear.isPending}>
              Crear evento
            </Boton>
          </div>
        </form>
      </Tarjeta>
    </Contenedor>
  )
}
