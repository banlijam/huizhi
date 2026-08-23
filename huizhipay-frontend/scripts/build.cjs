#!/usr/bin/env node
/**
 * 构建脚本：将 src/ 打包为独立的 dist/ 发布包。
 *
 * 产物特点：
 *   - 纯静态文件，可直接用 nginx 托管，无需 Node 环境。
 *   - Tailwind / Lucide / Chart.js 等 CDN 依赖会被下载到 dist/vendor/，
 *     所有 HTML 引用将被重写为本地路径，离线也可运行。
 *   - 入口（index.html、login.html、views/*.html、pay/index.html 等）
 *     都保留原有目录结构，方便 nginx 直接 rewrite。
 */

const fs = require('fs');
const path = require('path');
const http = require('http');
const https = require('https');

const ROOT = path.resolve(__dirname, '..');
const SRC_DIR = path.join(ROOT, 'src');
const DIST_DIR = path.join(ROOT, 'dist');
const VENDOR_DIR = path.join(DIST_DIR, 'vendor');

const CDN_ASSETS = [
  {
    name: 'tailwindcss',
    // standalone 构建（浏览器内直接运行，无需构建）
    url: 'https://cdn.tailwindcss.com',
    file: 'tailwindcss.js',
  },
  {
    name: 'lucide',
    // unpkg UMD 版本
    url: 'https://unpkg.com/lucide@latest/dist/umd/lucide.min.js',
    file: 'lucide.min.js',
  },
  {
    name: 'chart.js',
    url: 'https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js',
    file: 'chart.umd.min.js',
  },
];

/** 递归清空并重建目录 */
function resetDir(dir) {
  fs.rmSync(dir, { recursive: true, force: true });
  fs.mkdirSync(dir, { recursive: true });
}

/** 递归拷贝目录 */
function copyDir(src, dest) {
  fs.mkdirSync(dest, { recursive: true });
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);
    if (entry.isDirectory()) {
      copyDir(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

/** 下载文件（支持 http/https，跟随 302） */
function download(url, destPath) {
  return new Promise((resolve, reject) => {
    const protocol = url.startsWith('https') ? https : http;
    const req = protocol.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        // 跟随重定向
        download(new URL(res.headers.location, url).href, destPath).then(resolve, reject);
        return;
      }
      if (res.statusCode !== 200) {
        reject(new Error(`下载失败 ${url} HTTP ${res.statusCode}`));
        return;
      }
      fs.mkdirSync(path.dirname(destPath), { recursive: true });
      const file = fs.createWriteStream(destPath);
      res.pipe(file);
      file.on('finish', () => file.close(resolve));
      file.on('error', reject);
    });
    req.on('error', reject);
    req.setTimeout(30000, () => req.destroy(new Error(`下载超时 ${url}`)));
  });
}

/**
 * 根据 dist 中每个 HTML 文件的位置，计算相对 vendor/ 的引用路径。
 */
function relVendorPath(htmlPath) {
  const depth = htmlPath.replace(DIST_DIR, '').split(path.sep).length - 2;
  const prefix = depth === 0 ? './' : '../'.repeat(depth);
  return {
    tailwind: `${prefix}vendor/tailwindcss.js`,
    lucide: `${prefix}vendor/lucide.min.js`,
    chart: `${prefix}vendor/chart.umd.min.js`,
  };
}

/** 重写 dist 中所有 HTML 的 CDN 引用为本地路径 */
function rewriteHtmlAssets() {
  const htmlFiles = [];
  const walk = (dir) => {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) walk(full);
      else if (entry.name.endsWith('.html')) htmlFiles.push(full);
    }
  };
  walk(DIST_DIR);

  for (const htmlPath of htmlFiles) {
    let html = fs.readFileSync(htmlPath, 'utf-8');
    const vendor = relVendorPath(htmlPath);

    // Tailwind CDN
    html = html.replace(
      /<script[^>]*\ssrc=["']https:\/\/cdn\.tailwindcss\.com["'][^>]*><\/script>/g,
      `<script src="${vendor.tailwind}"></script>`
    );

    // Lucide
    html = html.replace(
      /<script[^>]*\ssrc=["']https:\/\/unpkg\.com\/lucide[^"']*["'][^>]*><\/script>/g,
      `<script src="${vendor.lucide}"></script>`
    );

    // Chart.js
    html = html.replace(
      /<script[^>]*\ssrc=["']https:\/\/[^"']*chart\.js[^"']*["'][^>]*><\/script>/g,
      `<script src="${vendor.chart}"></script>`
    );

    fs.writeFileSync(htmlPath, html, 'utf-8');
  }
}

/** 为 nginx 静态托管生成与本地预览一致的目录入口。 */
function writeRouteEntrypoints() {
  const routes = {
    merchant: 'index.html',
    demo: 'index.html',
    developer: 'developer.html',
  };

  for (const [route, source] of Object.entries(routes)) {
    const routeDir = path.join(DIST_DIR, route);
    fs.mkdirSync(routeDir, { recursive: true });
    fs.copyFileSync(path.join(DIST_DIR, source), path.join(routeDir, 'index.html'));
  }
}

async function build() {
  console.log('🧹 清理 dist 目录...');
  resetDir(DIST_DIR);

  console.log('📦 拷贝 src -> dist...');
  copyDir(SRC_DIR, DIST_DIR);

  console.log('⬇️  下载 CDN 依赖到 dist/vendor/...');
  for (const asset of CDN_ASSETS) {
    const dest = path.join(VENDOR_DIR, asset.file);
    try {
      await download(asset.url, dest);
      console.log(`   ✓ ${asset.name}`);
    } catch (err) {
      console.error(`   ✗ ${asset.name} 下载失败：${err.message}`);
      process.exitCode = 1;
    }
  }

  if (process.exitCode) {
    console.warn('⚠️  部分依赖下载失败，已生成的 dist 仍可使用（需联网）。');
  }

  console.log('✏️  重写 HTML 中的 CDN 引用为本地路径...');
  rewriteHtmlAssets();

  console.log('🧭 生成静态部署路由入口...');
  writeRouteEntrypoints();

  console.log('✅ 构建完成，发布产物位于 dist/');
  console.log('   可直接将 dist/ 拷贝到 nginx 的 root 目录使用。');
}

build().catch((err) => {
  console.error('❌ 构建失败：', err);
  process.exit(1);
});
