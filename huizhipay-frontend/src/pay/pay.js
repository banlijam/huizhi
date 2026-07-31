// Fixed fee rate (7%)
const FEE_RATE = 0.07;

// I18n Configuration
let currentLang = localStorage.getItem('lang') || 'en';

function getI18nData() {
    return currentLang === 'zh' ? window.i18nZh : window.i18nEn;
}

function translate(key) {
    const keys = key.split('.');
    let data = getI18nData();
    for (const k of keys) {
        if (data && data[k]) {
            data = data[k];
        } else {
            return key;
        }
    }
    return data;
}

function applyI18n() {
    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        const translated = translate(key);
        if (typeof translated === 'string') {
            el.textContent = translated;
        }
    });
    document.documentElement.lang = currentLang === 'zh' ? 'zh' : 'en';
    
    // Update language switcher active state
    const langEn = document.getElementById('lang-en');
    const langZh = document.getElementById('lang-zh');
    if (langEn && langZh) {
        if (currentLang === 'en') {
            langEn.classList.add('bg-white', 'shadow-sm');
            langZh.classList.remove('bg-white', 'shadow-sm');
        } else {
            langZh.classList.add('bg-white', 'shadow-sm');
            langEn.classList.remove('bg-white', 'shadow-sm');
        }
    }
}

function setLanguage(lang) {
    currentLang = lang;
    localStorage.setItem('lang', lang);
    applyI18n();
    calculateCrypto();
}

// Make setLanguage globally accessible
window.setLanguage = setLanguage;

// Initialize i18n
applyI18n();

// State management (values populated from API responses)
const state = {
    fiatAmount: 0,
    fiatCurrency: null,
    fiatSymbol: null,
    fiatRate: null,
    cryptoType: null,
    cryptoNetwork: null,
    cryptoChain: null,
    feePanelOpen: false,
    supportedCurrencies: [],
    currencyPage: 1,
    currencyTotalPages: 1,
    hasMoreCurrencies: false,
    paymentMethods: [],
    paymentMethodPage: 1,
    paymentMethodTotalPages: 1,
    hasMorePaymentMethods: false,
    tokens: [],
    tokenPage: 1,
    tokenTotalPages: 1,
    hasMoreTokens: false,
    combinedCryptoOptions: [],
    orderType: 'onramp',
    isBuyMode: true,
    lastExchangeRate: null,
    isLoading: true
};

// API Configuration
const apiConfig = {
    baseUrl: 'https://sandbox-api.transfi.com/v3',
    // TODO: 后续从接口获取以下配置
    mid: 'HODQSB_NA_NA',
    authorization: 'aG9uZ2tvbmdodWl6aGl0ZWNobm9sb2d5ZGV2ZWxvcG1lbnRsaW1pdGVkOnpsTkdXaVFBT3dpcFN3'
};

// API helper functions
async function fetchSupportedCurrencies(page = 1, limit = 20) {
    try {
        const response = await fetch(
            `${apiConfig.baseUrl}/config/supported-currencies?direction=deposit&userType=individual&limit=${limit}&page=${page}`,
            {
                method: 'GET',
                headers: {
                    'MID': apiConfig.mid,
                    'accept': 'application/json',
                    'authorization': `Basic ${apiConfig.authorization}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        if (result.status === 'success') {
            return result;
        }
        throw new Error('API returned non-success status');
    } catch (error) {
        console.error('Failed to fetch supported currencies:', error);
        return null;
    }
}

// Fetch payment methods
async function fetchPaymentMethods(page = 1, limit = 20) {
    try {
        const response = await fetch(
            `${apiConfig.baseUrl}/config/payment-methods?direction=deposit&limit=${limit}&page=${page}&headlessMode=false&userType=individual`,
            {
                method: 'GET',
                headers: {
                    'MID': apiConfig.mid,
                    'accept': 'application/json',
                    'authorization': `Basic ${apiConfig.authorization}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        if (result.status === 'success') {
            return result;
        }
        throw new Error('API returned non-success status');
    } catch (error) {
        console.error('Failed to fetch payment methods:', error);
        return null;
    }
}

// Fetch tokens
async function fetchTokens(page = 1, limit = 20) {
    try {
        const response = await fetch(
            `${apiConfig.baseUrl}/config/list-tokens?direction=deposit&limit=${limit}&page=${page}`,
            {
                method: 'GET',
                headers: {
                    'MID': apiConfig.mid,
                    'accept': 'application/json',
                    'authorization': `Basic ${apiConfig.authorization}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        if (result.status === 'success') {
            return result;
        }
        throw new Error('API returned non-success status');
    } catch (error) {
        console.error('Failed to fetch tokens:', error);
        return null;
    }
}

// Fetch exchange rates
async function fetchExchangeRate(amount, sourceCurrency, destinationCurrency, orderType) {
    try {
        const params = new URLSearchParams({
            amount: amount.toFixed(2),
            sourceCurrency: sourceCurrency,
            destinationCurrency: destinationCurrency,
            direction: 'forward',
            orderType: orderType || 'onramp',
            paymentCode: 'DEFAULT',
            paymentType: 'DEFAULT'
        });

        const response = await fetch(
            `${apiConfig.baseUrl}/exchange-rates?${params.toString()}`,
            {
                method: 'GET',
                headers: {
                    'MID': apiConfig.mid,
                    'accept': 'application/json',
                    'authorization': `Basic ${apiConfig.authorization}`
                }
            }
        );

        if (!response.ok) {
            const errorBody = await response.text();
            let errorMsg = '';
            try {
                const errorJson = JSON.parse(errorBody);
                errorMsg = errorJson.error?.context?.details?.[0]?.message ||
                          errorJson.error?.message ||
                          errorJson.message ||
                          errorJson.error ||
                          errorBody;
            } catch (e) {
                errorMsg = errorBody;
            }
            console.warn(`Exchange rate API ${response.status}: ${errorMsg}`, {
                sourceCurrency,
                destinationCurrency,
                amount
            });
            return { error: true, status: response.status, message: errorMsg };
        }

        const result = await response.json();
        // API returns data directly (not wrapped in {status, data})
        if (result.sourceCurrency || result.destinationCurrency || result.conversionRate) {
            return result;
        }
        // Fallback for wrapped response format
        if (result.status === 'success') {
            return result.data;
        }
        if (result.status === 'failure') {
            return { error: true, message: result.error?.message || 'API returned failure' };
        }
        throw new Error('API returned non-success status');
    } catch (error) {
        console.error('Failed to fetch exchange rate:', error);
        return null;
    }
}

// Get currency symbol from code (first character as fallback)
function getCurrencySymbol(code) {
    return code ? code.charAt(0) : '?';
}

// Get currency initial for display
function getCurrencyInitial(code) {
    return code ? code.charAt(0) : '?';
}

// Render currency list from API data
function renderCurrencyOptions(currencies, isAppend = false) {
    if (!isAppend) {
        fiatDropdown.querySelector('.p-2').innerHTML = '';
    }

    const container = fiatDropdown.querySelector('.p-2');
    currencies.forEach((item) => {
        const symbol = getCurrencySymbol(item.currency);
        const isSelected = state.fiatCurrency === item.currency;
        const button = document.createElement('button');
        button.className = 'fiat-option w-full flex items-center gap-3 p-3 rounded-xl hover:bg-indigo-50 transition-colors duration-150';
        button.dataset.currency = item.currency;
        button.dataset.symbol = symbol;
        button.dataset.rate = (state.fiatRate || 1.0).toString();
        
        // Get display name with fallback
        const i18nKey = `payment.currencies.${item.currency}`;
        const translated = translate(i18nKey);
        const displayName = translated === i18nKey ? (item.name || item.currency) : translated;
        const hasTranslation = translated !== i18nKey;
        
        button.innerHTML = `
            <div class="w-9 h-9 rounded-full bg-gradient-to-br from-blue-100 to-indigo-100 flex items-center justify-center font-bold text-indigo-700 overflow-hidden">
                ${item.logoUrl 
                    ? `<img src="${item.logoUrl}" alt="${item.currency}" class="w-full h-full object-cover" data-fallback="${symbol}">`
                    : symbol}
            </div>
            <div class="flex-1 text-left">
                <div class="font-semibold text-slate-800 text-sm">${item.currency}</div>
                <div class="text-xs text-slate-500"${hasTranslation ? ` data-i18n="${i18nKey}"` : ''}>${displayName}</div>
            </div>
            <div class="w-2 h-2 rounded-full bg-indigo-500 fiat-check ${isSelected ? 'opacity-100' : 'opacity-0'}"></div>
        `;
        container.appendChild(button);
        
        // Add error handler for logo image
        const img = button.querySelector('img[data-fallback]');
        if (img) {
            img.addEventListener('error', () => {
                img.parentElement.textContent = img.dataset.fallback;
            });
        }
    });

    bindFiatOptionEvents();
}

// Bind click events to fiat options
function bindFiatOptionEvents() {
    document.querySelectorAll('.fiat-option').forEach(option => {
        option.addEventListener('click', () => {
            const currency = option.dataset.currency;
            const symbol = option.dataset.symbol;
            const rate = parseFloat(option.dataset.rate);

            state.fiatCurrency = currency;
            state.fiatSymbol = symbol;
            state.fiatRate = rate;

            document.getElementById('fiat-symbol').textContent = currency;
            
            // Update flag display
            const flagEl = document.getElementById('fiat-flag');
            const selectedItem = state.supportedCurrencies.find(c => c.currency === currency);
            if (selectedItem && selectedItem.logoUrl) {
                flagEl.innerHTML = `<img src="${selectedItem.logoUrl}" alt="${currency}" class="w-full h-full object-cover rounded-full" data-fallback="${symbol}">`;
                const img = flagEl.querySelector('img');
                img.addEventListener('error', () => {
                    img.outerHTML = symbol;
                });
            } else {
                flagEl.textContent = symbol;
            }

            document.querySelectorAll('.fiat-check').forEach(check => {
                check.classList.add('opacity-0');
                check.classList.remove('opacity-100');
            });
            const checkEl = option.querySelector('.fiat-check');
            if (checkEl) {
                checkEl.classList.remove('opacity-0');
                checkEl.classList.add('opacity-100');
            }

            closeAllDropdowns();
            calculateCrypto();
        });
    });
}

// Load currencies from API
async function loadCurrencies() {
    const result = await fetchSupportedCurrencies(1, 50);
    
    if (result && result.status === 'success') {
        state.supportedCurrencies = result.data;
        state.currencyTotalPages = result.pagination.pages;
        state.hasMoreCurrencies = result.pagination.hasNext;
        
        renderCurrencyOptions(result.data);
        
        // Set USD as default if available, otherwise first currency
        const usdCurrency = result.data.find(c => c.currency === 'USD');
        const defaultCurrency = usdCurrency || result.data[0];
        if (defaultCurrency) {
            state.fiatCurrency = defaultCurrency.currency;
            state.fiatSymbol = getCurrencySymbol(defaultCurrency.currency);
            
            document.getElementById('fiat-symbol').textContent = defaultCurrency.currency;
            const flagEl = document.getElementById('fiat-flag');
            if (defaultCurrency.logoUrl) {
                flagEl.innerHTML = `<img src="${defaultCurrency.logoUrl}" alt="${defaultCurrency.currency}" class="w-full h-full object-cover rounded-full" data-fallback="${state.fiatSymbol}">`;
                const img = flagEl.querySelector('img');
                img.addEventListener('error', () => {
                    img.outerHTML = img.dataset.fallback;
                });
            } else {
                flagEl.textContent = state.fiatSymbol;
            }
        }
    } else {
        console.error('Failed to load currencies from API');
        fiatDropdown.querySelector('.p-2').innerHTML = '<div class="p-4 text-center text-sm text-red-500">Failed to load currencies. Please refresh.</div>';
    }
}

// Load more currencies (pagination)
async function loadMoreCurrencies() {
    if (!state.hasMoreCurrencies) return;
    
    const nextPage = state.currencyPage + 1;
    const result = await fetchSupportedCurrencies(nextPage, 20);
    
    if (result && result.status === 'success') {
        state.supportedCurrencies = [...state.supportedCurrencies, ...result.data];
        state.currencyPage = nextPage;
        state.currencyTotalPages = result.pagination.pages;
        state.hasMoreCurrencies = result.pagination.hasNext;
        
        renderCurrencyOptions(result.data, true);
    }
}

// Token metadata map (populated from API data, with auto-generation for unknown tokens)
const tokenMetaMap = {};

// Build token metadata map from API tokens
function buildTokenMetaMap(tokens) {
    tokens.forEach(token => {
        const symbol = token.symbol || token.token || token.code;
        const name = token.name || token.tokenName || symbol;
        tokenMetaMap[symbol] = {
            symbol: symbol,
            name: name,
            logoUrl: token.logoUrl || token.iconUrl || null,
            colorClass: token.colorClass || generateColorClass(symbol),
            icon: token.icon || symbol.charAt(0)
        };
    });
}

// Generate a stable color class for unknown tokens
function generateColorClass(symbol) {
    const colorSchemes = [
        'from-slate-400 to-gray-500',
        'from-teal-400 to-cyan-500',
        'from-pink-400 to-rose-500',
        'from-lime-400 to-green-500',
        'from-amber-400 to-orange-500',
        'from-violet-400 to-purple-500',
        'from-sky-400 to-blue-500',
        'from-fuchsia-400 to-pink-500'
    ];
    let hash = 0;
    for (let i = 0; i < symbol.length; i++) {
        hash = symbol.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colorSchemes[Math.abs(hash) % colorSchemes.length];
}

// Get or create token metadata
function getTokenMeta(symbol, logoUrl) {
    if (tokenMetaMap[symbol]) {
        if (logoUrl && !tokenMetaMap[symbol].logoUrl) {
            tokenMetaMap[symbol].logoUrl = logoUrl;
        }
        return tokenMetaMap[symbol];
    }
    // Auto-generate metadata for unknown tokens
    const meta = {
        symbol: symbol,
        name: symbol,
        logoUrl: logoUrl || null,
        colorClass: generateColorClass(symbol),
        icon: symbol.charAt(0)
    };
    tokenMetaMap[symbol] = meta;
    return meta;
}

// Combine tokens with payment methods to create crypto options
function buildCombinedCryptoOptions(tokens, paymentMethods) {
    const options = [];
    const seen = new Set();

    tokens.forEach(token => {
        const symbol = token.symbol || token.token || token.code;
        const meta = getTokenMeta(symbol, token.logoUrl);
        
        // Find matching payment methods for this token
        const matchingMethods = paymentMethods.filter(pm => {
            const pmCrypto = pm.cryptoType || pm.currency || pm.token;
            return pmCrypto === symbol || 
                   pmCrypto === token.name ||
                   (pm.id && pm.id.includes(symbol));
        });

        if (matchingMethods.length > 0) {
            matchingMethods.forEach(method => {
                const networkKey = method.network || method.paymentMethodId?.split('_')[1] || 'ERC20';
                const chain = method.chain || method.network || 'Unknown';
                const uniqueKey = `${symbol}_${networkKey}`;
                
                if (!seen.has(uniqueKey)) {
                    seen.add(uniqueKey);
                    const networkLabels = {
                        'TRC20': 'payment.cryptoNetworks.TRC20',
                        'ERC20': 'payment.cryptoNetworks.ERC20',
                        'Stellar': 'payment.cryptoNetworks.Stellar',
                        'Bitcoin': 'payment.cryptoNetworks.Bitcoin',
                        'BitcoinCash': 'payment.cryptoNetworks.BitcoinCash',
                        'Litecoin': 'payment.cryptoNetworks.Litecoin',
                        'Polygon': 'payment.cryptoNetworks.Polygon',
                        'BNBChain': 'payment.cryptoNetworks.BNBChain',
                        'Solana': 'payment.cryptoNetworks.Solana',
                        'Arbitrum': 'payment.cryptoNetworks.Arbitrum',
                        'Optimism': 'payment.cryptoNetworks.Optimism'
                    };
                    const networkTagMap = {
                        'TRC20': 'payment.labels.fastest',
                        'Stellar': 'payment.labels.lowFee',
                        'ERC20': 'payment.labels.secure',
                        'Bitcoin': 'payment.labels.secure',
                        'BitcoinCash': 'payment.labels.fastest',
                        'Litecoin': 'payment.labels.lowFee',
                        'Polygon': 'payment.labels.fastest',
                        'BNBChain': 'payment.labels.lowFee',
                        'Solana': 'payment.labels.fastest',
                        'Arbitrum': 'payment.labels.secure',
                        'Optimism': 'payment.labels.secure'
                    };
                    
                    options.push({
                        id: method.id || method.paymentMethodId || uniqueKey,
                        cryptoType: symbol,
                        network: networkKey,
                        chain: chain,
                        label: networkLabels[networkKey] || `payment.cryptoNetworks.${networkKey}`,
                        tag: networkTagMap[networkKey] || 'payment.labels.secure',
                        icon: meta.logoUrl 
                            ? `<img src="${meta.logoUrl}" alt="${symbol}" class="w-5 h-5 rounded-full" data-fallback-icon='${meta.icon}'>`
                            : `<span class="text-white text-sm font-bold">${meta.icon}</span>`,
                        colorClass: meta.colorClass,
                        logoUrl: meta.logoUrl,
                        tokenName: meta.name,
                        displayName: meta.name || chain || symbol
                    });
                }
            });
        } else {
            // Token without matching payment method - still add it as standalone option
            const uniqueKey = `${symbol}_standalone`;
            if (!seen.has(uniqueKey)) {
                seen.add(uniqueKey);
                options.push({
                        id: uniqueKey,
                        cryptoType: symbol,
                        network: 'Standalone',
                        chain: meta.name,
                        label: `payment.cryptoNetworks.${symbol}`,
                        tag: '',
                        icon: meta.logoUrl 
                            ? `<img src="${meta.logoUrl}" alt="${symbol}" class="w-5 h-5 rounded-full" data-fallback-icon='${meta.icon}'>`
                            : `<span class="text-white text-sm font-bold">${meta.icon}</span>`,
                        colorClass: meta.colorClass,
                        logoUrl: meta.logoUrl,
                        tokenName: meta.name,
                        displayName: meta.name || symbol
                    });
            }
        }
    });

    return options;
}

// Get display label for crypto option (tries i18n first, falls back to name/network)
function getOptionDisplayLabel(item) {
    const translated = translate(item.label);
    // If translate returns the key itself, translation doesn't exist
    if (translated === item.label) {
        // Fallback: use pre-built displayName, or tokenName, or chain, or type
        return item.displayName || item.tokenName || item.chain || item.cryptoType;
    }
    return translated;
}

// Get display tag for crypto option
function getOptionDisplayTag(item) {
    const translated = translate(item.tag);
    if (translated === item.tag) {
        return '';
    }
    return translated;
}

// Render crypto option list
function renderCryptoOptions(options, isAppend = false) {
    if (!isAppend) {
        cryptoDropdown.querySelector('.p-2').innerHTML = '';
    }

    if (!options || options.length === 0) {
        cryptoDropdown.querySelector('.p-2').innerHTML = '<div class="p-4 text-center text-sm text-slate-400">No payment methods available</div>';
        return;
    }

    const container = cryptoDropdown.querySelector('.p-2');
    options.forEach((item) => {
        const isSelected = state.cryptoType === item.cryptoType && state.cryptoNetwork === item.network;
        const button = document.createElement('button');
        button.className = 'crypto-option w-full flex items-center gap-3 p-3 rounded-xl hover:bg-emerald-50 transition-colors duration-150';
        button.dataset.crypto = item.cryptoType;
        button.dataset.network = item.network;
        button.dataset.chain = item.chain;
        button.dataset.rate = '1.0';
        
        const displayLabel = getOptionDisplayLabel(item);
        const displayTag = getOptionDisplayTag(item);
        const hasTranslation = translate(item.label) !== item.label;
        const hasTagTranslation = translate(item.tag) !== item.tag;
        
        button.innerHTML = `
            <div class="w-9 h-9 rounded-full bg-gradient-to-br ${item.colorClass} flex items-center justify-center overflow-hidden">
                ${item.logoUrl 
                    ? `<img src="${item.logoUrl}" alt="${item.cryptoType}" class="w-full h-full object-cover" data-fallback-icon='${item.icon}'>` 
                    : item.icon}
            </div>
            <div class="flex-1 text-left">
                <div class="font-semibold text-slate-800 text-sm">${item.cryptoType}</div>
                <div class="text-xs text-slate-500"${hasTranslation ? ` data-i18n="${item.label}"` : ''}>${displayLabel}</div>
            </div>
            <div class="flex flex-col items-end">
                ${displayTag ? `<div class="text-xs font-mono text-slate-600"${hasTagTranslation ? ` data-i18n="${item.tag}"` : ''}>${displayTag}</div>` : ''}
                <div class="w-2 h-2 rounded-full bg-emerald-500 crypto-check ${isSelected ? 'opacity-100' : 'opacity-0'}"></div>
            </div>
        `;
        container.appendChild(button);
        
        // Add error handler for logo image
        const img = button.querySelector('img[data-fallback-icon]');
        if (img) {
            img.addEventListener('error', () => {
                img.parentElement.innerHTML = img.dataset.fallbackIcon;
            });
        }
    });

    bindCryptoOptionEvents();
}

// Bind click events to crypto options
function bindCryptoOptionEvents() {
    document.querySelectorAll('.crypto-option').forEach(option => {
        option.addEventListener('click', () => {
            const crypto = option.dataset.crypto;
            const network = option.dataset.network;
            const chain = option.dataset.chain;
            const rate = parseFloat(option.dataset.rate);

            state.cryptoType = crypto;
            state.cryptoNetwork = network;
            state.cryptoChain = chain;

            document.getElementById('crypto-name').textContent = crypto;
            
            // Update the crypto icon in the selector
            const cryptoIcon = document.getElementById('crypto-icon');
            const selectedOption = state.combinedCryptoOptions.find(o => 
                o.cryptoType === crypto && o.network === network
            );
            if (selectedOption) {
                cryptoIcon.className = `w-7 h-7 rounded-full bg-gradient-to-br ${selectedOption.colorClass} flex items-center justify-center overflow-hidden`;
                if (selectedOption.logoUrl) {
                    cryptoIcon.innerHTML = `<img src="${selectedOption.logoUrl}" alt="${crypto}" class="w-full h-full object-cover" data-fallback-icon='${selectedOption.icon}'>`;
                    const img = cryptoIcon.querySelector('img');
                    img.addEventListener('error', () => {
                        img.outerHTML = img.dataset.fallbackIcon;
                    });
                } else {
                    cryptoIcon.innerHTML = selectedOption.icon;
                }
            }
            
            // Update check marks
            document.querySelectorAll('.crypto-check').forEach(check => {
                check.classList.add('opacity-0');
                check.classList.remove('opacity-100');
            });
            const checkEl = option.querySelector('.crypto-check');
            if (checkEl) {
                checkEl.classList.remove('opacity-0');
                checkEl.classList.add('opacity-100');
            }

            closeAllDropdowns();
            calculateCrypto();
        });
    });
}

// Load tokens from API
async function loadTokens() {
    const result = await fetchTokens(1, 50);
    
    if (result && result.status === 'success') {
        state.tokens = result.data;
        state.tokenTotalPages = result.pagination.pages;
        state.hasMoreTokens = result.pagination.hasNext;
        
        // Build token metadata from API data
        buildTokenMetaMap(state.tokens);
        
        // Build combined options with existing payment methods
        state.combinedCryptoOptions = buildCombinedCryptoOptions(state.tokens, state.paymentMethods);
        
        renderCryptoOptions(state.combinedCryptoOptions);
        
        // Set USDT as default if available, otherwise first option
        const usdtOption = state.combinedCryptoOptions.find(o => o.cryptoType === 'USDT');
        const defaultOption = usdtOption || state.combinedCryptoOptions[0];
        if (defaultOption) {
            state.cryptoType = defaultOption.cryptoType;
            state.cryptoNetwork = defaultOption.network;
            state.cryptoChain = defaultOption.chain;
            document.getElementById('crypto-name').textContent = state.cryptoType;
            
            const cryptoIcon = document.getElementById('crypto-icon');
            cryptoIcon.className = `w-7 h-7 rounded-full bg-gradient-to-br ${defaultOption.colorClass} flex items-center justify-center overflow-hidden`;
            if (defaultOption.logoUrl) {
                cryptoIcon.innerHTML = `<img src="${defaultOption.logoUrl}" alt="${state.cryptoType}" class="w-full h-full object-cover" data-fallback-icon='${defaultOption.icon}'>`;
                const img = cryptoIcon.querySelector('img');
                img.addEventListener('error', () => {
                    img.outerHTML = img.dataset.fallbackIcon;
                });
            } else {
                cryptoIcon.innerHTML = defaultOption.icon;
            }
        }
    } else {
        console.error('Failed to load tokens from API');
        cryptoDropdown.querySelector('.p-2').innerHTML = '<div class="p-4 text-center text-sm text-red-500">Failed to load tokens. Please refresh.</div>';
    }
}

// Load more tokens (pagination)
async function loadMoreTokens() {
    if (!state.hasMoreTokens) return;
    
    const nextPage = state.tokenPage + 1;
    const result = await fetchTokens(nextPage, 50);
    
    if (result && result.status === 'success') {
        state.tokens = [...state.tokens, ...result.data];
        state.tokenPage = nextPage;
        state.tokenTotalPages = result.pagination.pages;
        state.hasMoreTokens = result.pagination.hasNext;
        
        // Update metadata and rebuild options
        buildTokenMetaMap(result.data);
        state.combinedCryptoOptions = buildCombinedCryptoOptions(state.tokens, state.paymentMethods);
        renderCryptoOptions(state.combinedCryptoOptions);
    }
}

// Load payment methods from API
async function loadPaymentMethods() {
    const result = await fetchPaymentMethods(1, 50);
    
    if (result && result.status === 'success') {
        state.paymentMethods = result.data;
        state.paymentMethodTotalPages = result.pagination.pages;
        state.hasMorePaymentMethods = result.pagination.hasNext;
        
        // If tokens are already loaded, rebuild combined options
        if (state.tokens.length > 0) {
            state.combinedCryptoOptions = buildCombinedCryptoOptions(state.tokens, state.paymentMethods);
            renderCryptoOptions(state.combinedCryptoOptions);
            
            // Set first option as default if not already set
            if (!state.cryptoType || state.combinedCryptoOptions.findIndex(o => 
                o.cryptoType === state.cryptoType && o.network === state.cryptoNetwork) === -1) {
                const defaultOption = state.combinedCryptoOptions[0];
                if (defaultOption) {
                    state.cryptoType = defaultOption.cryptoType;
                    state.cryptoNetwork = defaultOption.network;
                    state.cryptoChain = defaultOption.chain;
                    document.getElementById('crypto-name').textContent = state.cryptoType;
                    
                    const cryptoIcon = document.getElementById('crypto-icon');
                    cryptoIcon.className = `w-7 h-7 rounded-full bg-gradient-to-br ${defaultOption.colorClass} flex items-center justify-center overflow-hidden`;
                    if (defaultOption.logoUrl) {
                        cryptoIcon.innerHTML = `<img src="${defaultOption.logoUrl}" alt="${state.cryptoType}" class="w-full h-full object-cover" data-fallback-icon='${defaultOption.icon}'>`;
                        const img = cryptoIcon.querySelector('img');
                        img.addEventListener('error', () => {
                            img.outerHTML = img.dataset.fallbackIcon;
                        });
                    } else {
                        cryptoIcon.innerHTML = defaultOption.icon;
                    }
                }
            }
        }
    } else {
        console.error('Failed to load payment methods from API');
        if (state.tokens.length === 0) {
            cryptoDropdown.querySelector('.p-2').innerHTML = '<div class="p-4 text-center text-sm text-red-500">Failed to load payment methods. Please refresh.</div>';
        }
    }
}

// DOM Elements
const fiatAmountInput = document.getElementById('fiat-amount');
const cryptoAmountInput = document.getElementById('crypto-amount');
const rateDisplay = document.getElementById('rate-display');
const fiatSelector = document.getElementById('fiat-selector');
const fiatDropdown = document.getElementById('fiat-dropdown');
const fiatArrow = document.getElementById('fiat-arrow');
const cryptoSelector = document.getElementById('crypto-selector');
const cryptoDropdown = document.getElementById('crypto-dropdown');
const cryptoArrow = document.getElementById('crypto-arrow');
const feeToggle = document.getElementById('fee-toggle');
const feePanel = document.getElementById('fee-panel');
const feeArrow = document.getElementById('fee-arrow');
const checkoutBtn = document.getElementById('checkout-btn');
const modal = document.getElementById('checkout-modal');
const modalBackdrop = document.getElementById('modal-backdrop');
const modalContent = document.getElementById('modal-content');
const modalClose = document.getElementById('modal-close');
const modalCancel = document.getElementById('modal-cancel');
const step3ds = document.getElementById('step-3ds');
const stepAddress = document.getElementById('step-address');
const copyBtn = document.getElementById('copy-btn');
const copyIcon = document.getElementById('copy-icon');
const checkIcon = document.getElementById('check-icon');
const swapBtn = document.getElementById('swap-btn');
const toast = document.getElementById('toast');
const tabBuy = document.getElementById('tab-buy');
const tabSell = document.getElementById('tab-sell');

// Generate random TRON address
function generateTronAddress() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789abcdefghijklmnopqrstuvwxyz';
    let address = 'T';
    for (let i = 0; i < 33; i++) {
        address += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return address;
}

// Calculate crypto amount using real-time exchange rate
async function calculateCrypto() {
    // Guard: wait for API data to be loaded
    if (!state.fiatCurrency || !state.cryptoType) {
        cryptoAmountInput.value = '--';
        rateDisplay.textContent = 'Loading...';
        return;
    }

    const fiatAmount = parseFloat(fiatAmountInput.value) || 0;
    
    // Don't trigger exchange rate calculation when amount is 0
    if (fiatAmount <= 0) {
        cryptoAmountInput.value = '--';
        rateDisplay.textContent = 'Enter an amount';
        document.getElementById('routing-fee').textContent = '--';
        document.getElementById('routing-fee-rate').textContent = '--';
        document.getElementById('merchant-net').textContent = '--';
        document.getElementById('merchant-net-rate').textContent = '--';
        document.getElementById('total-fees').textContent = '--';
        return;
    }
    
    // Set loading state
    cryptoAmountInput.value = '...';
    rateDisplay.textContent = '...';
    
    // Determine source and destination currencies based on mode
    let sourceCurrency, destinationCurrency;
    if (state.isBuyMode) {
        sourceCurrency = state.fiatCurrency;
        destinationCurrency = state.cryptoType;
    } else {
        sourceCurrency = state.cryptoType;
        destinationCurrency = state.fiatCurrency;
    }

    // Fetch real-time exchange rate
    const exchangeData = await fetchExchangeRate(
        fiatAmount,
        sourceCurrency,
        destinationCurrency,
        state.orderType
    );

    if (exchangeData && !exchangeData.error) {
        // Parse exchange rate from API response (field names based on actual API)
        const rate = exchangeData.conversionRate || 
                   exchangeData.rate || 
                   exchangeData.exchangeRate || 
                   exchangeData.sourceRate ||
                   (exchangeData.destinationAmount / exchangeData.sourceAmount) ||
                   state.fiatRate || 1;
        
        const convertedAmount = exchangeData.destinationAmount || 
                               exchangeData.amount ||
                               fiatAmount * rate;
        
        state.lastExchangeRate = rate;
        state.fiatRate = rate;

        // Use fixed FEE_RATE (7%) instead of API's totalFee
        const feeAmount = fiatAmount * FEE_RATE;
        const netAmount = fiatAmount - feeAmount;
        const symbol = state.fiatSymbol || state.fiatCurrency || '';
        
        // Calculate fee percentage dynamically
        const feePercent = FEE_RATE * 100;
        const netPercent = (1 - FEE_RATE) * 100;
        
        cryptoAmountInput.value = convertedAmount.toFixed(2);
        document.getElementById('routing-fee').textContent = `${symbol}${feeAmount.toFixed(2)}`;
        document.getElementById('routing-fee-rate').textContent = `${feePercent.toFixed(1)}%`;
        document.getElementById('merchant-net').textContent = `${symbol}${netAmount.toFixed(2)}`;
        document.getElementById('merchant-net-rate').textContent = `${netPercent.toFixed(1)}%`;
        document.getElementById('total-fees').textContent = `≈ ${symbol}${feeAmount.toFixed(2)}`;
        
        rateDisplay.textContent = `1 ${sourceCurrency} = ${rate.toFixed(4)} ${destinationCurrency}`;
        
        if (state.isBuyMode) {
            document.getElementById('modal-send').textContent = `${fiatAmount.toFixed(2)} ${state.fiatCurrency}`;
            document.getElementById('modal-receive').textContent = `${convertedAmount.toFixed(2)} ${state.cryptoType}`;
        } else {
            document.getElementById('modal-send').textContent = `${convertedAmount.toFixed(2)} ${state.cryptoType}`;
            document.getElementById('modal-receive').textContent = `${fiatAmount.toFixed(2)} ${state.fiatCurrency}`;
        }
    } else if (exchangeData && exchangeData.error) {
        // API returned an error (e.g. unsupported currency pair)
        cryptoAmountInput.value = '--';
        rateDisplay.textContent = exchangeData.message || `Rate not available for ${sourceCurrency} → ${destinationCurrency}`;
        document.getElementById('routing-fee').textContent = '--';
        document.getElementById('routing-fee-rate').textContent = '--';
        document.getElementById('merchant-net').textContent = '--';
        document.getElementById('merchant-net-rate').textContent = '--';
        document.getElementById('total-fees').textContent = '--';
    } else {
        const rate = state.fiatRate || 1;
        const feeAmount = fiatAmount * FEE_RATE;
        const netAmount = fiatAmount - feeAmount;
        const cryptoAmount = fiatAmount * rate;
        const symbol = state.fiatSymbol || state.fiatCurrency || '';
        const feePercent = FEE_RATE * 100;
        const netPercent = (1 - FEE_RATE) * 100;
        
        cryptoAmountInput.value = cryptoAmount.toFixed(2);
        document.getElementById('routing-fee').textContent = `${symbol}${feeAmount.toFixed(2)}`;
        document.getElementById('routing-fee-rate').textContent = `${feePercent.toFixed(1)}%`;
        document.getElementById('merchant-net').textContent = `${symbol}${netAmount.toFixed(2)}`;
        document.getElementById('merchant-net-rate').textContent = `${netPercent.toFixed(1)}%`;
        document.getElementById('total-fees').textContent = `≈ ${symbol}${feeAmount.toFixed(2)}`;
        rateDisplay.textContent = `1 ${state.fiatCurrency} = ${rate} ${state.cryptoType}`;
        
        document.getElementById('modal-send').textContent = `${fiatAmount.toFixed(2)} ${state.fiatCurrency}`;
        document.getElementById('modal-receive').textContent = `${cryptoAmount.toFixed(2)} ${state.cryptoType}`;
    }
    
    const networkKey = state.cryptoNetwork || 'Network';
    document.getElementById('modal-network').textContent = translate('payment.cryptoNetworks.' + networkKey) || networkKey;
}

// Debounced exchange rate calculation (triggers 1s after user stops typing, only if amount > 0)
let exchangeRateTimer = null;
function debouncedCalculateCrypto() {
    const amount = parseFloat(fiatAmountInput.value) || 0;
    if (amount <= 0) {
        cryptoAmountInput.value = '--';
        rateDisplay.textContent = 'Enter an amount';
        return;
    }
    if (exchangeRateTimer) clearTimeout(exchangeRateTimer);
    cryptoAmountInput.value = '...';
    rateDisplay.textContent = '...';
    exchangeRateTimer = setTimeout(() => {
        calculateCrypto();
    }, 1000);
}

// Fiat amount input handler
fiatAmountInput.addEventListener('input', debouncedCalculateCrypto);

// Fiat dropdown toggle
fiatSelector.addEventListener('click', (e) => {
    e.stopPropagation();
    const isOpen = !fiatDropdown.classList.contains('hidden');
    closeAllDropdowns();
    if (!isOpen) {
        fiatDropdown.classList.remove('hidden', 'scale-95', 'opacity-0');
        fiatDropdown.classList.add('scale-100', 'opacity-100');
        fiatArrow.classList.add('rotate-180');
    }
});

// Crypto dropdown toggle
cryptoSelector.addEventListener('click', (e) => {
    e.stopPropagation();
    const isOpen = !cryptoDropdown.classList.contains('hidden');
    closeAllDropdowns();
    if (!isOpen) {
        cryptoDropdown.classList.remove('hidden', 'scale-95', 'opacity-0');
        cryptoDropdown.classList.add('scale-100', 'opacity-100');
        cryptoArrow.classList.add('rotate-180');
    }
});

// Close all dropdowns
function closeAllDropdowns() {
    fiatDropdown.classList.add('hidden', 'scale-95', 'opacity-0');
    fiatDropdown.classList.remove('scale-100', 'opacity-100');
    fiatArrow.classList.remove('rotate-180');
    cryptoDropdown.classList.add('hidden', 'scale-95', 'opacity-0');
    cryptoDropdown.classList.remove('scale-100', 'opacity-100');
    cryptoArrow.classList.remove('rotate-180');
}

// Crypto option selection
document.querySelectorAll('.crypto-option').forEach(option => {
    option.addEventListener('click', () => {
        const crypto = option.dataset.crypto;
        const network = option.dataset.network;
        const chain = option.dataset.chain;
        const rate = parseFloat(option.dataset.rate);

        state.cryptoType = crypto;
        state.cryptoNetwork = network;
        state.cryptoChain = chain;
        state.fiatRate = rate;

        document.getElementById('crypto-name').textContent = crypto;
        
        const iconContainer = document.getElementById('crypto-icon');
        if (network === 'TRC20') {
            iconContainer.className = 'w-7 h-7 rounded-full bg-gradient-to-br from-emerald-400 to-green-500 flex items-center justify-center';
            iconContainer.innerHTML = '<span class="text-white text-xs font-bold">₮</span>';
        } else if (chain === 'Stellar') {
            iconContainer.className = 'w-7 h-7 rounded-full bg-gradient-to-br from-blue-400 to-indigo-500 flex items-center justify-center';
            iconContainer.innerHTML = '<span class="text-white text-xs font-bold">$</span>';
        } else {
            iconContainer.className = 'w-7 h-7 rounded-full bg-gradient-to-br from-indigo-400 to-purple-500 flex items-center justify-center';
            iconContainer.innerHTML = '<span class="text-white text-xs font-bold">Ξ</span>';
        }

        document.querySelectorAll('.crypto-check').forEach(check => check.classList.add('opacity-0'));
        option.querySelector('.crypto-check').classList.remove('opacity-0');

        // Update gateway name based on network
        const gatewayKeys = {
            'TRC20': 'payment.gateway.moonpayTransak',
            'Stellar': 'payment.gateway.mercuryoTransak',
            'ERC20': 'payment.gateway.moonpayMercuryo'
        };
        document.getElementById('gateway-name').textContent = translate(gatewayKeys[network] || gatewayKeys['TRC20']);

        closeAllDropdowns();
        calculateCrypto();
    });
});

// Fee panel toggle
feeToggle.addEventListener('click', () => {
    state.feePanelOpen = !state.feePanelOpen;
    if (state.feePanelOpen) {
        feePanel.classList.remove('hidden');
        feeArrow.classList.add('rotate-180');
    } else {
        feePanel.classList.add('hidden');
        feeArrow.classList.remove('rotate-180');
    }
});

// Swap button - toggle between buy/sell modes
swapBtn.addEventListener('click', () => {
    swapBtn.classList.add('animate-spin');
    setTimeout(() => {
        swapBtn.classList.remove('animate-spin');
        // Toggle mode
        if (state.isBuyMode) {
            tabSell.click();
        } else {
            tabBuy.click();
        }
    }, 500);
});

// Tab switching
tabBuy.addEventListener('click', () => {
    tabBuy.classList.add('bg-white', 'text-slate-900', 'shadow-sm', 'font-semibold');
    tabBuy.classList.remove('text-slate-500', 'font-medium');
    tabSell.classList.remove('bg-white', 'text-slate-900', 'shadow-sm', 'font-semibold');
    tabSell.classList.add('text-slate-500', 'font-medium');
    
    state.isBuyMode = true;
    state.orderType = 'onramp';
    
    // Update UI for buy mode
    document.getElementById('payment-direction').textContent = translate('payment.buy');
    document.querySelector('[data-i18n="payment.desc"]').textContent = 'onramp';
    calculateCrypto();
});

tabSell.addEventListener('click', () => {
    tabSell.classList.add('bg-white', 'text-slate-900', 'shadow-sm', 'font-semibold');
    tabSell.classList.remove('text-slate-500', 'font-medium');
    tabBuy.classList.remove('bg-white', 'text-slate-900', 'shadow-sm', 'font-semibold');
    tabBuy.classList.add('text-slate-500', 'font-medium');
    
    state.isBuyMode = false;
    state.orderType = 'offramp';
    
    // Update UI for sell mode
    document.getElementById('payment-direction').textContent = translate('payment.sell');
    document.querySelector('[data-i18n="payment.desc"]').textContent = 'offramp';
    calculateCrypto();
});

// Close dropdowns on outside click
document.addEventListener('click', (e) => {
    if (!fiatSelector.contains(e.target) && !fiatDropdown.contains(e.target) &&
        !cryptoSelector.contains(e.target) && !cryptoDropdown.contains(e.target)) {
        closeAllDropdowns();
    }
});

// Modal functionality
let countdownInterval;

function openModal() {
    modal.classList.remove('hidden');
    document.body.style.overflow = 'hidden';
    
    // Reset to 3DS step
    step3ds.classList.remove('hidden');
    stepAddress.classList.add('hidden');
    
    // Show backdrop
    setTimeout(() => {
        modalBackdrop.classList.remove('opacity-0');
        modalContent.classList.remove('scale-95', 'opacity-0');
        modalContent.classList.add('scale-100', 'opacity-100');
    }, 10);

    // Generate new address
    document.getElementById('tron-address').textContent = generateTronAddress();

    // Simulate 3DS verification
    setTimeout(() => {
        step3ds.classList.add('hidden');
        stepAddress.classList.remove('hidden');
        startCountdown();
    }, 2500);
}

function closeModal() {
    modalBackdrop.classList.add('opacity-0');
    modalContent.classList.add('scale-95', 'opacity-0');
    modalContent.classList.remove('scale-100', 'opacity-100');
    
    setTimeout(() => {
        modal.classList.add('hidden');
        document.body.style.overflow = '';
        if (countdownInterval) clearInterval(countdownInterval);
    }, 300);
}

function startCountdown() {
    let seconds = 15 * 60;
    const countdownEl = document.getElementById('countdown');
    
    if (countdownInterval) clearInterval(countdownInterval);
    
    countdownInterval = setInterval(() => {
        seconds--;
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        countdownEl.textContent = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
        
        if (seconds <= 0) {
            clearInterval(countdownInterval);
            document.getElementById('tron-address').textContent = generateTronAddress();
            seconds = 15 * 60;
        }
    }, 1000);
}

checkoutBtn.addEventListener('click', openModal);
modalClose.addEventListener('click', closeModal);
modalCancel.addEventListener('click', closeModal);
modalBackdrop.addEventListener('click', closeModal);

// Copy address
copyBtn.addEventListener('click', async () => {
    const address = document.getElementById('tron-address').textContent;
    try {
        await navigator.clipboard.writeText(address);
    } catch {
        const textArea = document.createElement('textarea');
        textArea.value = address;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
    }

    copyIcon.classList.add('hidden');
    checkIcon.classList.remove('hidden');
    copyBtn.classList.add('bg-emerald-600');
    copyBtn.classList.remove('bg-indigo-600');

    showToast();

    setTimeout(() => {
        copyIcon.classList.remove('hidden');
        checkIcon.classList.add('hidden');
        copyBtn.classList.remove('bg-emerald-600');
        copyBtn.classList.add('bg-indigo-600');
    }, 2000);
});

function showToast() {
    toast.classList.remove('translate-y-24', 'opacity-0');
    toast.classList.add('translate-y-0', 'opacity-100');
    
    setTimeout(() => {
        toast.classList.add('translate-y-24', 'opacity-0');
        toast.classList.remove('translate-y-0', 'opacity-100');
    }, 2500);
}

// Escape key to close modal
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !modal.classList.contains('hidden')) {
        closeModal();
    }
});

// Add scroll-to-load-more for fiat dropdown
fiatDropdown.addEventListener('scroll', () => {
    const { scrollTop, scrollHeight, clientHeight } = fiatDropdown;
    if (scrollTop + clientHeight >= scrollHeight - 50 && state.hasMoreCurrencies) {
        loadMoreCurrencies();
    }
});

// Add scroll-to-load-more for crypto dropdown
cryptoDropdown.addEventListener('scroll', () => {
    const { scrollTop, scrollHeight, clientHeight } = cryptoDropdown;
    if (scrollTop + clientHeight >= scrollHeight - 50 && state.hasMoreTokens) {
        loadMoreTokens();
    }
});

// Initialize calculations and load data
// Load currencies first, then tokens/payment methods in parallel
loadCurrencies().then(() => {
    // After currencies are loaded, start loading tokens and payment methods
    Promise.all([
        loadTokens().catch(err => {
            console.error('Failed to load tokens:', err);
            return null;
        }),
        loadPaymentMethods().catch(err => {
            console.error('Failed to load payment methods:', err);
            return null;
        })
    ]).then(() => {
        state.isLoading = false;
        
        // Trigger initial calculation now that all data is loaded
        if (state.fiatCurrency && state.cryptoType) {
            calculateCrypto();
        }
        
        // If both APIs failed, show error
        if (state.combinedCryptoOptions.length === 0) {
            cryptoDropdown.querySelector('.p-2').innerHTML = '<div class="p-4 text-center text-sm text-red-500">Failed to load payment methods. Please try again later.</div>';
        }
    });
}).catch(err => {
    console.error('Failed to load currencies:', err);
    state.isLoading = false;
});