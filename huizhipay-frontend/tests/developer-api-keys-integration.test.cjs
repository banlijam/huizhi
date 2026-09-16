const assert = require('node:assert/strict');
const path = require('node:path');
const { readFile } = require('node:fs/promises');
const { test } = require('node:test');

const ROOT = path.resolve(__dirname, '..');

test('developer API key page uses the authenticated Test key service', async () => {
  const html = await readFile(path.join(ROOT, 'src', 'index.html'), 'utf8');
  assert.match(html, /\/api\/v1\/developer\/api-keys/);
  assert.match(html, /Generate Test key/);
  assert.match(html, /Disable active key/);
  assert.match(html, /issued\.secretKey/);
  assert.match(html, /csrfFetch/);
  assert.match(html, /\/activate/);
  assert.doesNotMatch(html, /X-HuizhiPay-CSRF/);
  assert.match(html, /mountApiKeys\(showToast\)/);
  assert.doesNotMatch(html, /密钥签发服务尚未接入/);
});
