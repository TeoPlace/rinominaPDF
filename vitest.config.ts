import { defineConfig } from 'vitest/config';

// I test del processo principale (src/main) girano in Node 24 (node:sqlite incluso), senza Electron.
export default defineConfig({ test: { include: ['tests/**/*.test.ts'], environment: 'node', testTimeout: 30000, pool: 'forks' } });
