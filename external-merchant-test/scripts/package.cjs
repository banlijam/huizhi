#!/usr/bin/env node
const { cpSync, existsSync, mkdirSync, rmSync } = require('node:fs');
const { join, resolve } = require('node:path');

const root = resolve(__dirname, '..');
const dist = join(root, 'dist');

rmSync(dist, { recursive: true, force: true });
mkdirSync(dist, { recursive: true });

for (const directory of ['src', 'public']) {
  cpSync(join(root, directory), join(dist, directory), { recursive: true });
}
for (const file of ['package.json', '.env.example', 'start.ps1', 'start.cmd', 'README.md']) {
  cpSync(join(root, file), join(dist, file));
}

if (!existsSync(join(dist, 'src', 'server.js'))) throw new Error('Package is missing its server entrypoint');
console.log(`External merchant test package created at ${dist}`);
