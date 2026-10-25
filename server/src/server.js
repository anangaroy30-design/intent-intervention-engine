
require('dotenv').config();

const db = require('./db');

const PORT = Number(process.env.PORT) || 3000;

async function startServer() {
  await db.init();

  // Load the API only after the database is initialized.
  const app = require('./app');

  const server = app.listen(PORT, () => {
    console.log(`INTENT API running at http://localhost:${PORT}`);
  });

  function shutdown() {
    server.close(() => {
      db.close();
      process.exit(0);
    });
  }

  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);
}

startServer().catch((error) => {
  console.error('Could not start INTENT API:', error);
  process.exit(1);
});

