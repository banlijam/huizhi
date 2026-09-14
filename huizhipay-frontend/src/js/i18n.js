function getNestedValue(obj, key) {
  return key.split('.').reduce((o, k) => o ? o[k] : null, obj);
}

function getTranslations(lang) {
  return lang === 'zh' ? window.i18nZh : window.i18nEn;
}

function applyLanguage(lang) {
  const t = getTranslations(lang);
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    const value = getNestedValue(t, key);
    if (value) {
      el.textContent = value;
    }
  });
}

function applyI18n() {
  const lang = document.documentElement.lang === 'zh' ? 'zh' : 'en';
  applyLanguage(lang);
  updateLanguageToggle(lang);
}

function updateLanguageToggle(lang) {
  const toggle = document.getElementById('language-toggle');
  if (toggle) {
    const flag = lang === 'zh' ? '🇨🇳' : '🇺🇸';
    const label = lang === 'zh' ? '中文' : 'EN';
    toggle.innerHTML = `${flag} ${label}`;
  }
}

function t(key, lang) {
  const translations = getTranslations(lang || 'en');
  const value = getNestedValue(translations, key);
  // 缺失文案返回空串而非 null：避免渲染成 "null"，也避免对 null 调用 .replace() 导致整页崩溃。
  return value == null ? '' : value;
}