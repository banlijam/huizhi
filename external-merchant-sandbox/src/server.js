const { createApp } = require('./app');
const { mkdirSync } = require('node:fs');
const { join } = require('node:path');
mkdirSync(join(__dirname, '..', 'data'), { recursive: true });
const app = createApp({ databasePath: process.env.ORDER_DB_PATH || join(__dirname, '..', 'data', 'orders.sqlite') });
const port = Number(process.env.PORT || 14330);
app.server.listen(port, app.listenHost, () => console.log(`External merchant Sandbox listening on http://${app.listenHost}:${port}`));
