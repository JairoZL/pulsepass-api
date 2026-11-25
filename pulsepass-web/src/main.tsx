import '@fontsource/inter/400.css'
import '@fontsource/inter/500.css'
import '@fontsource/inter/600.css'
import '@fontsource/poppins/600.css'
import '@fontsource/poppins/700.css'
import './index.css'

import { QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router/dom'
import { AvisosProvider } from './componentes/Avisos'
import { AuthProvider } from './funcionalidades/auth/AuthContext'
import { crearQueryClient } from './lib/queryClient'
import { crearRouter } from './router'

// PRD Frontend §17: QueryClientProvider · AuthProvider · RouterProvider (de react-router/dom, FE-007)
const queryClient = crearQueryClient()
const router = crearRouter()

const raiz = document.getElementById('root')
if (!raiz) throw new Error('No existe el elemento #root en index.html')

createRoot(raiz).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AvisosProvider>
        <AuthProvider>
          <RouterProvider router={router} />
        </AuthProvider>
      </AvisosProvider>
    </QueryClientProvider>
  </StrictMode>,
)
