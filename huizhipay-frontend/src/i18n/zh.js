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