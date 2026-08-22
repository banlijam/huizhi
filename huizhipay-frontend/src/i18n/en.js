window.i18nEn = {
  sidebar: {
    operations: 'Operations',
    onboarding: 'Onboarding & KYB',
    overview: 'Overview',
    factoring: 'Factoring & Settlement',
    ledgerSplits: 'Ledger & Splits',
    settlements: 'Settlements',
    riskAml: 'Risk & AML',
    integrations: 'Integrations',
    settings: 'Settings',
    restricted: 'Restricted',
    bank: 'Bank Accounts',
    fx: 'FX Withdrawals',
    locked: 'Available after licensing',
    pci: 'All card data is tokenized. CVV is never stored in the console.'
  },
  pages: {
    onboarding: {
      title: 'Onboarding & KYB',
      description: 'Minimal KYB flow and settlement wallet binding.'
    },
    merchant: {
      title: 'Merchant Dashboard',
      description: 'Settlement transparency across fiat rails and on-chain destinations.'
    },
    overview: {
      title: 'Control Room',
      description: 'Real-time metrics, transparent ledger, and settlement countdown.'
    },
    factoring: {
      title: 'Factoring & Settlement',
      description: 'Manage factoring limits, chargeback exposure, and T+1 settlements.'
    },
    settlements: {
      title: 'Settlements',
      description: 'Track T+1 settlement status and on-chain finality.'
    },
    integrations: {
      title: 'Integrations',
      description: 'Connect commerce platforms and manage real-time data sync.'
    },
    risk: {
      title: 'Risk & Routing',
      description: 'Configure smart routing, anti-fraud strategy, and team members.'
    },
    settings: {
      title: 'Settings',
      description: 'Manage account preferences, team, and security.'
    }
  },
  header: {
    balance: 'Available Balance',
    topup: 'Top Up',
    langZh: '中文',
    langEn: 'English',
    nextSettlement: 'Next T+1 Settlement in'
  },
  merchant: {
    title: 'Merchant Dashboard',
    subtitle: 'Settlement transparency across fiat rails and on-chain destinations.'
  },
  kpi: {
    todayVolume: {
      label: "Today's Volume",
      vsYesterday: 'vs yesterday'
    },
    netSettlement: {
      label: 'Net Settlement',
      netYield: 'net yield'
    },
    conversionRate: {
      label: 'Conversion Rate',
      silentApproval: 'silent approval'
    },
    amlRisk: {
      label: 'AML Risk Score',
      intercepted: 'Chainalysis intercepted'
    }
  },
  payout: {
    label: 'Payout Strategy',
    title: 'Choose the settlement destination',
    subtitle: 'Switch without changing the buyer-facing checkout.',
    crypto: {
      option: 'Crypto',
      desc: 'Polygon / Stellar smart-contract split → cold wallet'
    },
    fiat: {
      option: 'Fiat',
      desc: 'Stellar SEP-24 anchor → business bank account'
    }
  },
  ledger: {
    sectionLabel: '1:N Split Waterfall',
    title: 'Virtual Ledger',
    allGateways: 'All gateways',
    exportCsv: 'Export CSV',
    table: {
      orderId: 'Order ID',
      fiatIn: 'Fiat In',
      geoGateway: 'Geo Gateway',
      feeSplit: 'Fee Split',
      chainDest: 'Chain Destination',
      chainalysisStatus: 'Chainalysis Status',
      settlementTime: 'Settlement Time'
    }
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
  controlRoom: {
    todayTitle: "Today's Market",
    todayCount: 'Successful Tx Today',
    todayCountUnit: 'tx',
    conversionRate: 'Conversion Rate',
    todayVolume: "Today's Volume",
    countdownTitle: 'Settlement Countdown',
    countdownDesc: 'Next T+1 settlement in',
    countdownUnit: 'h',
    countdownNext: 'Next settlement estimated',
    ledgerTitle: 'Transparent Ledger',
    ledgerDesc: '7:93 split breakdown for every order — what you see is what you get.',
    ledgerExample: 'Example: buyer pays',
    ledgerFee: 'Huizhi Service Fee',
    ledgerNet: 'Your Net Revenue',
    ledgerBuyer: 'Buyer Pays',
    ledgerTable: {
      orderId: 'Order ID',
      gross: 'Buyer Pays',
      fee: 'Fee (7%)',
      net: 'Net (93%)',
      status: 'Status',
      time: 'Time'
    },
    trendTitle: '7-Day Tx & Conversion Trend',
    statusSettled: 'Settled',
    statusPending: 'Pending'
  },
  onboarding: {
    stepperTitle: 'KYB Onboarding Progress',
    step1: 'Basic Info',
    step2: 'Business License',
    step3: 'Legal Rep',
    step4: 'Settlement Pref',
    formTitle: 'Minimal Onboarding Form',
    formDesc: 'No multi-page PDFs. Complete compliance review in 4 steps.',
    company: 'Company Name',
    country: 'Registered Country',
    licenseNo: 'License Number',
    licenseUpload: 'Upload Business License',
    licenseUploadHint: 'Drag a file here or click to upload (PDF / JPG / PNG)',
    legalRep: 'Legal Representative',
    idNo: 'Rep ID Number',
    settlementPref: 'Settlement Preference',
    prefCrypto: 'Crypto (USDT / Polygon)',
    prefFiat: 'Fiat Wire Transfer',
    prev: 'Previous',
    next: 'Next',
    submit: 'Submit for Review',
    statusTitle: 'KYB Review Status',
    statusDraft: 'Draft',
    statusPending: 'In Review',
    statusApproved: 'Approved',
    statusRejected: 'Rejected',
    submittedAt: 'Submitted',
    reviewedAt: 'Reviewed',
    walletTitle: 'Settlement Wallet',
    walletDesc: 'Once bound, T+1 net revenue auto-settles to this address.',
    walletBound: 'Current Settlement Address',
    walletNetwork: 'Network',
    walletType: 'Type',
    walletChange: 'Change',
    connectMetamask: 'Connect MetaMask',
    metamaskDesc: 'One-click authorization to bind Polygon payout address',
    stellarLabel: 'Or enter a Stellar address manually',
    stellarPlaceholder: '56-char address starting with G',
    bindStellar: 'Bind Stellar Address',
    installMetamask: 'MetaMask not detected. Please install the wallet extension first.',
    fieldRequired: 'Please fill in all required fields',
    submittedHint: 'Submitted for review. Please wait for manual review.',
    approvedHint: 'Approved. All features are now available.',
    rejectedHint: 'Rejected. Please revise and resubmit.',
    edit: 'Edit',
    resubmit: 'Resubmit'
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
    routing: 'Smart Routing Settings',
    routingDesc: 'Complex smart routing wrapped as simple toggles. Instant effect.',
    rules: 'Risk Rules',
    rulesDesc: 'Rule changes take effect immediately for all real-time authorization requests.',
    strictMode: 'Strict Anti-Fraud Mode',
    strictModeDesc: 'When on, the system raises the Chainalysis KYT interception threshold and forces 3DS2 verification on all orders.',
    strictModeBadge: 'One-tap Harden',
    blockPrepaid: 'Block Prepaid Cards',
    blockPrepaidDesc: 'Block transactions from prepaid BINs.',
    forceUs3ds: 'Force 3DS for US BINs',
    forceUs3dsDesc: 'Execute challenge flow for US-issued cards.',
    kytScreening: 'Chainalysis KYT Screening',
    kytScreeningDesc: 'Real-time screening of source-of-funds against sanctioned addresses.',
    blockHighRiskRegion: 'Block High-Risk Regions',
    blockHighRiskRegionDesc: 'Block transactions from high-risk jurisdictions.',
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
  payment: {
    nonCustodial: 'Non-Custodial Routing Engine',
    systemStatus: 'System Status: Operational',
    rollingReserve: '(0% Rolling Reserve)',
    buyTitle: 'Buy Crypto',
    buyTitleHighlight: 'Instantly',
    buyDesc: 'Aggregated liquidity from 12+ licensed gateways. Best rate guaranteed by smart routing.',
    buy: 'Buy',
    sell: 'Sell',
    desc: 'onramp',
    youPay: 'You Pay',
    youReceive: 'You Receive',
    liveRate: 'Live Rate',
    smartSplitter: 'Huizhi Smart Splitter',
    transparent: 'Transparent',
    autoRouted: 'Auto-routed via MoonPay + Transak',
    smartRouting: 'Smart order routing & liquidity aggregation',
    merchantNet: 'Merchant Net Payout',
    directWallet: 'Direct to hard wallet',
    settlementFinality: 'Settlement Finality: T+1 | Non-custodial by design',
    checkout: 'Request Secure Checkout API',
    secured3ds: '3DS2 Secured',
    pciDss: 'PCI DSS L1',
    micaLicensed: 'MiCA Licensed',
    poweredBy: 'Powered by Huizhi Smart Routing Engine v5.5',
    copyright: '© 2026 HuizhiPay Technologies. Non-custodial infrastructure.',
    terms: 'Terms',
    privacy: 'Privacy',
    verifying3ds: 'Verifying 3DS2 Signature',
    authenticatingBank: 'Authenticating with issuing bank...',
    secureAddress: 'Secure Address Generated',
    create2Desc: 'CREATE2 deterministic address derived on Tron Network',
    youSend: 'You Send',
    youReceiveLabel: 'You Receive',
    network: 'Network',
    depositAddress: 'Deposit Address',
    addressExpiresIn: 'Address expires in',
    sendExactly: 'Send',
    sendExactlyDesc: 'equivalent to this address. Smart contract will auto-swap and forward to merchant wallet after 1 block confirmation.',
    cancel: 'Cancel',
    sentFunds: "I've Sent the Funds",
    addressCopied: 'Address copied to clipboard!',
    currencies: {
      EUR: 'Euro',
      USD: 'US Dollar',
      GBP: 'British Pound',
      ZAR: 'South African Rand',
      AED: 'UAE Dirham',
      ARS: 'Argentine Peso',
      AUD: 'Australian Dollar',
      BDT: 'Bangladeshi Taka',
      BRL: 'Brazilian Real',
      CAD: 'Canadian Dollar',
      CHF: 'Swiss Franc',
      CNY: 'Chinese Yuan',
      CZK: 'Czech Koruna',
      DKK: 'Danish Krone',
      EGP: 'Egyptian Pound',
      HKD: 'Hong Kong Dollar',
      HUF: 'Hungarian Forint',
      IDR: 'Indonesian Rupiah',
      INR: 'Indian Rupee',
      ILS: 'Israeli Shekel',
      JPY: 'Japanese Yen',
      KRW: 'South Korean Won',
      MYR: 'Malaysian Ringgit',
      MXN: 'Mexican Peso',
      NOK: 'Norwegian Krone',
      NZD: 'New Zealand Dollar',
      PKR: 'Pakistani Rupee',
      PHP: 'Philippine Peso',
      PLN: 'Polish Zloty',
      RON: 'Romanian Leu',
      RUB: 'Russian Ruble',
      SAR: 'Saudi Riyal',
      SEK: 'Swedish Krona',
      SGD: 'Singapore Dollar',
      THB: 'Thai Baht',
      TRY: 'Turkish Lira',
      UAH: 'Ukrainian Hryvnia',
      VND: 'Vietnamese Dong',
      HRK: 'Croatian Kuna',
      BGN: 'Bulgarian Lev',
      LKR: 'Sri Lankan Rupee'
    },
    cryptoNetworks: {
      TRC20: 'Tron (TRC20)',
      Stellar: 'Stellar',
      ERC20: 'Ethereum (ERC20)',
      Bitcoin: 'Bitcoin',
      BitcoinCash: 'Bitcoin Cash',
      Litecoin: 'Litecoin',
      Polygon: 'Polygon',
      BNBChain: 'BNB Chain (BEP20)',
      Solana: 'Solana',
      Arbitrum: 'Arbitrum',
      Optimism: 'Optimism',
      Standalone: 'Network'
    },
    labels: {
      fastest: 'Fastest',
      lowFee: 'Low fee',
      secure: 'Secure'
    },
    gateway: {
      moonpayTransak: 'Auto-routed via MoonPay + Transak',
      mercuryoTransak: 'Auto-routed via Mercuryo + Transak',
      moonpayMercuryo: 'Auto-routed via MoonPay + Mercuryo'
    }
  },
  loading: 'Loading...',
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
    notifications: 'No new notifications',
    walletBound: 'Settlement wallet bound',
    walletMetamask: 'MetaMask authorization successful',
    walletStellar: 'Stellar address bound',
    walletInvalid: 'Invalid Stellar address format',
    onboardingSubmitted: 'KYB submitted, pending review',
    strictModeOn: 'Strict Anti-Fraud Mode on — 3DS2 forced on all orders',
    strictModeOff: 'Strict Anti-Fraud Mode off'
  }
};