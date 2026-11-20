/**
 * rinominaPDF: pacchetti Windows con electron-builder, stesso metodo di callApi.
 *   npm run dist                       rinominaPDF-<v>.msi            per macchina (Program Files)
 *   TEO_PERUSER=1 npm run dist         rinominaPDF-peruser-<v>.msi    per utente (solo se il prodotto ha un UpgradeCode per utente)
 * L'UpgradeCode e' CONGELATO (tabella "Identita' dei prodotti" di teo.place\CLAUDE.md): non si cambia, non si riusa.
 */
const perUser = process.env.TEO_PERUSER === '1';
const UPGRADE_CODE = '6E50594E-22A6-3AC3-A7FB-48CFCA119568';
const UPGRADE_CODE_PERUSER = '';
if (perUser && !UPGRADE_CODE_PERUSER) {
  throw new Error('rinominaPDF non ha un UpgradeCode per utente congelato: va deciso prima (CLAUDE.md di teo.place)');
}

module.exports = {
  appId: 'place.teo.rinominapdf',
  productName: 'rinominaPDF',
  copyright: 'Copyright (c) 2026 teo.place',
  electronVersion: '43.8.4',
  directories: { buildResources: 'resources', output: 'out' },
  // l'interfaccia e' gia' dentro build/ui (Vite): le sue librerie (React, icone, xterm) NON vanno nel pacchetto
  files: [
    'main.js', 'package.json', 'build/**/*', 'resources/icons/**/*', 'node_modules/**/*',
    '!node_modules/**/{test,tests,docs,*.md,*.map}',
    '!node_modules/@tabler/**', '!node_modules/@xterm/**', '!node_modules/@teoplace/kit/{src/ui,demo,template,tests,docs,scripts}/**',
    // koffi porta i prebuild di tutte le piattaforme: qui serve solo Windows x64; pdf.js: niente build moderna ne' sorgenti
    '!node_modules/koffi/{src,doc,vendor,build/koffi/!(win32_x64)}/**', '!node_modules/pdfjs-dist/{web,build,types}/**', '!node_modules/@napi-rs/canvas-!(win32-x64-msvc)/**'
  ],
  // la libreria C del lettore (copia pubblica in lib/sorter-barcode): accanto all'app, fuori dall'asar
  extraResources: [{ from: 'lib/sorter-barcode', to: 'native', filter: ['sorter_barcode.dll', 'LEGGIMI.md', 'LICENSE'] }],
  asar: true,
  asarUnpack: ['**/*.node', '**/node-pty/**', '**/@lydell/**', '**/@napi-rs/**', '**/pdfjs-dist/legacy/**'],
  npmRebuild: false,
  win: {
    executableName: 'rinominaPDF',
    artifactName: perUser ? 'rinominaPDF-peruser-${version}.${ext}' : 'rinominaPDF-${version}.${ext}',
    icon: 'resources/icons/win/icon.ico',
    target: [{ target: 'msi', arch: ['x64'] }]
  },
  msi: {
    oneClick: false,
    perMachine: !perUser,
    upgradeCode: perUser ? UPGRADE_CODE_PERUSER : UPGRADE_CODE,
    createDesktopShortcut: true,
    createStartMenuShortcut: true,
    runAfterFinish: false
  }
};
