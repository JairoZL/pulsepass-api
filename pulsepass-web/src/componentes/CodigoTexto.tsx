import { clsx } from 'clsx'
import type { ReactNode } from 'react'

// DES-05: los códigos (CMF-2026, TKT-8F2A1C) van en fuente monoespaciada
export function CodigoTexto({ children, className }: { children: ReactNode; className?: string }) {
  return <span className={clsx('font-mono', className)}>{children}</span>
}
