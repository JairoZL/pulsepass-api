import type { ComponentType } from 'react'
import { createBrowserRouter, type RouteObject } from 'react-router'
import { Cargando } from './componentes/Estados'
import Layout from './layouts/Layout'
import RutaPorRol from './layouts/RutaPorRol'
import RutaProtegida from './layouts/RutaProtegida'
import SoloAnonimo from './layouts/SoloAnonimo'
import ErrorPagina from './pages/ErrorPagina'
import Inicio from './pages/Inicio'

// PRD Frontend §8.2 y FE-012: toda página se carga de forma perezosa (lazy) salvo Layout e Inicio.
// Así la primera carga no descarga el código de admin ni de staff (NFR-FE-003).
function perezosa(cargar: () => Promise<{ default: ComponentType }>): Pick<RouteObject, 'lazy'> {
  return {
    lazy: async () => ({ Component: (await cargar()).default }),
  }
}

// Las rutas se exportan para que las pruebas usen las mismas con createMemoryRouter
export const rutas: RouteObject[] = [
  {
    path: '/',
    Component: Layout,
    errorElement: <ErrorPagina />,
    HydrateFallback: () => <Cargando />,
    children: [
      {
        // Ruta sin path: los errores de cualquier página se muestran DENTRO del Layout (con navbar)
        errorElement: <ErrorPagina />,
        children: [
          { index: true, Component: Inicio },
          { path: 'eventos', ...perezosa(() => import('./pages/CatalogoEventos')) },
          { path: 'eventos/:eventCode', ...perezosa(() => import('./pages/DetalleEvento')) },
          { path: 'artistas', ...perezosa(() => import('./pages/ListaArtistas')) },
          { path: 'artistas/:id', ...perezosa(() => import('./pages/DetalleArtista')) },
          { path: 'sedes', ...perezosa(() => import('./pages/ListaSedes')) },
          { path: 'sedes/:code', ...perezosa(() => import('./pages/DetalleSede')) },

          // Solo sin sesión
          {
            Component: SoloAnonimo,
            children: [
              { path: 'login', ...perezosa(() => import('./pages/Login')) },
              { path: 'registro', ...perezosa(() => import('./pages/Registro')) },
            ],
          },

          // Con sesión (cualquier rol)
          {
            Component: RutaProtegida,
            children: [
              { path: 'cuenta', ...perezosa(() => import('./pages/Cuenta')) },
              {
                element: <RutaPorRol roles={['USER']} />,
                children: [
                  { path: 'mis-tickets', ...perezosa(() => import('./pages/MisTickets')) },
                  { path: 'mis-tickets/:ticketCode', ...perezosa(() => import('./pages/DetalleTicket')) },
                  {
                    path: 'compra/confirmacion/:ticketCode',
                    ...perezosa(() => import('./pages/ConfirmacionCompra')),
                  },
                ],
              },
              {
                element: <RutaPorRol roles={['STAFF', 'ADMIN']} />,
                children: [
                  { path: 'staff/validacion', ...perezosa(() => import('./pages/ValidacionIngreso')) },
                  // :eventCode? opcional: sin código, la página pide el evento (enlace "Tickets pagados" de la navbar)
                  {
                    path: 'staff/eventos/:eventCode?/tickets',
                    ...perezosa(() => import('./pages/TicketsPagadosEvento')),
                  },
                ],
              },
              {
                element: <RutaPorRol roles={['ADMIN']} />,
                children: [
                  { path: 'admin/eventos', ...perezosa(() => import('./pages/AdminEventos')) },
                  { path: 'admin/eventos/nuevo', ...perezosa(() => import('./pages/AdminCrearEvento')) },
                  { path: 'admin/eventos/:eventCode', ...perezosa(() => import('./pages/AdminGestionEvento')) },
                ],
              },
            ],
          },

          { path: '403', ...perezosa(() => import('./pages/SinPermisos')) },
          { path: '*', ...perezosa(() => import('./pages/NoEncontrada')) },
        ],
      },
    ],
  },
]

export function crearRouter() {
  return createBrowserRouter(rutas)
}
