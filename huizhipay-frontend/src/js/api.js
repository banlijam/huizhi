const API_BASE = '/api/v1';

function getApiUrl(endpoint) {
  return `${API_BASE}${endpoint}`;
}

async function login(email, password, totpCode) {
  const data = await apiPost('/auth/login', { email, password, totpCode });
  if (data) {
    if (data.code === 200) {
      return data.data || {};
    }
    throw new Error(data.message || 'Login failed');
  }
  throw new Error('Failed to connect to server');
}

async function register(email, password) {
  const data = await apiPost('/auth/register', { email, password });
  if (data) {
    if (data.code === 200) {
      return true;
    }
    throw new Error(data.message || 'Registration failed');
  }
  throw new Error('Failed to connect to server');
}

async function forgotPassword(email) {
  const data = await apiPost('/auth/forgot-password', { email });
  if (data) {
    if (data.code === 200) {
      return true;
    }
    throw new Error(data.message || 'Failed to send reset link');
  }
  throw new Error('Failed to connect to server');
}

async function isLoggedIn() {
  try {
    const response = await fetch('/api/v1/auth/me', {
      method: 'GET',
      credentials: 'include'
    });
    if (response.ok) {
      return true;
    }
  } catch (e) {
    console.error('Failed to check login status', e);
  }
  return false;
}

async function logout() {
  try {
    await fetch('/api/v1/auth/logout', {
      method: 'POST',
      credentials: 'include'
    });
  } catch (error) {
    console.error('Logout failed:', error);
  } finally {
    window.location.href = 'login.html';
  }
}

function getLanguageHeader() {
  const lang = document.documentElement.lang === 'zh' ? 'zh-CN' : 'en-US';
  return lang;
}

async function apiGet(endpoint) {
  try {
    const response = await fetch(getApiUrl(endpoint), {
      credentials: 'include',
      headers: { 'Accept-Language': getLanguageHeader() }
    });
    if (response.ok) {
      return await response.json();
    }
  } catch (error) {
    console.log(`API GET ${endpoint} failed, using mock data`);
  }
  return null;
}

async function apiPost(endpoint, data) {
  try {
    const response = await fetch(getApiUrl(endpoint), {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'Accept-Language': getLanguageHeader()
      },
      body: JSON.stringify(data),
      credentials: 'include'
    });
    if (response.ok) {
      return await response.json();
    }
    const error = await response.json();
    throw new Error(error.message || 'Unknown error');
  } catch (error) {
    if (!error.message.includes('Failed to fetch')) {
      throw error;
    }
    return null;
  }
}

async function apiPut(endpoint, data) {
  try {
    const response = await fetch(getApiUrl(endpoint), {
      method: 'PUT',
      headers: { 
        'Content-Type': 'application/json',
        'Accept-Language': getLanguageHeader()
      },
      body: JSON.stringify(data),
      credentials: 'include'
    });
    if (response.ok) {
      return await response.json();
    }
    const error = await response.json();
    throw new Error(error.message || 'Unknown error');
  } catch (error) {
    if (!error.message.includes('Failed to fetch')) {
      throw error;
    }
    return null;
  }
}

const mockTransactions = [
  { time: '14:32:08', orderId: 'HP-839201', card: '•••• 4242', provider: 'Adyen', status: 'verified', cavvEci: 'AAABBI / 05' },
  { time: '14:18:41', orderId: 'HP-839184', card: '•••• 1881', provider: 'Stripe', status: 'verified', cavvEci: 'kB8F2x / 05' },
  { time: '13:57:22', orderId: 'HP-839112', card: '•••• 9010', provider: 'Checkout', status: 'pending', cavvEci: '— / 07' },
  { time: '13:44:09', orderId: 'HP-839086', card: '•••• 4242', provider: 'Adyen', status: 'verified', cavvEci: 'Y3N2AW / 02' },
  { time: '13:21:35', orderId: 'HP-839041', card: '•••• 0026', provider: 'Stripe', status: 'failed', cavvEci: '— / 00' },
];

const mockOverviewStats = {
  apiCalls: 4210,
  apiCallsChange: 12.4,
  successRate: 94.2,
  successRateChange: 1.8,
  authVolume: 86400,
  authVolumeChange: 8.1,
  chartData: {
    labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
    requests: [482, 620, 545, 718, 680, 824, 756],
    approved: [451, 578, 519, 664, 642, 771, 713]
  }
};

const mockFactoringStats = {
  chargebackRate: 0.8,
  chargebackRateChange: -0.1,
  factoringLimit: 50000,
  factoringUsed: 28200,
  pendingNet: 37000,
  pendingCount: 3,
  settlements: [
    { id: 'PO-20260723-01', date: '2026-07-23', channel: 'visaNa', gross: 18420.00, fees: 326.14, net: 18093.86, status: 'processing' },
    { id: 'PO-20260723-02', date: '2026-07-23', channel: 'mastercardEu', gross: 11280.00, fees: 198.62, net: 11081.38, status: 'locked' },
    { id: 'PO-20260724-01', date: '2026-07-24', channel: 'localApac', gross: 7940.00, fees: 119.10, net: 7820.90, status: 'estimated' }
  ]
};

const mockIntegrations = [
  {
    id: 'shopify',
    name: 'Shopify',
    description: 'Sync orders, refunds, and 3DS verification status.',
    status: 'running',
    version: 'API v2026-07',
    authType: 'OAuth 2.0',
    lastSync: '2026-07-23T14:30:00Z'
  },
  {
    id: 'woocommerce',
    name: 'WooCommerce',
    description: 'Receive real-time transaction events via secure webhooks.',
    status: 'disabled',
    version: 'API v9.8',
    authType: 'OAuth 2.0',
    lastSync: null
  }
];

const mockRiskRules = [
  { id: 'blockPrepaid', name: 'Block Prepaid Cards', description: 'Block transactions from prepaid BINs.', enabled: true },
  { id: 'forceUs3ds', name: 'Force 3DS for US BINs', description: 'Execute challenge flow for US-issued cards.', enabled: true }
];

const mockTeamMembers = [
  { email: 'ops@huizhipay.org', role: 'admin', sentOn: '2026-07-21', status: 'accepted' },
  { email: 'risk@huizhipay.org', role: 'analyst', sentOn: '2026-07-22', status: 'pending' },
  { email: 'finance@huizhipay.org', role: 'readonly', sentOn: '2026-07-22', status: 'pending' }
];

const mockUserProfile = {
  balance: 12450.00,
  email: 'merchant@example.com',
  company: 'HuizhiPay'
};

async function fetchTransactions() {
  const data = await apiGet('/3ds/transactions');
  return data || mockTransactions;
}

async function fetchOverviewStats() {
  const data = await apiGet('/overview/stats');
  return data || mockOverviewStats;
}

async function fetchFactoringStats() {
  const data = await apiGet('/factoring/stats');
  return data || mockFactoringStats;
}

async function fetchSettlements() {
  const data = await apiGet('/factoring/settlements');
  return data || mockFactoringStats.settlements;
}

async function fetchIntegrations() {
  const data = await apiGet('/integrations');
  return data || mockIntegrations;
}

async function toggleIntegration(id, enabled) {
  const data = await apiPut(`/integrations/${id}`, { enabled });
  return data || { id, enabled };
}

async function fetchRiskRules() {
  const data = await apiGet('/risk/rules');
  return data || mockRiskRules;
}

async function toggleRiskRule(id, enabled) {
  const data = await apiPut(`/risk/rules/${id}`, { enabled });
  return data || { id, enabled };
}

async function fetchTeamMembers() {
  const data = await apiGet('/team/members');
  return data || mockTeamMembers;
}

async function inviteTeamMember(email, role) {
  const data = await apiPost('/team/invite', { email, role });
  return data || { success: true };
}

async function fetchUserProfile() {
  const data = await apiGet('/user/profile');
  return data || mockUserProfile;
}

async function createTopUpInvoice(amount) {
  const data = await apiPost('/topup/invoice', { amount });
  if (data) return data;
  
  return {
    invoiceId: `INV-${Date.now().toString().slice(-8)}`,
    amount: Number(amount),
    network: 'TRON (TRC20)'
  };
}