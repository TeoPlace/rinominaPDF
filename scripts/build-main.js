// Impacchetta il processo principale del prodotto (TypeScript) in build/main. Il kit e le librerie esterne restano
// fuori dal bundle: si caricano da node_modules, come in sviluppo. I worker (src/main/worker/*.ts) sono entry a parte.
const esbuild = require('esbuild');
const fs = require('node:fs');

const entries = ['src/main/app.ts'];
if (fs.existsSync('src/main/worker')) {
  for (const f of fs.readdirSync('src/main/worker')) if (f.endsWith('.ts')) entries.push('src/main/worker/' + f);
}
esbuild.buildSync({
  entryPoints: entries,
  outdir: 'build/main',
  outbase: 'src/main',
  bundle: true,
  platform: 'node',
  target: 'node24',
  format: 'cjs',
  external: ['electron', '@teoplace/kit', '@teoplace/kit/*', 'koffi', 'pdfjs-dist', 'pdfjs-dist/*', '@napi-rs/canvas', 'node:*'],
  sourcemap: true,
  logLevel: 'info'
});
