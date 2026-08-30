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
    if (!response.ok) return false;
    const body = await response.json();
    return body.code === 200 && Boolean(body.data);
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
  const lang = localStorage.getItem('huizhipay-lang') || 'zh';
  return lang === 'zh' ? 'zh-CN' : 'en-US';
}

async function apiGet(endpoint) {
  try {
    const response = await fetch(getApiUrl(endpoint), {
      credentials: 'include',
      headers: { 'Accept-Language': getLanguageHeader() }
    });
    if (response.ok) {
      const json = await response.json();
      // 后端统一返回 R<T> 包装 { code, data, message }，提取内层 data
      return json.data !== undefined ? json.data : json;
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
    throw new Error(error.message || error.error || `Request failed (${response.status})`);
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
  // 指挥中心 - 今日大盘
  todayCount: 318,
  todayCountChange: 14.2,
  conversionRate: 86.4,
  conversionRateChange: 2.1,
  todayVolume: 42860.50,
  todayVolumeChange: 9.7,
  // 清算倒计时 (距离下一笔 T+1 清算剩余小时数 / 一轮总时长)
  settlementCountdownHours: 6.5,
  settlementCountdownTotal: 24,
  // 透明分账比例 (服务费 / 商户净收益)
  splitRatio: { feeRate: 0.07, netRate: 0.93, feeLabel: '7', netLabel: '93' },
  chartData: {
    labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
    requests: [482, 620, 545, 718, 680, 824, 756],
    approved: [451, 578, 519, 664, 642, 771, 713]
  }
};

// 透明分账账本流水 (Ledger)
const mockLedger = [
  { orderId: 'HP-839201', gross: 100.00, fee: 7.00, net: 93.00, status: 'settled', time: '14:32' },
  { orderId: 'HP-839184', gross: 250.00, fee: 17.50, net: 232.50, status: 'settled', time: '14:18' },
  { orderId: 'HP-839112', gross: 480.00, fee: 33.60, net: 446.40, status: 'pending', time: '13:57' },
  { orderId: 'HP-839086', gross: 75.00, fee: 5.25, net: 69.75, status: 'settled', time: '13:44' },
  { orderId: 'HP-839041', gross: 1200.00, fee: 84.00, net: 1116.00, status: 'pending', time: '13:21' }
];

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
  { id: 'strictMode', name: 'Strict Anti-Fraud Mode', description: 'Raise Chainalysis KYT threshold and force 3DS2 on all orders.', enabled: false, category: 'master' },
  { id: 'blockPrepaid', name: 'Block Prepaid Cards', description: 'Block transactions from prepaid BINs.', enabled: true, category: 'normal' },
  { id: 'forceUs3ds', name: 'Force 3DS for US BINs', description: 'Execute challenge flow for US-issued cards.', enabled: true, category: 'normal' },
  { id: 'kytScreening', name: 'Chainalysis KYT Screening', description: 'Real-time screening of source-of-funds against sanctioned addresses.', enabled: true, category: 'normal' },
  { id: 'blockHighRiskRegion', name: 'Block High-Risk Regions', description: 'Block transactions originating from high-risk jurisdictions.', enabled: false, category: 'normal' }
];

// 入驻与合规 (Onboarding & KYB) 状态
// 枚举与后端保持一致：KybStatus ∈ {DRAFT,PENDING,APPROVED,REJECTED}，SettlementPref ∈ {CRYPTO,FIAT}
const mockOnboarding = {
  status: 'APPROVED',
  currentStep: 4,
  totalSteps: 4,
  company: {
    name: 'HuizhiPay Technologies Ltd.',
    country: 'Singapore',
    licenseNo: 'SG-2026-AC-8392',
    legalRep: 'Zhang Wei',
    idNo: '••••••••2841',
    settlementPref: 'CRYPTO'
  },
  submittedAt: '2026-07-18',
  reviewedAt: '2026-07-20'
};

// 结算钱包绑定状态
const mockWallet = {
  bound: true,
  type: 'metamask',
  network: 'Polygon',
  address: '0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb1'
};

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
  return (data && data.data) || { id, enabled };
}

async function fetchRiskRules() {
  const data = await apiGet('/risk/rules');
  return data;
}

async function toggleRiskRule(id, enabled) {
  const data = await apiPut(`/risk/rules/${id}`, { enabled });
  return (data && data.data) || { id, enabled };
}

async function fetchTeamMembers() {
  const data = await apiGet('/team/members');
  return data;
}

async function inviteTeamMember(email, role) {
  const data = await apiPost('/team/invite', { email, role });
  return (data && data.data) || { success: true };
}

async function fetchUserProfile() {
  const data = await apiGet('/user/profile');
  return data;
}

async function createTopUpInvoice(amount) {
  const data = await apiPost('/topup/invoice', { amount });
  if (data && data.data) return data.data;

  return {
    invoiceId: `INV-${Date.now().toString().slice(-8)}`,
    amount: Number(amount),
    network: 'TRON (TRC20)'
  };
}

// ===== 指挥中心：透明分账账本 =====
async function fetchLedger() {
  const data = await apiGet('/overview/ledger');
  return data;
}

// ===== 入驻与合规 (Onboarding & KYB) =====
async function fetchOnboardingStatus() {
  const data = await apiGet('/onboarding/status');
  return data;
}

async function submitOnboarding(payload) {
  const data = await apiPost('/onboarding/submit', payload);
  return (data && data.data) || { success: true, status: 'pending' };
}

async function fetchWallet() {
  const data = await apiGet('/onboarding/wallet');
  return data;
}

async function bindWallet(type, address, network) {
  const data = await apiPost('/onboarding/wallet/bind', { type, address, network });
  return (data && data.data) || { success: true, bound: true, type, address, network };
}
