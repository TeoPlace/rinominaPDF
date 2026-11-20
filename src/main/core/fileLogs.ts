import fs from 'node:fs';
import path from 'node:path';

const pad = (n: number, w = 2) => String(n).padStart(w, '0');
/** Come LocalDateTime.now().toString() della 1.x: 2026-11-20T17:43:21.123 */
const stamp = (d = new Date()) =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}`;

/**
 * Log su file nella cartella di output (stessi nomi e stesso formato della 1.x):
 * LOG_OK.txt, LOG_ERRORI.txt, LOG_DATAMATRIX_NON_LETTI.txt, una riga per PDF: `<ora>;<file>;<dettaglio>`.
 */
export class FileLogs {
  private readonly okLog: string;
  private readonly errorLog: string;
  private readonly unreadLog: string;

  constructor(outputFolder: string) {
    fs.mkdirSync(outputFolder, { recursive: true });
    this.okLog = path.join(outputFolder, 'LOG_OK.txt');
    this.errorLog = path.join(outputFolder, 'LOG_ERRORI.txt');
    this.unreadLog = path.join(outputFolder, 'LOG_DATAMATRIX_NON_LETTI.txt');
    for (const f of [this.okLog, this.errorLog, this.unreadLog]) if (!fs.existsSync(f)) fs.writeFileSync(f, '');
  }

  ok(sourceFile: string, newName: string) {
    this.append(this.okLog, `${sourceFile};${newName}`);
  }

  unread(sourceFile: string, reason: string) {
    this.append(this.unreadLog, `${sourceFile};${reason}`);
  }

  error(sourceFile: string, e: unknown) {
    const text = e instanceof Error ? `${e.name}: ${e.message}\r\n${e.stack ?? ''}` : String(e);
    this.append(this.errorLog, `${sourceFile};${text}`);
  }

  private append(file: string, line: string) {
    try {
      fs.appendFileSync(file, `${stamp()};${line}\r\n`);
    } catch {
      // un log che non si scrive non ferma il lavoro: il registro a schermo resta
    }
  }
}
