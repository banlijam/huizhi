window.i18nEn = {
  sidebar: {
    operations: 'Operations',
    overview: 'Overview',
    factoring: 'Factoring & Settlement',
    integrations: 'Integrations',
    risk: 'Risk & Team',
    restricted: 'Restricted',
    bank: 'Bank Accounts',
    fx: 'FX Withdrawals',
    locked: 'Available after licensing',
    pci: 'All card data is tokenized. CVV is never stored in the console.'
  },
  pages: {
    overview: {
      title: 'Overview',
      description: 'Monitor payment authentication, API performance, and fund health.'
    },
    factoring: {
      title: 'Factoring & Settlement',
      description: 'Manage factoring limits, chargeback exposure, and T+1 settlements.'
    },
    integrations: {
      title: 'Integrations',
      description: 'Connect commerce platforms and manage real-time data sync.'
    },
    risk: {
      title: 'Risk & Team',
      description: 'Configure transaction rules, access permissions, and team members.'
    }
  },
  header: {
    balance: 'Available Balance',
    topup: 'Top Up',
    langZh: '中文',
    langEn: 'English'
  },
  overview: {
    apiCalls: 'API Calls',
    successRate: 'Success Rate',
    authVolume: 'Auth Volume',
    last24h: 'Last 24 hours',
    vsLastPeriod: 'vs last period',
    last7Days: '7-day cumulative',
    chartTitle: '7-Day 3DS Usage',
    chartDesc: 'Real-time trends for authentication requests and approvals',
    realtime: 'Live',
    logTitle: 'Recent 3DS Logs',
    chart3dsRequests: '3DS Requests',
    chartApproved: 'Approved',
    weekdays: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
    table: {
      time: 'Time',
      orderId: 'Order ID',
      card: 'Card',
      provider: 'Provider',
      status: 'Status',
      cavv: 'CAVV / ECI'
    }
  },
  factoring: {
    chargebackRate: 'Chargeback Rate',
    factoringLimit: 'Factoring Limit',
    pendingNet: 'Pending Net',
    belowThreshold: 'Below 1.0% risk threshold',
    used: 'Used $28,200',
    next48h: 'Next 48 hours',
    tableTitle: 'Upcoming T+1 Settlements',
    tableDesc: 'Estimated net after fees and rolling reserves',
    channels: {
      visaNa: 'Visa · North America',
      mastercardEu: 'Mastercard · Europe',
      localApac: 'Local Acquiring · APAC'
    },
    table: {
      id: 'Settlement ID',
      date: 'Settlement Date',
      channel: 'Channel',
      gross: 'Gross',
      fees: 'Fees',
      net: 'Estimated Net',
      status: 'Status'
    },
    status: {
      processing: 'Processing',
      locked: 'Locked',
      estimated: 'Estimated'
    }
  },
  integrations: {
    running: 'Running',
    disabled: 'Disabled',
    shopifyDesc: 'Sync orders, refunds, and 3DS verification status.',
    woocommerceDesc: 'Receive real-time transaction events via secure webhooks.',
    connection: 'Connection Status',
    lastSync: 'Last sync: 2 minutes ago',
    waiting: 'Waiting for connection',
    configure: 'Configure'
  },
  risk: {
    rules: 'Risk Rules',
    rulesDesc: 'Rule changes take effect immediately for all real-time authorization requests.',
    blockPrepaid: 'Block Prepaid Cards',
    blockPrepaidDesc: 'Block transactions from prepaid BINs.',
    forceUs3ds: 'Force 3DS for US BINs',
    forceUs3dsDesc: 'Execute challenge flow for US-issued cards.',
    team: 'Team Invites',
    teamDesc: 'Centralized access control with minimum privilege roles.',
    invite: 'Invite Member',
    table: {
      email: 'Email',
      role: 'Role',
      sent: 'Sent On',
      status: 'Status'
    },
    role: {
      admin: 'Operations Admin',
      analyst: 'Risk Analyst',
      readonly: 'Read-only Finance'
    },
    status: {
      accepted: 'Accepted',
      pending: 'Pending'
    }
  },
  topup: {
    title: 'Top Up HuizhiPay Balance',
    desc: 'Funds will be automatically credited upon confirmation. On-chain transactions typically require 1-3 confirmations.',
    fiat: 'Fiat',
    amount: 'Top Up Amount (USDT)',
    min: 'Minimum 10 USDT. TRC20 network only.',
    generate: 'Generate Invoice',
    waiting: 'Invoice Waiting for Payment',
    invoiceId: 'Invoice',
    network: 'Network',
    copy: 'Copy Deposit Address',
    fiatDesc: 'Fiat wire transfer channel is under review. Please contact your customer success manager for dedicated funding instructions.'
  },
  status: {
    verified: 'Verified',
    pending: 'Pending Review',
    failed: 'Failed'
  },
  login: {
    title: 'Sign In',
    subtitle: 'Merchant Console',
    email: 'Email',
    password: 'Password',
    totp: 'Two-Factor Code',
    totpHint: 'Enter the 6-digit code from your authenticator app',
    remember: 'Remember me',
    forgot: 'Forgot password?',
    signin: 'Sign In',
    noAccount: "Don't have an account?",
    signup: 'Create account'
  },
  register: {
    title: 'Create Account',
    email: 'Email',
    password: 'Password',
    create: 'Create Account'
  },
  forgot: {
    title: 'Reset Password',
    desc: 'Enter your email and we\'ll send you a reset link.',
    email: 'Email',
    send: 'Send Reset Link'
  },
  verify: {
    email: {
      verifying: 'Verifying your email...',
      success: 'Verification Successful!',
      successDesc: 'Your email has been verified. You can now log in.',
      login: 'Go to Login',
      error: 'Verification Failed',
      register: 'Register Again'
    }
  },
  reset: {
    title: 'Reset Password',
    password: 'New Password',
    confirm: 'Confirm Password',
    submit: 'Reset Password',
    login: 'Back to Login'
  },
  toast: {
    copied: 'Deposit address copied',
    invoiceGenerated: 'Invoice generated',
    invoiceError: 'Failed to generate invoice',
    integrationEnabled: 'enabled',
    integrationDisabled: 'disabled',
    integrationEnabledDesc: 'Real-time sync will start within seconds.',
    integrationDisabledDesc: 'New events will no longer be processed.',
    ruleEnabled: 'enabled',
    ruleDisabled: 'disabled',
    inviteCreated: 'Invite link created',
    notifications: 'No new notifications'
  }
};