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

test('KYB workspace uses the dashboard hierarchy and semantic status styles', async () => {
  const html = await readFile(path.join(ROOT, 'dist', 'merchant', 'onboarding', 'index.html'), 'utf8');
  assert.match(html, /workspace-grid kyb-layout/);
  assert.match(html, /ops-card kyb-hero/);
  assert.match(html, /kyb-decision-state/);
  assert.match(html, /TRANSACTIONS ENABLED/);
  assert.match(html, /TRANSACTIONS BLOCKED/);
  assert.match(html, /\.ops-status\.warning\{[^}]*var\(--amber\)/);
  assert.match(html, /\.ops-status\.danger\{[^}]*var\(--red\)/);
});
