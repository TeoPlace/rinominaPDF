import { useEffect, useRef } from 'react';

const W = 640;
const H = 150;

/**
 * Animazione leggera stile arcade anni '90 (portata dalla RetroAnimation della 1.x): un «documento» pixelato viaggia dalla
 * cartella di input allo scanner (che lampeggia leggendo il barcode) e poi alla cartella di output, su sfondo nero con
 * scanline. Un solo requestAnimationFrame limitato a ~30 fps su un canvas; gira solo finche' la vista e' a schermo.
 */
export function Retro() {
  const ref = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    const c = ref.current!;
    const g = c.getContext('2d')!;
    let raf = 0;
    let last = 0;
    let t = 0;

    const folder = (x: number, y: number, color: string, dark: string) => {
      g.fillStyle = color;
      g.fillRect(x, y + 6, 36, 26);
      g.fillRect(x, y, 16, 8);
      g.fillStyle = '#000';
      g.fillRect(x + 3, y + 10, 30, 18);
      g.fillStyle = dark;
      g.fillRect(x + 3, y + 10, 30, 4);
    };
    const scanner = (x: number, y: number) => {
      g.fillStyle = '#888';
      g.fillRect(x, y, 44, 10);
      g.fillStyle = '#ff2079';
      g.fillRect(x + 18, y + 3, 8, 4);
      g.fillStyle = '#555';
      g.fillRect(x + 2, y + 10, 4, 14);
      g.fillRect(x + 38, y + 10, 4, 14);
    };
    const doc = (x: number, y: number, progress: number) => {
      g.fillStyle = '#fff';
      g.fillRect(x, y, 20, 26);
      g.fillStyle = '#222';
      for (let i = 0; i < 3; i++) g.fillRect(x + 3, y + 4 + i * 5, 14, 2);
      if (progress > 0.5) {
        g.fillStyle = '#000';
        for (let i = 0; i < 7; i++) g.fillRect(x + 3 + i * 2, y + 19, i % 3 === 0 ? 1.5 : 1, 5);
      }
    };
    const draw = () => {
      g.fillStyle = '#000';
      g.fillRect(0, 0, W, H);
      const progress = (t % 4) / 4; // 4 s per documento
      const scanX = W / 2;
      folder(40, H / 2 - 18, '#39ff14', '#23990c');
      scanner(scanX - 22, H / 2 - 34);
      folder(W - 80, H / 2 - 18, '#00bfff', '#007399');
      const docX = 70 + progress * (W - 170);
      const docY = H / 2 - 14 + Math.sin(t * 6) * 2;
      doc(docX, docY, progress);
      if (Math.abs(docX + 10 - scanX) < 26 && Math.floor(t * 10) % 2 === 0) {
        g.strokeStyle = '#ff2079';
        g.lineWidth = 2;
        g.beginPath();
        g.moveTo(scanX, H / 2 - 30);
        g.lineTo(docX + 10, docY + 8);
        g.stroke();
      }
      g.fillStyle = 'rgba(57,255,20,.35)';
      for (let i = 1; i <= 4; i++) g.fillRect(docX - i * 9, docY + 10, 4, 4);
      g.fillStyle = 'rgba(255,255,255,.04)';
      for (let y = 0; y < H; y += 4) g.fillRect(0, y, W, 1);
    };
    const frame = (now: number) => {
      raf = requestAnimationFrame(frame);
      if (now - last < 33) return;
      last = now;
      t += 0.016;
      draw();
    };
    raf = requestAnimationFrame(frame);
    return () => cancelAnimationFrame(raf);
  }, []);
  return <canvas ref={ref} width={W} height={H} className="rp-retro" />;
}
