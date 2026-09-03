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

test('build merges the public home and login entry while separating protected workspaces and checkout', async () => {
  const homeHtml = await readFile(path.join(ROOT, 'dist', 'index.html'), 'utf8');
  const loginHtml = await readFile(path.join(ROOT, 'dist', 'login.html'), 'utf8');
  assert.equal(homeHtml, loginHtml);
  assert.match(homeHtml, /id=["']login-form["']/);
  assert.match(homeHtml, /data-i18n=["']login\.heroTitle["']/);
  assert.match(homeHtml, /href=["']\/docs["']/);
  assert.match(homeHtml, /class=["']login-shell["']/);
  assert.match(homeHtml, /radial-gradient/);
  assert.doesNotMatch(homeHtml, /data-screen=/);
  await assert.rejects(readFile(path.join(ROOT, 'dist', 'home.html'), 'utf8'), /ENOENT/);

  const html = await readFile(path.join(ROOT, 'dist', 'merchant', 'index.html'), 'utf8');
  assert.match(html, /HuizhiPay Web-first Interactive Prototype/);

  for (const screen of ['checkout-web', 'checkout-mobile', 'dashboard', 'developer']) {
    assert.match(html, new RegExp(`id=["']${screen}["']`));
  }

  assert.match(html, /new URLSearchParams\(location\.search\)/);
  assert.match(html, /id=["']dummy-create-form["']/);
  assert.match(html, /src=["']\/js\/api\.js["']/);
  assert.match(html, /await isLoggedIn\(\)/);
  assert.match(html, /location\.replace\(['"]\/login\.html['"]\)/);
  assert.match(html, /body class=["']auth-pending["']/);
  assert.match(html, /src=["']\/app-config\.js["']/);
  assert.match(html, /APP_CONFIG\.ordersApi/);
  assert.match(html, /normalizedPath===['"]\/demo['"]/);
  assert.doesNotMatch(html, /pageParams\.get\(['"]demo['"]\)/);
  assert.doesNotMatch(html, /Huizhi Developers|Developer Portal/);
  assert.match(html, /applyState\(['"]loading['"]\)/);
  assert.match(html, /orderText\(['"]creatingTitle['"]\)/);
  assert.match(html, /id=["']language-toggle["']/);
  assert.match(html, /id=["']developer-language-toggle["']/);
  assert.equal((html.match(/<button[^>]*data-workspace-language-toggle/g) || []).length, 2);
  assert.match(html, /#merchant-workspace \.side>huizhi-brand/);
  assert.match(html, /src=["']\/i18n\/zh\.js["']/);
  assert.match(html, /src=["']\/i18n\/en\.js["']/);
  assert.match(html, /Merchant Dashboard/);
  assert.match(html, /id=\\?["']orders-prev\\?["']/);
  assert.match(html, /id=\\?["']orders-next\\?["']/);
  assert.match(html, /searchParams\.set\(['"]page['"],page\)/);
  assert.match(html, /response\.items/);
  assert.match(html, /history\[historyAction===['"]replace['"]\?['"]replaceState['"]:['"]pushState['"]\]/);
  assert.match(html, /addEventListener\(['"]popstate['"]/);
  assert.match(html, /data-developer-nav-toggle/);
  assert.match(html, /\/merchant\/orders/);
  assert.match(html, /\/merchant\/developer\/api-keys/);
  assert.match(html, /<select id=\\?"dummy-currency\\?">/);
  assert.match(html, /<select id=\\?"dummy-currency\\?"><option>USD<\/option><\/select>/);
  assert.match(html, /orderText\(IS_DUMMY\?['"]sandboxNote['"]:['"]productionNote['"]\)/);
  assert.match(await readFile(path.join(ROOT, 'dist', 'i18n', 'zh.js'), 'utf8'), /USD Sandbox，不会产生真实扣款/);
  assert.match(html, /Responsive Checkout/);
  assert.doesNotMatch(html, /data-screen=["']checkout-mobile["']/);
  assert.doesNotMatch(html, /<script[^>]+src=["']https:\/\//i);

  for (const file of ['tailwindcss.js', 'lucide.min.js', 'chart.umd.min.js']) {
    const info = await stat(path.join(ROOT, 'dist', 'vendor', file));
    assert.ok(info.size > 0, `${file} must not be empty`);
  }

  const merchantRoute = html;
  const demoRoute = await readFile(path.join(ROOT, 'dist', 'demo', 'index.html'), 'utf8');
  const developerRoute = await readFile(path.join(ROOT, 'dist', 'developer', 'index.html'), 'utf8');
  assert.match(merchantRoute, /id=["']dashboard["']/);
  assert.match(demoRoute, /class=["']demo-nav["']/);
  assert.match(developerRoute, /id=["']developer["']/);
  assert.match(developerRoute, /Developer Tools/);
  assert.match(developerRoute, /Sandbox Request Builder/);
  assert.match(developerRoute, /Capability Map/);
  assert.match(developerRoute, /backend (?:is )?not connected/i);
  assert.doesNotMatch(developerRoute, /sk_test_[a-z0-9]/i);
  assert.match(developerRoute, /src=["']\/js\/api\.js["']/);
  assert.match(developerRoute, /await isLoggedIn\(\)/);
  assert.match(developerRoute, /location\.replace\(['"]\/login\.html['"]\)/);

  for (const route of ['merchant/onboarding', 'merchant/ledger', 'merchant/risk', 'merchant/wallet']) {
    const workspaceRoute = await readFile(path.join(ROOT, 'dist', ...route.split('/'), 'index.html'), 'utf8');
    assert.match(workspaceRoute, /id=["']merchant-workspace["']/);
    assert.match(workspaceRoute, /workspace-nav/);
    assert.match(workspaceRoute, /merchantPageConfig/);
    assert.match(workspaceRoute, /await isLoggedIn\(\)/);
    assert.doesNotMatch(workspaceRoute, /<h1 id=["']title["']>占位页面/);
  }

  for (const route of [
    'developer/api-keys',
    'developer/sandbox',
    'developer/webhooks',
    'developer/logs',
  ]) {
    const routeHtml = await readFile(path.join(ROOT, 'dist', ...route.split('/'), 'index.html'), 'utf8');
    assert.match(routeHtml, /id=["']developer["']/);
    assert.match(routeHtml, /class=["']nav workspace-nav dev-route-nav["']/);
    assert.match(routeHtml, /developerPageConfig/);
    assert.match(routeHtml, /await isLoggedIn\(\)/);
    assert.doesNotMatch(routeHtml, /<h1 id=["']title["']>占位页面/);
  }

  for (const route of [
    'merchant/orders',
    'merchant/developer',
    'merchant/developer/api-keys',
    'merchant/developer/sandbox',
    'merchant/developer/webhooks',
    'merchant/developer/logs',
  ]) {
    const routeHtml = await readFile(path.join(ROOT, 'dist', ...route.split('/'), 'index.html'), 'utf8');
    assert.match(routeHtml, /workspace-nav/);
    assert.match(routeHtml, /navigateWorkspace/);
    assert.match(routeHtml, /await isLoggedIn\(\)/);
  }

  assert.match(await readFile(path.join(ROOT, 'dist', 'checkout', 'widget', 'index.html'), 'utf8'), /Embedded Checkout Widget/);
  assert.match(await readFile(path.join(ROOT, 'dist', 'merchant', 'login', 'index.html'), 'utf8'), /前往统一登录/);
  await assert.rejects(readFile(path.join(ROOT, 'dist', 'developer', 'login', 'index.html'), 'utf8'), /ENOENT/);
  await assert.rejects(readFile(path.join(ROOT, 'dist', 'developer.html'), 'utf8'), /ENOENT/);
  await assert.rejects(readFile(path.join(ROOT, 'dist', 'pay', 'pay.js'), 'utf8'), /ENOENT/);
  await assert.rejects(readFile(path.join(ROOT, 'dist', 'pay', 'style.css'), 'utf8'), /ENOENT/);
  assert.match(await readFile(path.join(ROOT, 'dist', 'docs', 'index.html'), 'utf8'), /公开开发文档占位页/);
  assert.match(await readFile(path.join(ROOT, 'dist', 'developer', 'docs', 'index.html'), 'utf8'), /前往公开开发文档/);
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

  const root = await fetch(`${baseUrl}/`);
  assert.equal(root.status, 200);
  const rootHtml = await root.text();
  assert.match(rootHtml, /id=["']login-form["']/);
  assert.match(rootHtml, /href=["']\/docs["']/);

  const merchant = await fetch(`${baseUrl}/merchant`);
  assert.equal(merchant.status, 200);
  assert.match(await merchant.text(), /id=["']dummy-create-form["']/);

  const merchantSlash = await fetch(`${baseUrl}/merchant/`);
  assert.equal(merchantSlash.status, 200);
  assert.equal(merchantSlash.redirected, true);
  assert.match(merchantSlash.url, /\/merchant\/orders$/);
  assert.match(await merchantSlash.text(), /id=["']dashboard["']/);

  const demo = await fetch(`${baseUrl}/demo?screen=developer`);
  assert.equal(demo.status, 200);
  assert.match(await demo.text(), /Prototype preview/);

  const demoSlash = await fetch(`${baseUrl}/demo/?screen=developer`);
  assert.equal(demoSlash.status, 200);
  assert.match(await demoSlash.text(), /normalizedPath/);

  const developer = await fetch(`${baseUrl}/developer`);
  assert.equal(developer.status, 200);
  assert.equal(developer.redirected, true);
  assert.match(developer.url, /\/merchant\/developer$/);
  assert.match(await developer.text(), /Developer Tools/);

  const developerSlash = await fetch(`${baseUrl}/developer/`);
  assert.equal(developerSlash.status, 200);
  assert.match(await developerSlash.text(), /Sandbox Request Builder/);

  for (const route of [
    '/merchant/orders',
    '/merchant/onboarding',
    '/merchant/ledger',
    '/merchant/risk',
    '/merchant/wallet',
    '/merchant/developer',
    '/merchant/developer/api-keys',
    '/merchant/developer/sandbox',
    '/merchant/developer/webhooks',
    '/merchant/developer/logs',
  ]) {
    const response = await fetch(`${baseUrl}${route}`);
    assert.equal(response.status, 200, route);
    const routeHtml = await response.text();
    assert.match(routeHtml, /HuizhiPay Merchant Workspace/);
    if (!route.startsWith('/merchant/developer')) {
      assert.match(routeHtml, /id=["']merchant-workspace["']/);
      assert.match(routeHtml, /workspace-nav/);
      assert.doesNotMatch(routeHtml, /<h1 id=["']title["']>占位页面/);
    } else {
      assert.match(routeHtml, /id=["']developer["']/);
      assert.match(routeHtml, /dev-route-nav/);
      assert.doesNotMatch(routeHtml, /<h1 id=["']title["']>占位页面/);
    }
  }

  for (const [legacy, canonical] of [
    ['/developer/api-keys', '/merchant/developer/api-keys'],
    ['/developer/sandbox', '/merchant/developer/sandbox'],
    ['/developer/webhooks', '/merchant/developer/webhooks'],
    ['/developer/logs', '/merchant/developer/logs'],
  ]) {
    const response = await fetch(`${baseUrl}${legacy}`, { redirect: 'manual' });
    assert.equal(response.status, 302);
    assert.equal(response.headers.get('location'), canonical);
  }

  const widget = await fetch(`${baseUrl}/checkout/widget`);
  assert.equal(widget.status, 200);
  assert.match(await widget.text(), /Embedded Checkout Widget/);

  const docs = await fetch(`${baseUrl}/docs`);
  assert.equal(docs.status, 200);
  assert.match(await docs.text(), /公开开发文档占位页/);

  const mergedHome = await fetch(`${baseUrl}/`);
  const mergedLogin = await fetch(`${baseUrl}/login`);
  assert.equal(mergedHome.status, 200);
  assert.equal(mergedLogin.status, 200);
  assert.equal(await mergedHome.text(), await mergedLogin.text());

  for (const legacyHome of ['/home', '/home.html']) {
    const response = await fetch(`${baseUrl}${legacyHome}`, { redirect: 'manual' });
    assert.equal(response.status, 302);
    assert.equal(response.headers.get('location'), '/');
  }

  const oldDeveloperDocs = await fetch(`${baseUrl}/developer/docs`);
  assert.equal(oldDeveloperDocs.status, 200);
  assert.equal(oldDeveloperDocs.redirected, true);
  assert.match(await oldDeveloperDocs.text(), /公开开发文档占位页/);

  const oldMerchantLogin = await fetch(`${baseUrl}/merchant/login`);
  assert.equal(oldMerchantLogin.status, 200);
  assert.equal(oldMerchantLogin.redirected, true);
  assert.match(await oldMerchantLogin.text(), /HuizhiPay - Login/);

  const removedDeveloperLogin = await fetch(`${baseUrl}/developer/login`, { redirect: 'manual' });
  assert.equal(removedDeveloperLogin.status, 404);
  assert.equal(removedDeveloperLogin.headers.get('location'), null);

  const removedDeveloperPage = await fetch(`${baseUrl}/developer.html`);
  assert.equal(removedDeveloperPage.status, 404);

  const paymentPage = await fetch(`${baseUrl}/pay/`);
  assert.equal(paymentPage.status, 200);
  const paymentHtml = await paymentPage.text();
  assert.match(paymentHtml, /HuizhiPay/);
  assert.match(paymentHtml, /id=["']success["']/);
  assert.match(paymentHtml, /id=["']fail["']/);
  assert.match(paymentHtml, /AUTO_RETURN_SECONDS/);
  assert.match(paymentHtml, /id=["']transition["']/);
  assert.match(paymentHtml, /pointer-events:none/);
  assert.match(paymentHtml, /\.transition-layer\{position:fixed;z-index:20;inset:0;display:grid;place-items:center/);
  assert.match(paymentHtml, /id=["']language-toggle["']/);
  assert.match(paymentHtml, /showLocalizedTransition\(['"]loading['"],['"]submittingTitle['"],['"]submittingCopy['"]\)/);
  assert.match(paymentHtml, /showLocalizedTransition\(['"]success['"],['"]success['"],['"]returnCountdown['"]/);
  assert.match(paymentHtml, /get\(['"]checkoutToken['"]\)/);
  assert.doesNotMatch(paymentHtml, /get\(['"]orderNo['"]\)/);
  assert.match(paymentHtml, /id=["']merchant-name["']/);
  assert.match(paymentHtml, /order\.returnUrl/);
  assert.match(paymentHtml, /location\.href=returnUrl/);
  assert.match(paymentHtml, /refreshProductionStatus/);
  assert.match(paymentHtml, /classList\.toggle\(['"]hidden['"],!IS_DUMMY\)/);
  assert.match(paymentHtml, /APP_CONFIG\.ordersApi/);

  const paymentWithoutSlash = await fetch(`${baseUrl}/pay`);
  assert.equal(paymentWithoutSlash.status, 200);
  assert.match(await paymentWithoutSlash.text(), /完成付款/);

  const favicon = await fetch(`${baseUrl}/favicon.svg`);
  assert.equal(favicon.status, 200);
  assert.match(favicon.headers.get('content-type') || '', /^image\/svg\+xml/);

  const appConfig = await fetch(`${baseUrl}/app-config.js`);
  assert.equal(appConfig.status, 200);
  assert.match(await appConfig.text(), /mode:\s*['"]dummy['"]/);

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

test('checkout token, return URL, and merchant authorization contracts stay aligned across layers', async () => {
  const apiJs = await readFile(path.join(ROOT, 'dist', 'js', 'api.js'), 'utf8');
  assert.match(apiJs, /body\.code === 200/);
  assert.match(apiJs, /Boolean\(body\.data\)/);
  assert.match(apiJs, /error\.message \|\| error\.error/);
  assert.doesNotMatch(apiJs, /body\.data\?\.merchant(?:Id|Role)/);

  const controller = await readFile(path.join(
    ROOT, '..', 'huizhipay-acquiring', 'src', 'main', 'java', 'com', 'huizhipay',
    'acquiring', 'controller', 'DummyPaymentController.java'
  ), 'utf8');
  assert.match(controller, /@GetMapping\("\/\{checkoutToken\}"\)/);
  assert.match(controller, /PaymentOrder::getCheckoutToken/);
  assert.match(controller, /"\/pay\/\?checkoutToken="/);
  assert.match(controller, /String returnUrl/);
  assert.match(controller, /MerchantResolver/);
  assert.match(controller, /setMerchantId\(merchantId\)/);
  assert.match(controller, /PaymentOrder::getMerchantId, merchantId/);
  assert.doesNotMatch(controller, /"\/pay\/\?orderNo="/);

  const securityConfig = await readFile(path.join(
    ROOT, '..', 'huizhipay-user', 'src', 'main', 'java', 'com', 'huizhipay',
    'user', 'config', 'SecurityConfig.java'
  ), 'utf8');
  assert.doesNotMatch(securityConfig, /"\/api\/v1\/dummy\/\*\*"/);
  assert.match(securityConfig, /HttpMethod\.GET, "\/api\/v1\/dummy\/orders\/\*"/);
  assert.match(securityConfig, /HttpMethod\.POST, "\/api\/v1\/dummy\/orders\/\*\/result"/);
  assert.match(securityConfig, /\.denyAll\(\)/);
  assert.match(securityConfig, /dummyCheckoutResultEnabled/);

  const migration = await readFile(path.join(
    ROOT, '..', 'huizhipay-bootstrap', 'src', 'main', 'resources', 'db', 'migration',
    'V1.2__add_checkout_token_and_return_url.sql'
  ), 'utf8');
  assert.match(migration, /checkout_token/);
  assert.match(migration, /return_url/);
  assert.match(migration, /unique index/);
});
