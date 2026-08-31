const assert = require('node:assert/strict');
const path = require('node:path');
const vm = require('node:vm');
const { readFile } = require('node:fs/promises');
const { test } = require('node:test');

const ROOT = path.resolve(__dirname, '..');

async function evaluateLoginStatus({ ok, body }) {
  const calls = [];
  const context = {
    console: { error() {}, log() {} },
    fetch: async (url, options) => {
      calls.push({ url, options });
      return { ok, json: async () => body };
    },
    localStorage: { getItem: () => null },
    window: { location: {} },
  };
  vm.createContext(context);
  vm.runInContext(await readFile(path.join(ROOT, 'dist', 'js', 'api.js'), 'utf8'), context);
  return { loggedIn: await context.isLoggedIn(), calls };
}

test('auth state requires HTTP success plus a successful business envelope and identity data', async () => {
  assert.equal((await evaluateLoginStatus({ ok: false, body: { code: 200, data: {} } })).loggedIn, false);
  assert.equal((await evaluateLoginStatus({ ok: true, body: { code: 401, data: null } })).loggedIn, false);
  assert.equal((await evaluateLoginStatus({ ok: true, body: { code: 200, data: null } })).loggedIn, false);

  const authenticated = await evaluateLoginStatus({
    ok: true,
    body: { code: 200, data: { email: 'owner@example.com' } },
  });
  assert.equal(authenticated.loggedIn, true);
  assert.equal(authenticated.calls.length, 1);
  assert.equal(authenticated.calls[0].url, '/api/v1/auth/me');
  assert.equal(authenticated.calls[0].options.credentials, 'include');
});

test('public routes stay public while every merchant and developer workspace route has an auth gate', async () => {
  for (const route of ['index.html', 'docs/index.html', 'pay/index.html']) {
    const html = await readFile(path.join(ROOT, 'dist', ...route.split('/')), 'utf8');
    assert.doesNotMatch(html, /await isLoggedIn\(\)/, route);
  }

  for (const route of [
    'merchant/orders', 'merchant/onboarding', 'merchant/ledger', 'merchant/risk',
    'merchant/wallet', 'merchant/developer', 'merchant/developer/api-keys',
    'merchant/developer/sandbox', 'merchant/developer/webhooks', 'merchant/developer/logs',
  ]) {
    const html = await readFile(path.join(ROOT, 'dist', ...route.split('/'), 'index.html'), 'utf8');
    assert.match(html, /await isLoggedIn\(\)/, route);
    assert.match(html, /location\.replace\(['"]\/login\.html['"]\)/, route);
  }
});

test('production checkout is display-only and server security fails closed for result submission', async () => {
  const paymentHtml = await readFile(path.join(ROOT, 'dist', 'pay', 'index.html'), 'utf8');
  assert.match(paymentHtml, /classList\.toggle\(['"]hidden['"],!IS_DUMMY\)/);
  assert.match(paymentHtml, /if\(!IS_DUMMY\)/);
  assert.match(paymentHtml, /setInterval\(refreshProductionStatus,STATUS_POLL_MS\)/);
  assert.match(paymentHtml, /get\(['"]checkoutToken['"]\)/);
  assert.doesNotMatch(paymentHtml, /get\(['"]orderNo['"]\)/);

  const securityConfig = await readFile(path.join(
    ROOT, '..', 'huizhipay-user', 'src', 'main', 'java', 'com', 'huizhipay',
    'user', 'config', 'SecurityConfig.java'
  ), 'utf8');
  assert.match(securityConfig, /checkout-result-enabled:false/);
  assert.match(securityConfig, /requestMatchers\(HttpMethod\.POST, "\/api\/v1\/dummy\/orders\/\*\/result"\)\.denyAll\(\)/);
  assert.match(securityConfig, /dummyCheckoutResultEnabled/);
});

test('write endpoints retain explicit server-side role guards', async () => {
  const expectations = [
    ['huizhipay-merchant', 'controller/TeamController.java', /requireAnyRole\(OWNER, ADMIN\)/],
    ['huizhipay-merchant', 'controller/OnboardingController.java', /requireAnyRole\(OWNER, ADMIN\)/],
    ['huizhipay-risk', 'controller/RiskRuleController.java', /requireAnyRole\(OWNER, ADMIN, ANALYST\)/],
    ['huizhipay-acquiring', 'controller/DummyPaymentController.java', /requireAnyRole\(OWNER, ADMIN\)/],
  ];
  for (const [module, relative, pattern] of expectations) {
    const source = await readFile(path.join(
      ROOT, '..', module, 'src', 'main', 'java', 'com', 'huizhipay',
      module.replace('huizhipay-', ''), ...relative.split('/')
    ), 'utf8');
    assert.match(source, pattern, `${module}/${relative}`);
  }
});
