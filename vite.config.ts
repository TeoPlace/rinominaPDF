import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

// L'interfaccia sta in src/ui e finisce in build/ui (caricata da file:// da Electron).
export default defineConfig({
  root: 'src/ui',
  base: './',
  plugins: [react()],
  resolve: { dedupe: ['react', 'react-dom'] },
  server: { port: 5174, strictPort: true, fs: { allow: [path.resolve('.'), path.resolve('../teo-kit')] } },
  build: { outDir: '../../build/ui', emptyOutDir: true, chunkSizeWarningLimit: 4000 }
});
