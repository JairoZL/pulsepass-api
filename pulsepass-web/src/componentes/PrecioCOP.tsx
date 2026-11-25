import { formatearPrecio } from '../lib/formato'

// El precio siempre viene del backend (GAP-06); aquí solo se formatea en pesos colombianos
export function PrecioCOP({ valor }: { valor: number }) {
  return <span>{formatearPrecio(valor)}</span>
}
