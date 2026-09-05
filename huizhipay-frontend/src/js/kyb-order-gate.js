(function (global) {
  async function requireApprovedKyb({ fetchStatus, showBlocked }) {
    const onboarding = await fetchStatus();
    const status = onboarding?.status || 'DRAFT';
    if (status === 'APPROVED') return true;
    showBlocked(status);
    return false;
  }

  global.KybOrderGate = { requireApprovedKyb };
})(typeof window === 'undefined' ? globalThis : window);
