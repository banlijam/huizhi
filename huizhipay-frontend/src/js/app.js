let currentLang = 'zh';
let currentView = 'overview';

async function init() {
  const savedLang = localStorage.getItem('huizhipay-lang');
  if (savedLang && (savedLang === 'zh' || savedLang === 'en')) {
    currentLang = savedLang;
  }
  applyLanguage();
  await loadUserProfile();
  // iframe 首次加载完成后隐藏加载指示器
  const iframe = document.getElementById('content-frame');
  if (iframe) {
    iframe.addEventListener('load', hideFrameLoader);
  }
}

function showFrameLoader() {
  const loader = document.getElementById('frame-loader');
  if (loader) {
    loader.classList.remove('pointer-events-none');
    const spinner = loader.querySelector('i');
    const text = loader.querySelector('span');
    if (spinner) spinner.classList.remove('hidden');
    if (text) text.classList.remove('hidden');
  }
}

function hideFrameLoader() {
  const loader = document.getElementById('frame-loader');
  if (loader) {
    const spinner = loader.querySelector('i');
    const text = loader.querySelector('span');
    // 保留元素但隐藏内容，避免布局抖动
    if (spinner) spinner.classList.add('hidden');
    if (text) text.classList.add('hidden');
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
  if (title) document.getElementById('page-title').textContent = title;
  if (description) {
    const descEl = document.getElementById('page-description');
    if (descEl) descEl.textContent = description;
  }
  
  const langText = currentLang === 'zh' ? '中文' : 'English';
  const currentLangEl = document.getElementById('current-lang');
  if (currentLangEl) currentLangEl.textContent = langText;
}

function updateIframeLanguage() {
  const iframe = document.getElementById('content-frame');
  if (iframe) {
    const currentSrc = iframe.src;
    const url = new URL(currentSrc, window.location.href);
    url.searchParams.set('lang', currentLang);
    iframe.src = url.toString();
  }
}

function switchLanguage(lang) {
  currentLang = lang;
  localStorage.setItem('huizhipay-lang', lang);
  applyLanguage();
}

function toggleLanguageMenu() {
  const menu = document.getElementById('language-menu');
  menu.classList.toggle('hidden');
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