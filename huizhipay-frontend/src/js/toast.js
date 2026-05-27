function showToast(message, description = '', type = 'success') {
  const container = document.getElementById('toast-container');
  if (!container) return;
  
  const toastEl = document.createElement('div');
  toastEl.className = `flex items-center gap-3 rounded-lg border bg-slate-900 border-slate-800 px-4 py-3 shadow-lg transition-all`;
  
  let icon = '';
  if (type === 'success') {
    icon = '<i data-lucide="check-circle" class="w-4 h-4 text-green-400"></i>';
  } else if (type === 'error') {
    icon = '<i data-lucide="x-circle" class="w-4 h-4 text-red-400"></i>';
  }
  
  toastEl.innerHTML = `
    ${icon}
    <div>
      <p class="text-sm font-medium text-slate-200">${message}</p>
      ${description ? `<p class="text-xs text-slate-400">${description}</p>` : ''}
    </div>
  `;
  
  container.appendChild(toastEl);
  
  lucide.createIcons();
  
  setTimeout(() => {
    toastEl.classList.add('opacity-0', 'translate-x-full');
    setTimeout(() => toastEl.remove(), 300);
  }, 3000);
}