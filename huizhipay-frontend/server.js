const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3000;
const PUBLIC_DIR = path.join(__dirname, process.argv.includes('--dist') ? 'dist' : 'src');
const BACKEND_API = process.env.BACKEND_API || 'http://localhost:8080';

const transactions = [
  { time: '14:32:08', orderId: 'HP-839201', card: '•••• 4242', provider: 'Adyen', status: 'verified', cavvEci: 'AAABBI / 05' },
  { time: '14:18:41', orderId: 'HP-839184', card: '•••• 1881', provider: 'Stripe', status: 'verified', cavvEci: 'kB8F2x / 05' },
  { time: '13:57:22', orderId: 'HP-839112', card: '•••• 9010', provider: 'Checkout', status: 'pending', cavvEci: '— / 07' },
  { time: '13:44:09', orderId: 'HP-839086', card: '•••• 4242', provider: 'Adyen', status: 'verified', cavvEci: 'Y3N2AW / 02' },
  { time: '13:21:35', orderId: 'HP-839041', card: '•••• 0026', provider: 'Stripe', status: 'failed', cavvEci: '— / 00' },
];

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const getContentType = (filePath) => {
  const extname = path.extname(filePath);
  switch (extname) {
    case '.html': return 'text/html; charset=utf-8';
    case '.js': return 'application/javascript; charset=utf-8';
    case '.css': return 'text/css; charset=utf-8';
    case '.json': return 'application/json; charset=utf-8';
    case '.png': return 'image/png';
    case '.jpg': return 'image/jpeg';
    case '.svg': return 'image/svg+xml';
    default: return 'application/octet-stream';
  }
};

const serveStaticFile = (req, res, filePath) => {
  fs.readFile(filePath, (err, content) => {
    if (err) {
      if (err.code === 'ENOENT') {
        res.writeHead(404);
        res.end('File not found');
      } else {
        res.writeHead(500);
        res.end('Server error: ' + err.code);
      }
    } else {
      res.writeHead(200, { 'Content-Type': getContentType(filePath) });
      res.end(content, 'utf-8');
    }
  });
};

function proxyToBackend(req, res) {
  const url = new URL(req.url, BACKEND_API);
  const options = {
    hostname: url.hostname,
    port: url.port,
    path: url.pathname + url.search,
    method: req.method,
    headers: {
      ...req.headers,
      host: url.hostname
    }
  };

  const protocol = url.protocol === 'https:' ? https : http;
  const proxyReq = protocol.request(options, (proxyRes) => {
    res.writeHead(proxyRes.statusCode, proxyRes.headers);
    proxyRes.pipe(res, { end: true });
  });

  proxyReq.on('error', (err) => {
    console.error('Proxy error:', err);
    res.writeHead(503, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Backend service unavailable' }));
  });

  req.pipe(proxyReq, { end: true });
}

const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  // 代理所有 /api/v1 请求到后端
  if (req.url.startsWith('/api/v1')) {
    proxyToBackend(req, res);
    return;
  }

  let pathname = req.url === '/' ? '/index.html' : req.url;
  const queryIndex = pathname.indexOf('?');
  if (queryIndex !== -1) {
    pathname = pathname.substring(0, queryIndex);
  }
  
  let filePath = path.join(PUBLIC_DIR, pathname);
  const extname = path.extname(filePath);
  
  if (!extname) {
    filePath += '.html';
  }

  serveStaticFile(req, res, filePath);
});

server.listen(PORT, () => {
  console.log(`Server running at http://localhost:${PORT}/`);
  console.log(`Serving frontend from ${PUBLIC_DIR}`);
  console.log(`Proxying /api/v1 to ${BACKEND_API}`);
});
