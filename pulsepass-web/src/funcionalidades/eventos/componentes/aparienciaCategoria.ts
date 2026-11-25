import type { ComponentType } from 'react'
import {
  IconoBalon,
  IconoBirrete,
  IconoEstrella,
  IconoMonitor,
  IconoMuseo,
  IconoMusica,
} from '../../../componentes/Iconos'
import type { EventCategory } from '../tipos'

// PRD Frontend DES-02: la API no entrega imágenes; cada categoría tiene un panel de color con ícono.
// Mapa tipado con clases COMPLETAS (FE-008): agregar una categoría al enum obliga a completar este mapa.
interface Apariencia {
  panel: string
  texto: string
  Icono: ComponentType<{ tamano?: number }>
}

export const APARIENCIA_CATEGORIA: Record<EventCategory, Apariencia> = {
  MUSIC: {
    panel: 'bg-indigo-100 dark:bg-indigo-950',
    texto: 'text-indigo-800 dark:text-indigo-300',
    Icono: IconoMusica,
  },
  TECHNOLOGY: { panel: 'bg-sky-100 dark:bg-sky-950', texto: 'text-sky-800 dark:text-sky-300', Icono: IconoMonitor },
  SPORTS: {
    panel: 'bg-emerald-100 dark:bg-emerald-950',
    texto: 'text-emerald-800 dark:text-emerald-300',
    Icono: IconoBalon,
  },
  EDUCATION: {
    panel: 'bg-amber-100 dark:bg-amber-950',
    texto: 'text-amber-800 dark:text-amber-300',
    Icono: IconoBirrete,
  },
  CULTURE: { panel: 'bg-rose-100 dark:bg-rose-950', texto: 'text-rose-800 dark:text-rose-300', Icono: IconoMuseo },
  ENTERTAINMENT: {
    panel: 'bg-violet-100 dark:bg-violet-950',
    texto: 'text-violet-800 dark:text-violet-300',
    Icono: IconoEstrella,
  },
}
