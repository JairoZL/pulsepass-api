// PRD Frontend NFR-FE-008: idioma es-CO con Intl (fechas y pesos colombianos)

const moneda = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
})

const fecha = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' })

const fechaHora = new Intl.DateTimeFormat('es-CO', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

export function formatearPrecio(valor: number): string {
  return moneda.format(valor)
}

// El backend envía LocalDateTime sin zona ("2026-12-12T20:00:00"): new Date lo interpreta como hora local
export function formatearFecha(iso: string): string {
  return fecha.format(new Date(iso))
}

export function formatearFechaHora(iso: string): string {
  return fechaHora.format(new Date(iso))
}

// "Andrea Ruiz" -> "AR"; "andrea" -> "AN"
export function iniciales(texto: string): string {
  const palabras = texto.trim().split(/\s+/).filter(Boolean)
  if (palabras.length >= 2) return `${palabras[0]?.[0] ?? ''}${palabras[1]?.[0] ?? ''}`.toUpperCase()
  return texto.trim().slice(0, 2).toUpperCase()
}
