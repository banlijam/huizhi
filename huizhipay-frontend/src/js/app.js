let currentLang = 'en';
let currentView = 'overview';

// 检测系统语言，默认中文
function detectSystemLang() {
  const lang = (navigator.language || navigator.userLanguage || 'en').toLowerCase();
  return lang.startsWith('zh') ? 'zh' : 'en';
}

function updateLanguageMenuUI() {
  const label = document.getElementById('current-lang-label');
  if (label) {
    label.textContent = currentLang === 'zh' ? '中文' : 'EN';
  }
  document.querySelectorAll('[data-lang-item]').forEach(btn => {
    const itemLang = btn.getAttribute('data-lang-item');
    if (itemLang === currentLang) {
      btn.classList.add('text-indigo-400', 'bg-slate-800/50');
      btn.classList.remove('text-slate-300');
    } else {
      btn.classList.remove('text-indigo-400', 'bg-slate-800/50');
      btn.classList.add('text-slate-300');
    }
  });
}

async function init() {
  // 优先使用用户保存的语言，否则使用系统语言
  const savedLang = localStorage.getItem('huizhipay-lang');
  if (savedLang && (savedLang === 'zh' || savedLang === 'en')) {
    currentLang = savedLang;
  } else {
    currentLang = detectSystemLang();
  }
  // 先渲染lucide图标，确保新加的图标（globe, chevron-down）能正常显示
  if (window.lucide) {
    lucide.createIcons();
  }
  applyLanguage();
  updateLanguageMenuUI();
  await loadUserProfile();
  // iframe 首次加载完成后隐藏加载指示器
  const iframe = document.getElementById('content-frame');
  if (iframe) {
    iframe.addEventListener('load', hideFrameLoader);
    // 如果 iframe 已经加载完成，立即隐藏加载器
    if (iframe.contentDocument && iframe.contentDocument.readyState === 'complete') {
      hideFrameLoader();
    }
  }
  // 确保初始化时加载器是隐藏的
  hideFrameLoader();
}

function showFrameLoader() {
  const loader = document.getElementById('frame-loader');
  if (loader) {
    // 显示加载器
    loader.classList.remove('hidden');
  }
}

function hideFrameLoader() {
  const loader = document.getElementById('frame-loader');
  if (loader) {
    // 完全隐藏加载器，避免遮挡侧边栏
    loader.classList.add('hidden');
  }
}

async function loadUserProfile() {
  const profile = await fetchUserProfile();
  if (profile && profile.balance !== undefined) {
    const balanceEl = document.getElementById('ui-balance');
    if (balanceEl) {
      balanceEl.textContent = '$' + profile.balance.toLocaleString();
    }
  }
}

function applyLanguage() {
  applyLanguageToPage();
  updateIframeLanguage();
}

function applyLanguageToPage() {
  const t = getTranslations(currentLang);
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    const value = getNestedValue(t, key);
    if (value) {
      el.textContent = value;
    }
  });
  
  const title = getNestedValue(t, `pages.${currentView}.title`);
  const description = getNestedValue(t, `pages.${currentView}.description`);
  const titleEl = document.getElementById('page-title');
  const descEl = document.getElementById('page-description');
  if (title && titleEl) titleEl.textContent = title;
  if (description && descEl) descEl.textContent = description;
}

function updateIframeLanguage() {
  const iframe = document.getElementById('content-frame');
  if (iframe) {
    showFrameLoader();
    const view = currentView || 'overview';
    // 如果 iframe 已经有视图路径，只更新语言参数；否则设置完整路径
    if (iframe.src && iframe.src.includes('views/')) {
      const url = new URL(iframe.src, window.location.href);
      url.searchParams.set('lang', currentLang);
      iframe.src = url.toString();
    } else {
      iframe.src = `views/${view}.html?lang=${currentLang}`;
    }
  }
}

function selectLanguage(lang) {
  currentLang = lang;
  localStorage.setItem('huizhipay-lang', lang);
  applyLanguage();
  updateLanguageMenuUI();
  // 关闭语言菜单
  const menu = document.getElementById('language-menu');
  if (menu) menu.classList.add('hidden');
}

function toggleLanguageMenu() {
  const menu = document.getElementById('language-menu');
  if (menu) menu.classList.toggle('hidden');
}

function switchView(view) {
  currentView = view;
  
  document.querySelectorAll('[data-view]').forEach(btn => {
    btn.classList.remove('active');
    if (btn.dataset.view === view) {
      btn.classList.add('active');
    }
  });
  
  const iframe = document.getElementById('content-frame');
  if (iframe) {
    showFrameLoader();
    iframe.src = `views/${view}.html?lang=${currentLang}`;
  }
  
  const t = getTranslations(currentLang);
  const title = getNestedValue(t, `pages.${view}.title`);
  const description = getNestedValue(t, `pages.${view}.description`);
  if (title) {
    const titleEl = document.getElementById('page-title');
    if (titleEl) titleEl.textContent = title;
  }
  if (description) {
    const descEl = document.getElementById('page-description');
    if (descEl) descEl.textContent = description;
  }
}

function openTopUpDialog() {
  document.getElementById('topup-dialog').classList.remove('hidden');
  document.getElementById('topup-dialog').classList.add('flex');
}

function closeTopUpDialog() {
  document.getElementById('topup-dialog').classList.add('hidden');
  document.getElementById('topup-dialog').classList.remove('flex');
}

function switchTopUpTab(tab) {
  document.getElementById('tab-crypto').className = tab === 'crypto' 
    ? 'flex-1 border-b-2 border-primary py-2 text-sm font-medium text-primary' 
    : 'flex-1 border-b-2 border-transparent py-2 text-sm font-medium text-muted-foreground';
  
  document.getElementById('tab-fiat').className = tab === 'fiat' 
    ? 'flex-1 border-b-2 border-primary py-2 text-sm font-medium text-primary' 
    : 'flex-1 border-b-2 border-transparent py-2 text-sm font-medium text-muted-foreground';
  
  document.getElementById('content-crypto').classList.toggle('hidden', tab !== 'crypto');
  document.getElementById('content-fiat').classList.toggle('hidden', tab !== 'fiat');
}

async function generateInvoice() {
  const amount = document.getElementById('topup-amount').value;
  const btn = document.getElementById('generate-btn');
  const btnText = btn.innerHTML;
  
  btn.disabled = true;
  btn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="animate-spin"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg> Generating...';
  
  try {
    const response = await fetch('/api/v1/topup/invoice', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ amount: Number(amount) })
    });
    
    if (response.ok) {
      const invoice = await response.json();
      displayInvoice(invoice);
      showToast(t('toast.invoiceGenerated', currentLang), `${invoice.amount.toLocaleString()} USDT · ${invoice.network}`);
    } else {
      const error = await response.json();
      throw new Error(error.error || 'Unknown error');
    }
  } catch (error) {
    showToast(t('toast.invoiceError', currentLang), error.message, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = btnText;
  }
}

function displayInvoice(invoice) {
  document.getElementById('topup-form').classList.add('hidden');
  document.getElementById('topup-invoice').classList.remove('hidden');
  document.getElementById('invoice-id').textContent = invoice.invoiceId;
  document.getElementById('invoice-network').textContent = invoice.network;
  document.getElementById('invoice-amount').textContent = `${invoice.amount.toLocaleString()} USDT`;
}

function copyAddress() {
  showToast(t('toast.copied', currentLang));
}

window.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'toast') {
    showToast(event.data.message, event.data.description, event.data.type || 'success');
  }
});

document.addEventListener('DOMContentLoaded', init);

document.addEventListener('click', (e) => {
  const languageMenu = document.getElementById('language-menu');
  const langBtn = document.querySelector('button[onclick="toggleLanguageMenu()"]');
  if (languageMenu && !languageMenu.classList.contains('hidden') && !langBtn.contains(e.target) && !languageMenu.contains(e.target)) {
    languageMenu.classList.add('hidden');
  }
  
  const dialog = document.getElementById('topup-dialog');
  if (dialog && !dialog.classList.contains('hidden') && e.target === dialog) {
    closeTopUpDialog();
  }
});