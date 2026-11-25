// Modo oscuro (FR-FE-GEN-004). Preferencia de interfaz (no es un dato sensible): localStorage está bien aquí.
// El token NUNCA va en localStorage (§24.6); esto es solo "claro" u "oscuro".
const CLAVE = 'pulsepass.tema'

export type Tema = 'claro' | 'oscuro'

export function temaActual(): Tema {
  return document.documentElement.classList.contains('dark') ? 'oscuro' : 'claro'
}

export function aplicarTema(tema: Tema): void {
  document.documentElement.classList.toggle('dark', tema === 'oscuro')
  try {
    localStorage.setItem(CLAVE, tema)
  } catch {
    // Navegación privada sin almacenamiento: el tema igual se aplica en esta visita
  }
}
