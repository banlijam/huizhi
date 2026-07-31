window.i18nZh = {
  sidebar: {
    operations: '运营',
    overview: '总览',
    factoring: '保理与结算',
    integrations: '集成',
    risk: '风控与团队',
    restricted: '受限功能',
    bank: '银行账户',
    fx: 'FX 提现',
    locked: '持牌后开放',
    pci: '所有卡数据经令牌化处理，控制台绝不存储 CVV。'
  },
  pages: {
    overview: {
      title: '总览',
      description: '监控支付认证、API 性能与资金健康度。'
    },
    factoring: {
      title: '保理与结算',
      description: '管理保理额度、拒付敞口与 T+1 结算。'
    },
    integrations: {
      title: '集成',
      description: '连接商业平台并管理实时数据同步。'
    },
    risk: {
      title: '风控与团队',
      description: '配置交易规则、访问权限与团队成员。'
    }
  },
  header: {
    balance: '可用余额',
    topup: '充值',
    langZh: '中文',
    langEn: 'English'
  },
  overview: {
    apiCalls: 'API 调用',
    successRate: '成功率',
    authVolume: '认证交易额',
    last24h: '过去 24 小时',
    vsLastPeriod: '较前一周期',
    last7Days: '7 天累计处理额',
    chartTitle: '7 日 3DS 使用量',
    chartDesc: '认证请求与成功认证的实时趋势',
    realtime: '实时',
    logTitle: '近期 3DS 日志',
    chart3dsRequests: '3DS 请求',
    chartApproved: '认证通过',
    weekdays: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    table: {
      time: '时间',
      orderId: '订单 ID',
      card: '卡号',
      provider: '提供商',
      status: '状态',
      cavv: 'CAVV / ECI'
    }
  },
  factoring: {
    chargebackRate: '拒付率',
    factoringLimit: '保理额度',
    pendingNet: '待结算净额',
    belowThreshold: '低于 1.0% 风险阈值',
    used: '已使用 $28,200',
    next48h: '未来 48 小时',
    tableTitle: '即将到账的 T+1 结算',
    tableDesc: '扣除处理费与滚动准备金后的预计净额',
    channels: {
      visaNa: 'Visa · 北美',
      mastercardEu: 'Mastercard · 欧洲',
      localApac: '本地收单 · APAC'
    },
    table: {
      id: '结算 ID',
      date: '到账日',
      channel: '通道',
      gross: '交易总额',
      fees: '费用',
      net: '预计净额',
      status: '状态'
    },
    status: {
      processing: '处理中',
      locked: '已锁定',
      estimated: '预估'
    }
  },
  integrations: {
    running: '运行中',
    disabled: '未启用',
    shopifyDesc: '同步订单、退款与 3DS 验证状态。',
    woocommerceDesc: '通过安全 Webhook 接收实时交易事件。',
    connection: '连接状态',
    lastSync: '最近同步：2 分钟前',
    waiting: '等待连接',
    configure: '配置'
  },
  risk: {
    rules: '风控规则',
    rulesDesc: '规则修改会立即应用于所有实时授权请求。',
    blockPrepaid: '阻止预付卡',
    blockPrepaidDesc: '拦截预付型 BIN 发起的交易。',
    forceUs3ds: '美国 BIN 强制 3DS',
    forceUs3dsDesc: '对美国发卡行交易执行挑战流程。',
    team: '团队邀请',
    teamDesc: '集中管理控制台访问与最小权限角色。',
    invite: '邀请成员',
    table: {
      email: '邮箱',
      role: '角色',
      sent: '发送日期',
      status: '状态'
    },
    role: {
      admin: '运营管理员',
      analyst: '风控分析员',
      readonly: '只读财务'
    },
    status: {
      accepted: '已接受',
      pending: '待接受'
    }
  },
  topup: {
    title: '为 HuizhiPay 余额充值',
    desc: '资金确认后将自动计入可用余额。链上交易通常需要 1–3 次确认。',
    fiat: '法币',
    amount: '充值金额（USDT）',
    min: '最低 10 USDT，仅支持 TRC20 网络。',
    generate: '生成账单',
    waiting: '账单等待付款',
    invoiceId: '账单',
    network: '网络',
    copy: '复制充值地址',
    fiatDesc: '法币电汇通道正在审核中。请联系您的客户成功经理获取专属入金指引。'
  },
  status: {
    verified: '已验证',
    pending: '需审核',
    failed: '失败'
  },
  login: {
    title: '登录',
    subtitle: '商户控制台',
    email: '邮箱',
    password: '密码',
    totp: '双重认证码',
    totpHint: '请输入认证器应用中的6位验证码',
    remember: '记住我',
    forgot: '忘记密码？',
    signin: '登录',
    noAccount: '还没有账户？',
    signup: '创建账户'
  },
  register: {
    title: '创建账户',
    email: '邮箱',
    password: '密码',
    create: '创建账户'
  },
  forgot: {
    title: '重置密码',
    desc: '输入您的邮箱，我们将发送重置链接。',
    email: '邮箱',
    send: '发送重置链接'
  },
  verify: {
    email: {
      verifying: '正在验证您的邮箱...',
      success: '验证成功！',
      successDesc: '您的邮箱已验证。现在可以登录了。',
      login: '去登录',
      error: '验证失败',
      register: '重新注册'
    }
  },
  reset: {
    title: '重置密码',
    password: '新密码',
    confirm: '确认密码',
    submit: '重置密码',
    login: '返回登录'
  },
  payment: {
    nonCustodial: '非托管路由引擎',
    systemStatus: '系统状态：正常运行',
    rollingReserve: '(0% 滚动准备金)',
    buyTitle: '即时购买',
    buyTitleHighlight: '加密货币',
    buyDesc: '聚合 12+ 持牌网关的流动性。智能路由保证最优汇率。',
    buy: '购买',
    sell: '出售',
    desc: '入金',
    youPay: '支付',
    youReceive: '您接收',
    liveRate: '实时汇率',
    smartSplitter: '慧智智能分账器',
    transparent: '透明',
    autoRouted: '通过 MoonPay + Transak 自动路由',
    smartRouting: '智能订单路由与流动性聚合',
    merchantNet: '商户净收款',
    directWallet: '直接进入硬件钱包',
    settlementFinality: '结算周期：T+1 | 设计上非托管',
    checkout: '请求安全结算 API',
    secured3ds: '3DS2 安全保障',
    pciDss: 'PCI DSS L1 认证',
    micaLicensed: 'MiCA 持牌',
    poweredBy: '由慧智智能路由引擎 v5.5 驱动',
    copyright: '© 2026 慧智支付科技。非托管基础设施。',
    terms: '服务条款',
    privacy: '隐私政策',
    verifying3ds: '正在验证 3DS2 签名',
    authenticatingBank: '正在与发卡行进行身份验证...',
    secureAddress: '安全地址已生成',
    create2Desc: '基于 Tron 网络的 CREATE2 确定性地址',
    youSend: '您支付',
    youReceiveLabel: '您接收',
    network: '网络',
    depositAddress: '充值地址',
    addressExpiresIn: '地址将在以下时间后过期',
    sendExactly: '请准确发送',
    sendExactlyDesc: '等值金额至此地址。智能合约将在 1 个区块确认后自动兑换并转发至商户钱包。',
    cancel: '取消',
    sentFunds: '我已完成转账',
    addressCopied: '地址已复制到剪贴板！',
    currencies: {
      EUR: '欧元',
      USD: '美元',
      GBP: '英镑',
      ZAR: '南非兰特',
      AED: '阿联酋迪拉姆',
      ARS: '阿根廷比索',
      AUD: '澳大利亚元',
      BDT: '孟加拉塔卡',
      BRL: '巴西雷亚尔',
      CAD: '加拿大元',
      CHF: '瑞士法郎',
      CNY: '人民币',
      CZK: '捷克克朗',
      DKK: '丹麦克朗',
      EGP: '埃及镑',
      HKD: '港币',
      HUF: '匈牙利福林',
      IDR: '印尼盾',
      INR: '印度卢比',
      ILS: '以色列谢克尔',
      JPY: '日元',
      KRW: '韩元',
      MYR: '马来西亚林吉特',
      MXN: '墨西哥比索',
      NOK: '挪威克朗',
      NZD: '新西兰元',
      PKR: '巴基斯坦卢比',
      PHP: '菲律宾比索',
      PLN: '波兰兹罗提',
      RON: '罗马尼亚列伊',
      RUB: '俄罗斯卢布',
      SAR: '沙特里亚尔',
      SEK: '瑞典克朗',
      SGD: '新加坡元',
      THB: '泰铢',
      TRY: '土耳其里拉',
      UAH: '乌克兰格里夫纳',
      VND: '越南盾',
      HRK: '克罗地亚库纳',
      BGN: '保加利亚列弗',
      LKR: '斯里兰卡卢比'
    },
    cryptoNetworks: {
      TRC20: '波场 (TRC20)',
      Stellar: '恒星币',
      ERC20: '以太坊 (ERC20)',
      Bitcoin: '比特币',
      BitcoinCash: '比特币现金',
      Litecoin: '莱特币',
      Polygon: 'Polygon',
      BNBChain: '币安链 (BEP20)',
      Solana: 'Solana',
      Arbitrum: 'Arbitrum',
      Optimism: 'Optimism',
      Standalone: '网络'
    },
    labels: {
      fastest: '最快',
      lowFee: '低费用',
      secure: '安全'
    },
    gateway: {
      moonpayTransak: '通过 MoonPay + Transak 自动路由',
      mercuryoTransak: '通过 Mercuryo + Transak 自动路由',
      moonpayMercuryo: '通过 MoonPay + Mercuryo 自动路由'
    }
  },
  toast: {
    copied: '充值地址已复制',
    invoiceGenerated: '充值账单已生成',
    invoiceError: '无法生成账单',
    integrationEnabled: '已启用',
    integrationDisabled: '已停用',
    integrationEnabledDesc: '实时同步将在数秒内开始。',
    integrationDisabledDesc: '新的事件将不再写入。',
    ruleEnabled: '已启用',
    ruleDisabled: '已停用',
    inviteCreated: '邀请链接已创建',
    notifications: '暂无新通知'
  }
};