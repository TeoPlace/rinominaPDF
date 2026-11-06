import path from "path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// Sito prodotto di rinominaPDF, pubblicato su https://rinominapdf.teo.place/ → base "/".
// L'app sta al ROOT del sottodominio: la base NON va messa a un sottopercorso,
// altrimenti nginx risponde 404 sugli asset (il vhost serve dist/ al root).
//
// Nessun backend: rinominaPDF non si vende, e la pagina legge solo /scarica/versione.json.
export default defineConfig({
  base: "/",
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    proxy: {
      // In sviluppo si parla direttamente con il backend authenticator.
      "/api": {
        target: "http://localhost:3002",
      },
      // Manifest e installer stanno su nginx, non in dist: in sviluppo
      // si prendono dal sito pubblicato cosi' il bottone e' testabile.
      "/scarica": {
        target: "https://rinominapdf.teo.place",
        changeOrigin: true,
      },
    },
  },
});
