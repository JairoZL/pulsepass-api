import type { ComponentProps, ReactNode } from 'react'

// PRD Frontend DES-01: íconos SVG de trazo (sin emojis). Son decorativos por defecto (aria-hidden):
// el texto que los acompaña es lo que lee el lector de pantalla.
interface PropsIcono extends ComponentProps<'svg'> {
  tamano?: number
}

function Icono({ tamano = 20, children, ...props }: PropsIcono & { children: ReactNode }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...props}
    >
      {children}
    </svg>
  )
}

export function IconoMusica(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M9 18V5l12-2v13" />
      <circle cx="6" cy="18" r="3" />
      <circle cx="18" cy="16" r="3" />
    </Icono>
  )
}

export function IconoCalendario(props: PropsIcono) {
  return (
    <Icono {...props}>
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M16 2v4M8 2v4M3 10h18" />
    </Icono>
  )
}

export function IconoUbicacion(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 0 1 16 0Z" />
      <circle cx="12" cy="10" r="3" />
    </Icono>
  )
}

export function IconoFlechaDerecha(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M5 12h14M12 5l7 7-7 7" />
    </Icono>
  )
}

export function IconoFlechaIzquierda(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M19 12H5M12 19l-7-7 7-7" />
    </Icono>
  )
}

export function IconoMenu(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M4 6h16M4 12h16M4 18h16" />
    </Icono>
  )
}

export function IconoCerrar(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M18 6 6 18M6 6l12 12" />
    </Icono>
  )
}

export function IconoAlerta(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="m21.73 18-8-14a2 2 0 0 0-3.46 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" />
      <path d="M12 9v4M12 17h.01" />
    </Icono>
  )
}

export function IconoCheck(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M20 6 9 17l-5-5" />
    </Icono>
  )
}

export function IconoCheckCirculo(props: PropsIcono) {
  return (
    <Icono {...props}>
      <circle cx="12" cy="12" r="10" />
      <path d="m9 12 2 2 4-4" />
    </Icono>
  )
}

export function IconoRayo(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M13 2 3 14h9l-1 8 10-12h-9l1-8Z" />
    </Icono>
  )
}

export function IconoBandeja(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M22 12h-6l-2 3h-4l-2-3H2" />
      <path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11Z" />
    </Icono>
  )
}

export function IconoChevronAbajo(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="m6 9 6 6 6-6" />
    </Icono>
  )
}

export function IconoBuscar(props: PropsIcono) {
  return (
    <Icono {...props}>
      <circle cx="11" cy="11" r="8" />
      <path d="m21 21-4.3-4.3" />
    </Icono>
  )
}

export function IconoOjo(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
      <circle cx="12" cy="12" r="3" />
    </Icono>
  )
}

export function IconoOjoTachado(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24" />
      <path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68" />
      <path d="M6.61 6.61A13.53 13.53 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61" />
      <path d="m2 2 20 20" />
    </Icono>
  )
}

export function IconoMas(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M12 5v14M5 12h14" />
    </Icono>
  )
}

export function IconoCopiar(props: PropsIcono) {
  return (
    <Icono {...props}>
      <rect x="9" y="9" width="13" height="13" rx="2" />
      <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
    </Icono>
  )
}

export function IconoTicket(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z" />
      <path d="M13 5v2M13 17v2M13 11v2" />
    </Icono>
  )
}

export function IconoLuna(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z" />
    </Icono>
  )
}

export function IconoSol(props: PropsIcono) {
  return (
    <Icono {...props}>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
    </Icono>
  )
}

export function IconoEstrella(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="m12 2 3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2Z" />
    </Icono>
  )
}

export function IconoMuseo(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M3 22h18M6 18v-7M10 18v-7M14 18v-7M18 18v-7M12 2l9 5H3l9-5Z" />
    </Icono>
  )
}

export function IconoTrofeo(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6M18 9h1.5a2.5 2.5 0 0 0 0-5H18" />
      <path d="M4 22h16M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22" />
      <path d="M18 2H6v7a6 6 0 0 0 12 0V2Z" />
    </Icono>
  )
}

export function IconoChip(props: PropsIcono) {
  return (
    <Icono {...props}>
      <rect x="4" y="4" width="16" height="16" rx="2" />
      <rect x="9" y="9" width="6" height="6" />
      <path d="M15 2v2M15 20v2M2 15h2M2 9h2M20 15h2M20 9h2M9 2v2M9 20v2" />
    </Icono>
  )
}

export function IconoBirrete(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M22 10 12 5 2 10l10 5 10-5Z" />
      <path d="M6 12v5c3 3 9 3 12 0v-5" />
    </Icono>
  )
}

export function IconoUsuario(props: PropsIcono) {
  return (
    <Icono {...props}>
      <circle cx="12" cy="8" r="4" />
      <path d="M20 21a8 8 0 0 0-16 0" />
    </Icono>
  )
}

export function IconoMonitor(props: PropsIcono) {
  return (
    <Icono {...props}>
      <rect x="2" y="3" width="20" height="14" rx="2" />
      <path d="M8 21h8M12 17v4" />
    </Icono>
  )
}

export function IconoBalon(props: PropsIcono) {
  return (
    <Icono {...props}>
      <circle cx="12" cy="12" r="10" />
      <path d="M12 2v20M2 12h20" />
    </Icono>
  )
}

export function IconoEdificio(props: PropsIcono) {
  return (
    <Icono {...props}>
      <path d="M3 21h18M5 21V7l7-4 7 4v14" />
      <path d="M9 21v-6h6v6" />
    </Icono>
  )
}
