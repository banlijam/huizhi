const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { mkdtempSync, rmSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { join } = require('node:path');
const { createApp } = require('../src/app');

function listen(server) { return new Promise(resolve => server.listen(0, '127.0.0.1', () => resolve(server.address().port))); }
function close(server) { return new Promise(resolve => server.close(resolve)); }

test('fixed catalog price is persisted and buyer can only query by random token', async () => {
  const seen = [];
  const platform = http.createServer(async (req, res) => {
    let raw=''; for await (const c of req) raw += c;
    if (req.method === 'POST') {
      seen.push(JSON.parse(raw));
      res.writeHead(200, {'content-type':'application/json'});
      return res.end(JSON.stringify({code:200,data:{platformOrderNo:'TFI-1',merchantOrderNo:seen[0].merchantOrderNo,
        amount:'12.00',currency:'USD',status:'PENDING',channelStatus:'INITIATED',paymentUrl:'https://checkout.transfi.test/pay/1'}}));
    }
    res.writeHead(200, {'content-type':'application/json'});
    res.end(JSON.stringify({code:200,data:{platformOrderNo:'TFI-1',status:'PENDING',channelStatus:'INITIATED',paymentUrl:'https://checkout.transfi.test/pay/1'}}));
  });
  const platformPort = await listen(platform);
  const dir = mkdtempSync(join(tmpdir(), 'hzp-shop-'));
  const app = createApp({databasePath:join(dir,'orders.sqlite'),apiBase:`http://127.0.0.1:${platformPort}`,
    apiKey:'hzp_test_'+'a'.repeat(48),publicOrigin:'https://merchant-sandbox.example.test'});
  const port = await listen(app.server);
  try {
    const createdResponse = await fetch(`http://127.0.0.1:${port}/api/orders`, {method:'POST',headers:{'content-type':'application/json'},
      body:JSON.stringify({productId:'sandbox-mug',amount:'0.01'})});
    assert.equal(createdResponse.status, 201);
    const created = await createdResponse.json();
    assert.equal(seen[0].amount, '12.00');
    assert.equal(created.product.amount, '12.00');
    assert.match(created.accessToken, /^[A-Za-z0-9_-]{32}$/);
    assert.equal((await fetch(`http://127.0.0.1:${port}/api/orders/not-a-token`)).status, 404);
    const queried = await (await fetch(`http://127.0.0.1:${port}/api/orders/${created.accessToken}`)).json();
    assert.equal(queried.platformOrderNo, 'TFI-1');
  } finally { await close(app.server); await close(platform); app.db.close(); rmSync(dir,{recursive:true,force:true}); }
});

test('platform timeout keeps a durable pending-confirmation order without a fake payment URL', async () => {
  const dir = mkdtempSync(join(tmpdir(), 'hzp-shop-'));
  const app = createApp({databasePath:join(dir,'orders.sqlite'),apiBase:'http://127.0.0.1:1',
    apiKey:'hzp_test_'+'b'.repeat(48),publicOrigin:'https://merchant-sandbox.example.test'});
  const port = await listen(app.server);
  try {
    const created = await (await fetch(`http://127.0.0.1:${port}/api/orders`, {method:'POST',headers:{'content-type':'application/json'},
      body:JSON.stringify({productId:'sandbox-mug'})})).json();
    assert.equal(created.status, 'PENDING_CONFIRMATION');
    assert.equal(created.paymentUrl, null);
  } finally { await close(app.server); app.db.close(); rmSync(dir,{recursive:true,force:true}); }
});
