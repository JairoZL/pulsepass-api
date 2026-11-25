import { Outlet, ScrollRestoration } from 'react-router'
import { Footer } from './Footer'
import { Navbar } from './Navbar'

// PRD Frontend §9.2: Navbar · <Outlet/> · Footer. FE-010: enlace "Saltar al contenido" y <main id="contenido">
export default function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <a
        href="#contenido"
        className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:text-marca-700 focus:shadow"
      >
        Saltar al contenido
      </a>
      <Navbar />
      <main id="contenido" tabIndex={-1} className="flex-1 focus:outline-none">
        <Outlet />
      </main>
      <Footer />
      <ScrollRestoration />
    </div>
  )
}
