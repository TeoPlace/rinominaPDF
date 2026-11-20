import { describe, expect, it } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';

const root = path.join(__dirname, '..');
const read = (f: string) => fs.readFileSync(path.join(root, f), 'utf8');
// congelato: e' quello degli MSI gia' distribuiti (tabella "Identita' dei prodotti" di STANDARD.md)
const UPGRADE_CODE = '6E50594E-22A6-3AC3-A7FB-48CFCA119568';

describe("identita' del prodotto (equivalente di VersionTest)", () => {
  const pkg = JSON.parse(read('package.json'));

  it('versione a tre campi, maggiore della 1.0.5 distribuita', () => {
    expect(pkg.version).toMatch(/^\d+\.\d+\.\d+$/);
    const [a, b, c] = pkg.version.split('.').map(Number);
    expect(a * 1e6 + b * 1e3 + c).toBeGreaterThan(1 * 1e6 + 0 * 1e3 + 5);
  });

  it("l'UpgradeCode e' quello congelato in main.js, nel pacchetto, nello script e nella tabella di STANDARD.md", () => {
    for (const f of ['main.js', 'electron-builder-config.js', 'build-msi.ps1']) expect(read(f)).toContain(UPGRADE_CODE);
    expect(read('STANDARD.md')).toMatch(new RegExp('rinominaPDF[^\\n]*' + UPGRADE_CODE));
  });

  it('il nome non cambia: rinominaPDF.exe, dati in %LOCALAPPDATA%\\rinominaPDF, niente MCP', () => {
    expect(pkg.productName).toBe('rinominaPDF');
    expect(read('main.js')).toContain("NAME: 'rinominaPDF'");
    expect(read('main.js')).toContain("EXE: 'rinominaPDF.exe'");
    expect(read('main.js')).toContain('MCP_PORT: 0');
    expect(read('electron-builder-config.js')).toContain("executableName: 'rinominaPDF'");
  });

  it('niente Java e niente jpackage fuori da legacy-java', () => {
    expect(fs.existsSync(path.join(root, 'pom.xml'))).toBe(false);
    expect(fs.existsSync(path.join(root, 'src', 'main', 'java'))).toBe(false);
    expect(read("build-msi.ps1")).not.toMatch(/Invoke-Native "(jpackage|mvn)/);
  });

  it('la libreria viaggia nel pacchetto ed e quella dichiarata nel LEGGIMI (sha256 della dll)', () => {
    expect(read('electron-builder-config.js')).toContain("to: 'native'");
    const dll = fs.readFileSync(path.join(root, 'lib', 'sorter-barcode', 'sorter_barcode.dll'));
    const sha = crypto.createHash('sha256').update(dll).digest('hex');
    expect(read('lib/sorter-barcode/LEGGIMI.md')).toContain(sha);
  });
});
