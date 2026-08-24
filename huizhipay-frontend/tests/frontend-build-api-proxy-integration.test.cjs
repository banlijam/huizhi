const assert = require('node:assert/strict');
const http = require('node:http');
const path = require('node:path');
const { spawn } = require('node:child_process');
const { readFile, stat } = require('node:fs/promises');
const { test } = require('node:test');

const ROOT = path.resolve(__dirname, '..');

function listen(server) {
  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => resolve(server.address().port));
  });
}

function close(server) {
  return new Promise((resolve, reject) => {
    server.close((error) => error ? reject(error) : resolve());
  });
}

async function freePort() {
  const server = http.createServer();
  const port = await listen(server);
  await close(server);
  return port;
}

async function waitFor(url, child) {
  const deadline = Date.now() + 5000;
  while (Date.now() < deadline) {
    if (child.exitCode !== null) {
      throw new Error(`Frontend server exited with code ${child.exitCode}`);
    }
    try {
      const response = await fetch(url);
      if (response.ok) return;
    } catch {}
    await new Promise((resolve) => setTimeout(resolve, 50));
  }
  throw new Error(`Frontend server did not become ready at ${url}`);
}

test('build contains the complete interactive prototype and local vendor assets', async () => {
  const html = await readFile(path.join(ROOT, 'dist', 'index.html'), 'utf8');
  assert.match(html, /HuizhiPay Web-first Interactive Prototype/);

  for (const screen of ['checkout-web', 'checkout-mobile', 'dashboard', 'developer']) {
    assert.match(html, new RegExp(`id=["']${screen}["']`));
  }

  assert.match(html, /new URLSearchParams\(location\.search\)/);
  assert.match(html, /id=["']dummy-create-form["']/);
  assert.match(html, /\/api\/v1\/dummy\/orders/);
  assert.match(html, /normalizedPath===['"]\/demo['"]/);
  assert.match(html, /<select id=\\?"dummy-currency\\?">/);
  for (const currency of ['USD', 'HKD', 'EUR', 'GBP', 'CNY', 'JPY', 'SGD']) {
    assert.match(html, new RegExp(`<option>${currency}</option>`));
  }
  assert.match(html, /Responsive Checkout/);
  assert.doesNotMatch(html, /data-screen=["']checkout-mobile["']/);
  assert.doesNotMatch(html, /<script[^>]+src=["']https:\/\//i);

  for (const file of ['tailwindcss.js', 'lucide.min.js', 'chart.umd.min.js']) {
    const info = await stat(path.join(ROOT, 'dist', 'vendor', file));
    assert.ok(info.size > 0, `${file} must not be empty`);
  }

  const merchantRoute = await readFile(path.join(ROOT, 'dist', 'merchant', 'index.html'), 'utf8');
  const demoRoute = await readFile(path.join(ROOT, 'dist', 'demo', 'index.html'), 'utf8');
  const developerRoute = await readFile(path.join(ROOT, 'dist', 'developer', 'index.html'), 'utf8');
  assert.match(merchantRoute, /id=["']dashboard["']/);
  assert.match(demoRoute, /class=["']demo-nav["']/);
  assert.match(developerRoute, /开发者门户本周暂缓/);
});

test('built frontend serves routes and proxies API requests to the backend', async (t) => {
  const backend = http.createServer((request, response) => {
    let body = '';
    request.setEncoding('utf8');
    request.on('data', (chunk) => { body += chunk; });
    request.on('end', () => {
      if (request.headers.origin) {
        response.writeHead(403, { 'content-type': 'text/plain' });
        response.end('Invalid CORS request');
        return;
      }
      response.writeHead(200, { 'content-type': 'application/json' });
      response.end(JSON.stringify({ method: request.method, url: request.url, body }));
    });
  });
  const backendPort = await listen(backend);
  t.after(() => close(backend));

  const frontendPort = await freePort();
  const child = spawn(process.execPath, ['server.js', '--dist'], {
    cwd: ROOT,
    env: {
      ...process.env,
      PORT: String(frontendPort),
      BACKEND_API: `http://127.0.0.1:${backendPort}`,
    },
    stdio: 'ignore',
  });
  t.after(() => {
    if (child.exitCode === null) child.kill();
  });

  const baseUrl = `http://127.0.0.1:${frontendPort}`;
  await waitFor(`${baseUrl}/?screen=dashboard`, child);

  const root = await fetch(`${baseUrl}/?screen=developer`);
  assert.equal(root.status, 200);
  assert.match(await root.text(), /id=["']developer["']/);

  const merchant = await fetch(`${baseUrl}/merchant`);
  assert.equal(merchant.status, 200);
  assert.match(await merchant.text(), /id=["']dummy-create-form["']/);

  const merchantSlash = await fetch(`${baseUrl}/merchant/`);
  assert.equal(merchantSlash.status, 200);
  assert.match(await merchantSlash.text(), /id=["']dashboard["']/);

  const demo = await fetch(`${baseUrl}/demo?screen=developer`);
  assert.equal(demo.status, 200);
  assert.match(await demo.text(), /Prototype sample data/);

  const demoSlash = await fetch(`${baseUrl}/demo/?screen=developer`);
  assert.equal(demoSlash.status, 200);
  assert.match(await demoSlash.text(), /normalizedPath/);

  const developer = await fetch(`${baseUrl}/developer`);
  assert.equal(developer.status, 200);
  assert.match(await developer.text(), /开发者门户本周暂缓/);

  const developerSlash = await fetch(`${baseUrl}/developer/`);
  assert.equal(developerSlash.status, 200);
  assert.match(await developerSlash.text(), /开发者门户本周暂缓/);

  const paymentPage = await fetch(`${baseUrl}/pay/`);
  assert.equal(paymentPage.status, 200);
  const paymentHtml = await paymentPage.text();
  assert.match(paymentHtml, /HuizhiPay/);
  assert.match(paymentHtml, /id=["']success["']/);
  assert.match(paymentHtml, /id=["']fail["']/);
  assert.match(paymentHtml, /let seconds=10/);
  assert.match(paymentHtml, /location\.href=['"]\/merchant['"]/);
  assert.match(paymentHtml, /\/api\/v1\/dummy\/orders/);

  const paymentWithoutSlash = await fetch(`${baseUrl}/pay`);
  assert.equal(paymentWithoutSlash.status, 200);
  assert.match(await paymentWithoutSlash.text(), /模拟付款成功/);

  const favicon = await fetch(`${baseUrl}/favicon.svg`);
  assert.equal(favicon.status, 200);
  assert.match(favicon.headers.get('content-type') || '', /^image\/svg\+xml/);

  const payload = JSON.stringify({ amount: 10, currency: 'USD' });
  const api = await fetch(`${baseUrl}/api/v1/orders?mode=test`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      origin: baseUrl,
    },
    body: payload,
  });
  assert.equal(api.status, 200);
  assert.deepEqual(await api.json(), {
    method: 'POST',
    url: '/api/v1/orders?mode=test',
    body: payload,
  });
});
