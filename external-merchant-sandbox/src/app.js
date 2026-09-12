const http = require('node:http');
const { DatabaseSync } = require('node:sqlite');
const { randomBytes, randomUUID } = require('node:crypto');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');

const PRODUCTS = Object.freeze({
  'sandbox-mug': { name: 'Sandbox Test Mug', amount: '12.00', currency: 'USD' }
});

function createApp(options = {}) {
  const db = new DatabaseSync(options.databasePath || ':memory:');
  const apiBase = options.apiBase || process.env.HUIZHIPAY_API_BASE || 'http://127.0.0.1:14329';
  const apiKey = options.apiKey || process.env.HUIZHIPAY_SANDBOX_API_KEY;
  const publicOrigin = options.publicOrigin || process.env.PUBLIC_HTTPS_ORIGIN;
  const listenHost = options.listenHost || '127.0.0.1';
  const calls = new Map();
  db.exec(`create table if not exists merchant_order (
    id integer primary key, merchant_order_no text not null unique, access_token text not null unique,
    product_id text not null, amount text not null, currency text not null,
    platform_order_no text, payment_url text, status text not null, channel_status text,
    created_at text not null, updated_at text not null
  )`);

  function json(res, status, body) {
    const data = Buffer.from(JSON.stringify(body));
    res.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'content-length': data.length,
      'cache-control': 'no-store', 'x-content-type-options': 'nosniff' });
    res.end(data);
  }
  function page(res) {
    const html = readFileSync(join(__dirname, '..', 'public', 'index.html'));
    res.writeHead(200, { 'content-type': 'text/html; charset=utf-8', 'content-length': html.length,
      'content-security-policy': "default-src 'self'; script-src 'self'; style-src 'self'; base-uri 'none'; frame-ancestors 'none'" });
    res.end(html);
  }
  function asset(res, name, type) {
    const data = readFileSync(join(__dirname, '..', 'public', name));
    res.writeHead(200, { 'content-type': type, 'content-length': data.length, 'x-content-type-options': 'nosniff' });
    res.end(data);
  }
  function allow(ip) {
    const now = Date.now(), prior = calls.get(ip) || [];
    const recent = prior.filter(t => now - t < 60_000);
    if (recent.length >= 30) return false;
    recent.push(now); calls.set(ip, recent); return true;
  }
  async function body(req) {
    let size = 0, chunks = [];
    for await (const chunk of req) {
      size += chunk.length;
      if (size > 8192) throw Object.assign(new Error('Request too large'), { status: 413 });
      chunks.push(chunk);
    }
    try { return JSON.parse(Buffer.concat(chunks).toString('utf8') || '{}'); }
    catch { throw Object.assign(new Error('Invalid JSON'), { status: 400 }); }
  }
  async function platform(path, init = {}) {
    if (!apiKey) throw Object.assign(new Error('Merchant Sandbox API key is not configured'), { status: 503 });
    const response = await fetch(apiBase + path, { ...init, signal: AbortSignal.timeout(8000), headers: {
      'content-type': 'application/json', 'x-huizhipay-sandbox-key': apiKey, ...(init.headers || {})
    }});
    const result = await response.json().catch(() => ({}));
    if (!response.ok || result.code !== 200) throw Object.assign(new Error(result.message || 'Payment platform request failed'), { status: response.status });
    return result.data;
  }
  async function createOrder(req, res) {
    if (!publicOrigin || !publicOrigin.startsWith('https://')) throw Object.assign(new Error('PUBLIC_HTTPS_ORIGIN is required'), { status: 503 });
    const input = await body(req), product = PRODUCTS[input.productId];
    if (!product) throw Object.assign(new Error('Unknown product'), { status: 400 });
    const merchantOrderNo = 'SHOP-' + randomUUID().replaceAll('-', '').slice(0, 20).toUpperCase();
    const accessToken = randomBytes(24).toString('base64url');
    const now = new Date().toISOString();
    db.prepare(`insert into merchant_order(merchant_order_no,access_token,product_id,amount,currency,status,created_at,updated_at)
      values(?,?,?,?,?,'CREATING',?,?)`).run(merchantOrderNo, accessToken, input.productId, product.amount, product.currency, now, now);
    const resultPath = `/orders/${accessToken}`;
    try {
      const payment = await platform('/api/v1/sandbox/payments', { method: 'POST', body: JSON.stringify({
        merchantOrderNo, amount: product.amount, currency: product.currency, productName: product.name,
        successRedirectUrl: publicOrigin + resultPath + '?return=success',
        failureRedirectUrl: publicOrigin + resultPath + '?return=failed'
      })});
      db.prepare(`update merchant_order set platform_order_no=?,payment_url=?,status=?,channel_status=?,updated_at=? where merchant_order_no=?`)
        .run(payment.platformOrderNo, payment.paymentUrl || null, payment.status, payment.channelStatus, new Date().toISOString(), merchantOrderNo);
    } catch (error) {
      db.prepare(`update merchant_order set status='PENDING_CONFIRMATION',channel_status='PLATFORM_UNCERTAIN',updated_at=? where merchant_order_no=?`)
        .run(new Date().toISOString(), merchantOrderNo);
    }
    const row = db.prepare('select * from merchant_order where merchant_order_no=?').get(merchantOrderNo);
    json(res, 201, publicOrder(row));
  }
  async function getOrder(token, res) {
    let row = db.prepare('select * from merchant_order where access_token=?').get(token);
    if (!row) return json(res, 404, { error: 'Order not found' });
    if (row.platform_order_no) {
      try {
        const payment = await platform('/api/v1/sandbox/payments/' + encodeURIComponent(row.merchant_order_no));
        db.prepare(`update merchant_order set payment_url=?,status=?,channel_status=?,updated_at=? where id=?`)
          .run(payment.paymentUrl || row.payment_url, payment.status, payment.channelStatus, new Date().toISOString(), row.id);
        row = db.prepare('select * from merchant_order where id=?').get(row.id);
      } catch { /* Preserve the last durable state when platform refresh fails. */ }
    }
    json(res, 200, publicOrder(row));
  }
  function publicOrder(row) { return { accessToken: row.access_token, merchantOrderNo: row.merchant_order_no,
    product: PRODUCTS[row.product_id], platformOrderNo: row.platform_order_no, paymentUrl: row.payment_url,
    status: row.status, channelStatus: row.channel_status, updatedAt: row.updated_at }; }

  const server = http.createServer(async (req, res) => {
    try {
      if (!allow(req.socket.remoteAddress || 'unknown')) return json(res, 429, { error: 'Too many requests' });
      const url = new URL(req.url, 'http://localhost');
      if (req.method === 'GET' && url.pathname === '/') return page(res);
      if (req.method === 'GET' && url.pathname === '/app.js') return asset(res, 'app.js', 'text/javascript; charset=utf-8');
      if (req.method === 'GET' && url.pathname === '/style.css') return asset(res, 'style.css', 'text/css; charset=utf-8');
      if (req.method === 'POST' && url.pathname === '/api/orders') return await createOrder(req, res);
      const match = url.pathname.match(/^\/api\/orders\/([A-Za-z0-9_-]{32})$/);
      if (req.method === 'GET' && match) return await getOrder(match[1], res);
      if (req.method === 'GET' && url.pathname.startsWith('/orders/')) return page(res);
      return json(res, 404, { error: 'Not found' });
    } catch (error) { return json(res, error.status || 500, { error: error.message }); }
  });
  return { server, db, listenHost };
}

module.exports = { createApp, PRODUCTS };
