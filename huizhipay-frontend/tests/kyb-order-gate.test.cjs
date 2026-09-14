const assert = require('node:assert/strict');
const path = require('node:path');
const vm = require('node:vm');
const { readFile } = require('node:fs/promises');
const { test } = require('node:test');

const ROOT = path.resolve(__dirname, '..');

async function loadGate() {
  const context = {};
  vm.createContext(context);
  vm.runInContext(await readFile(path.join(ROOT, 'dist', 'js', 'kyb-order-gate.js'), 'utf8'), context);
  return context.KybOrderGate;
}

test('DRAFT, PENDING, and REJECTED KYB show the blocker and do not authorize order creation', async () => {
  const gate = await loadGate();
  for (const status of ['DRAFT', 'PENDING', 'REJECTED']) {
    const shown = [];
    const approved = await gate.requireApprovedKyb({
      fetchStatus: async () => ({ status }),
      showBlocked: (value) => shown.push(value),
    });
    assert.equal(approved, false, status);
    assert.deepEqual(shown, [status], status);
  }
});

test('APPROVED KYB authorizes order creation without showing the blocker', async () => {
  const gate = await loadGate();
  let popupShown = false;
  const approved = await gate.requireApprovedKyb({
    fetchStatus: async () => ({ status: 'APPROVED' }),
    showBlocked: () => { popupShown = true; },
  });
  assert.equal(approved, true);
  assert.equal(popupShown, false);
});

test('orders page checks the real onboarding API before POST and provides a KYB popup link', async () => {
  const html = await readFile(path.join(ROOT, 'dist', 'merchant', 'orders', 'index.html'), 'utf8');
  assert.match(html, /id=["']kyb-blocked-modal["'][^>]*role=["']dialog["']/);
  assert.match(html, /class=["'][^"']*kyb-blocked-modal[^"']*["']/);
  assert.match(html, /class=["']kyb-modal-actions["']/);
  assert.match(html, /href=["']\/merchant\/onboarding["']/);
  assert.match(html, /fetchStatus:\(\)=>merchantApi\('\/api\/v1\/onboarding\/status'\)/);
  assert.match(html, /await KybOrderGate\.requireApprovedKyb/);
  assert.match(html, /if\(!approved\)return;[^]*orderRequest\(ORDERS_API/);
});

test('KYB workspace uses a five-step merchant-facing verification journey', async () => {
  const html = await readFile(path.join(ROOT, 'dist', 'merchant', 'onboarding', 'index.html'), 'utf8');
  const english = await readFile(path.join(ROOT, 'dist', 'i18n', 'en.js'), 'utf8');
  assert.match(html, /class=["']kyb-wizard/);
  assert.match(html, /kyb-progress-card/);
  assert.match(html, /kyb-progress-list/);
  assert.match(html, /wizardStepOwners/);
  assert.match(english, /Directors & Beneficial Owners/);
  assert.match(english, /Under review/);
  assert.match(english, /More information needed/);
});

test('KYB workspace submits first-time and rejected merchant details to the real onboarding API', async () => {
  const html = await readFile(path.join(ROOT, 'dist', 'merchant', 'onboarding', 'index.html'), 'utf8');

  for (const field of ['company', 'country', 'licenseNo', 'legalRep', 'idNo', 'settlementPref']) {
    assert.match(html, new RegExp(`name=["']${field}["']`), field);
  }
  assert.match(html, /id=["']kyb-submission-form["']/);
  assert.match(html, /status==='PENDING'\|\|status==='APPROVED'/);
  assert.match(html, /KYB_DRAFT_STORAGE_KEY='huizhipay\.kybDraft\.v1'/);
  assert.match(html, /saveKybDraft\(draft\)/);
  assert.match(html, /draft\.step<5/);
  assert.match(html, /await merchantPost\('\/api\/v1\/onboarding\/submit'/);
  assert.match(html, /const refreshed=await merchantApi\('\/api\/v1\/onboarding\/status'\)/);
  assert.match(html, /clearKybDraft\(\)/);
  assert.match(html, /button\.disabled=true/);
  assert.match(html, /message\.classList\.add\('error'\)/);
  assert.doesNotMatch(html, /id=["']kyb-license-file["']/);
});

test('pending KYB can be withdrawn, edited, and resubmitted', async () => {
  const html = await readFile(path.join(ROOT, 'dist', 'merchant', 'onboarding', 'index.html'), 'utf8');
  const chinese = await readFile(path.join(ROOT, 'dist', 'i18n', 'zh.js'), 'utf8');
  assert.match(html, /id=["']kyb-withdraw["']/);
  assert.match(html, /await merchantPost\('\/api\/v1\/onboarding\/withdraw',\{\}\)/);
  assert.match(html, /mountOnboardingForm\(refreshed,content,navigationId\)/);
  assert.match(chinese, /撤回并修改/);
});

test('KYB typography is readable at 100 percent browser zoom', async () => {
  const html = await readFile(path.join(ROOT, 'dist', 'merchant', 'onboarding', 'index.html'), 'utf8');
  assert.match(html, /KYB readability baseline: equivalent to the former page viewed at 120% browser zoom/);
  assert.match(html, /kyb-wizard-intro h2\{font-size:26px\}/);
  assert.match(html, /kyb-control input[^}]*font-size:14px/);
  assert.match(html, /kyb-submit[^}]*font-size:13px/);
});
