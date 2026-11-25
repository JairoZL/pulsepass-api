import type { ReactNode } from 'react'

// PRD Frontend §9.1 y §9.15: tabla genérica tipada. En escritorio es <table>; en móvil (< md)
// cada fila se muestra como una tarjeta con "encabezado: valor" (DES y MovilPagados.png).
export interface ColumnaTabla<T> {
  encabezado: string
  celda: (fila: T) => ReactNode
}

interface PropsTabla<T> {
  columnas: ColumnaTabla<T>[]
  filas: T[]
  claveFila: (fila: T) => string
  etiqueta: string // nombre accesible de la tabla
}

export function Tabla<T>({ columnas, filas, claveFila, etiqueta }: PropsTabla<T>) {
  return (
    <>
      <div className="hidden overflow-x-auto rounded-xl border border-slate-200 bg-white shadow-sm md:block dark:border-slate-800 dark:bg-slate-900">
        <table className="w-full text-left text-sm" aria-label={etiqueta}>
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500 dark:bg-slate-800 dark:text-slate-400">
            <tr>
              {columnas.map((columna) => (
                <th key={columna.encabezado} scope="col" className="px-4 py-3 font-semibold">
                  {columna.encabezado}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
            {filas.map((fila) => (
              <tr key={claveFila(fila)}>
                {columnas.map((columna) => (
                  <td key={columna.encabezado} className="px-4 py-3">
                    {columna.celda(fila)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ul className="flex flex-col gap-3 md:hidden" aria-label={etiqueta}>
        {filas.map((fila) => (
          <li
            key={claveFila(fila)}
            className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-900"
          >
            <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
              {columnas.map((columna) => (
                <div key={columna.encabezado} className="contents">
                  <dt className="text-slate-500 dark:text-slate-400">{columna.encabezado}</dt>
                  <dd className="text-right">{columna.celda(fila)}</dd>
                </div>
              ))}
            </dl>
          </li>
        ))}
      </ul>
    </>
  )
}
