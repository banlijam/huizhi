const message = document.querySelector('#message'), pay = document.querySelector('#pay');
const statusCard = document.querySelector('#status'), details = document.querySelector('#details');
let token = location.pathname.startsWith('/orders/') ? location.pathname.split('/')[2] : null;
async function load() { if (!token) return; const r = await fetch('/api/orders/' + encodeURIComponent(token)); const o = await r.json();
  statusCard.hidden = false; details.textContent = JSON.stringify({ merchantOrderNo:o.merchantOrderNo, platformOrderNo:o.platformOrderNo,
    amount:o.product?.amount, currency:o.product?.currency, status:o.status, channelStatus:o.channelStatus, updatedAt:o.updatedAt }, null, 2);
  if (o.paymentUrl) { pay.href=o.paymentUrl; pay.hidden=false; } }
document.querySelector('#buy').addEventListener('click', async () => { message.textContent='Creating…'; pay.hidden=true;
  const r=await fetch('/api/orders',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({productId:'test-mug'})});
  const o=await r.json(); if(!r.ok){message.textContent=o.error;return;} token=o.accessToken; history.replaceState({},'',`/orders/${token}`);
  message.textContent=o.paymentUrl?'Payment ready.':'Order saved; channel result needs confirmation.'; await load(); });
document.querySelector('#refresh').addEventListener('click', load); load();
