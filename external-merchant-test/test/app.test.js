const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { mkdtempSync, readFileSync, rmSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { join } = require('node:path');
const { createHmac } = require('node:crypto');
const { createApp } = require('../src/app');

function listen(server) { return new Promise(resolve => server.listen(0, '127.0.0.1', () => resolve(server.address().port))); }
function close(server) { return new Promise(resolve => server.close(resolve)); }

test('buyer is sent directly to the hosted checkout after order creation', () => {
  const script = readFileSync(join(__dirname, '..', 'public', 'app.js'), 'utf8');
  assert.match(script, /if\(o\.paymentUrl\)\{location\.assign\(o\.paymentUrl\);return;\}/);
});

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
    apiKey:'hzp_test_'+'a'.repeat(48),publicOrigin:'https://merchant-test.example.test'});
  const port = await listen(app.server);
  try {
    const createdResponse = await fetch(`http://127.0.0.1:${port}/api/orders`, {method:'POST',headers:{'content-type':'application/json'},
      body:JSON.stringify({productId:'test-mug',amount:'0.01'})});
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
    apiKey:'hzp_test_'+'b'.repeat(48),publicOrigin:'https://merchant-test.example.test'});
  const port = await listen(app.server);
  try {
    const created = await (await fetch(`http://127.0.0.1:${port}/api/orders`, {method:'POST',headers:{'content-type':'application/json'},
      body:JSON.stringify({productId:'test-mug'})})).json();
    assert.equal(created.status, 'PENDING_CONFIRMATION');
    assert.equal(created.paymentUrl, null);
  } finally { await close(app.server); app.db.close(); rmSync(dir,{recursive:true,force:true}); }
});

test('local loopback origin is allowed but non-HTTPS remote origin is rejected', async () => {
  const platform = http.createServer(async (req,res) => {
    let raw=''; for await(const c of req) raw+=c;
    const order=JSON.parse(raw);
    res.writeHead(200,{'content-type':'application/json'});
    res.end(JSON.stringify({code:200,data:{platformOrderNo:'DUMMY-LOCAL',merchantOrderNo:order.merchantOrderNo,
      amount:'12.00',currency:'USD',status:'PENDING',channelStatus:'INITIATED',paymentUrl:'http://127.0.0.1:3000/pay/?checkoutToken=ct_local'}}));
  });
  const platformPort=await listen(platform),dir=mkdtempSync(join(tmpdir(),'hzp-local-origin-'));
  const local=createApp({databasePath:join(dir,'local.sqlite'),apiBase:`http://127.0.0.1:${platformPort}`,
    apiKey:'hzp_test_'+'d'.repeat(48),publicOrigin:'http://127.0.0.1:14330'});
  const remote=createApp({databasePath:join(dir,'remote.sqlite'),apiBase:`http://127.0.0.1:${platformPort}`,
    apiKey:'hzp_test_'+'e'.repeat(48),publicOrigin:'http://merchant.example.test'});
  const localPort=await listen(local.server),remotePort=await listen(remote.server);
  try {
    const body={method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({productId:'test-mug'})};
    assert.equal((await fetch(`http://127.0.0.1:${localPort}/api/orders`,body)).status,201);
    assert.equal((await fetch(`http://127.0.0.1:${remotePort}/api/orders`,body)).status,503);
  } finally {
    await close(local.server); await close(remote.server); await close(platform);
    local.db.close(); remote.db.close(); rmSync(dir,{recursive:true,force:true});
  }
});

test('webhook verifies signature, persists idempotently, and only matching payment updates the order', async () => {
  const platform = http.createServer(async (req,res) => { let raw=''; for await(const c of req)raw+=c;
    const merchantOrderNo=req.method==='POST'?JSON.parse(raw).merchantOrderNo:'SHOP-X';
    res.writeHead(200,{'content-type':'application/json'});res.end(JSON.stringify({code:200,data:{platformOrderNo:'TFI-WEBHOOK',merchantOrderNo,amount:'12.00',currency:'USD',status:'PENDING',channelStatus:'INITIATED'}})); });
  const platformPort=await listen(platform),dir=mkdtempSync(join(tmpdir(),'hzp-webhook-')),secret='whsec_test_secret';
  const app=createApp({databasePath:join(dir,'orders.sqlite'),apiBase:`http://127.0.0.1:${platformPort}`,apiKey:'hzp_test_'+'c'.repeat(48),publicOrigin:'https://merchant-test.example.test',webhookSecret:secret});
  const port=await listen(app.server);
  try{
    const created=await(await fetch(`http://127.0.0.1:${port}/api/orders`,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({productId:'test-mug'})})).json();
    const event={eventId:'evt_stable_1',type:'payment.succeeded',sandbox:true,data:{platformOrderNo:'TFI-WEBHOOK',merchantOrderNo:created.merchantOrderNo,amount:'12.00',currency:'USD',status:'SUCCESS'}};
    const raw=JSON.stringify(event),timestamp=Math.floor(Date.now()/1000).toString(),signature='v1='+createHmac('sha256',secret).update(timestamp+'.'+raw).digest('hex');
    const bad=await fetch(`http://127.0.0.1:${port}/webhooks/huizhipay`,{method:'POST',headers:{'x-huizhipay-timestamp':timestamp,'x-huizhipay-signature':'v1=bad'},body:raw});assert.equal(bad.status,401);
    const send=body=>fetch(`http://127.0.0.1:${port}/webhooks/huizhipay`,{method:'POST',headers:{'x-huizhipay-timestamp':timestamp,'x-huizhipay-signature':'v1='+createHmac('sha256',secret).update(timestamp+'.'+body).digest('hex')},body});
    assert.equal((await send(raw)).status,200);assert.equal((await send(raw)).status,200);
    assert.equal(app.db.prepare('select status from merchant_order where merchant_order_no=?').get(created.merchantOrderNo).status,'SUCCESS');
    assert.equal((await send(JSON.stringify({...event,type:'payment.failed'}))).status,409);
    assert.equal(app.db.prepare('select count(*) n from webhook_event').get().n,1);
  }finally{await close(app.server);await close(platform);app.db.close();rmSync(dir,{recursive:true,force:true});}
});

test('test notification is persisted without changing an order', async () => {
  const dir=mkdtempSync(join(tmpdir(),'hzp-webhook-test-')),secret='whsec_test_secret';
  const app=createApp({databasePath:join(dir,'orders.sqlite'),publicOrigin:'https://merchant-test.example.test',webhookSecret:secret});const port=await listen(app.server);
  try{const event={eventId:'evt_test',type:'webhook.test',data:{message:'test'}},raw=JSON.stringify(event),timestamp=Math.floor(Date.now()/1000).toString(),signature='v1='+createHmac('sha256',secret).update(timestamp+'.'+raw).digest('hex');
    assert.equal((await fetch(`http://127.0.0.1:${port}/webhooks/huizhipay`,{method:'POST',headers:{'x-huizhipay-timestamp':timestamp,'x-huizhipay-signature':signature},body:raw})).status,200);
    assert.equal(app.db.prepare('select count(*) n from merchant_order').get().n,0);
  }finally{await close(app.server);app.db.close();rmSync(dir,{recursive:true,force:true});}
});
