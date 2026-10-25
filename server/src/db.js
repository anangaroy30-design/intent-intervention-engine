
const fs = require('node:fs');
const path = require('node:path');
const initSqlJs = require('sql.js');

const filename = process.env.DATABASE_PATH || './data/intent.sqlite';
const resolved = path.resolve(__dirname, '..', filename);

fs.mkdirSync(path.dirname(resolved), { recursive: true });

let database;
let SQL;
let saveTimer;

function normalize(values) {
  return values.map((value) => {
    if (value === undefined) return null;
    if (typeof value === 'boolean') return value ? 1 : 0;
    return value;
  });
}

function saveDatabase() {
  if (!database) return;
  const bytes = database.export();
  fs.writeFileSync(resolved, Buffer.from(bytes));
}

const db = {
  async init() {
    if (database) return db;

    SQL = await initSqlJs();

    if (fs.existsSync(resolved)) {
      const bytes = fs.readFileSync(resolved);
      database = new SQL.Database(new Uint8Array(bytes));
    } else {
      database = new SQL.Database();
    }

    database.run('PRAGMA foreign_keys = ON');

    database.exec(`
      CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        email TEXT NOT NULL UNIQUE,
        password_hash TEXT NOT NULL,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS settings (
        user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
        daily_goal INTEGER NOT NULL DEFAULT 120,
        monitoring_consent INTEGER NOT NULL DEFAULT 0,
        tone TEXT NOT NULL DEFAULT 'gentle',
        quiet_start TEXT,
        quiet_end TEXT
      );

      CREATE TABLE IF NOT EXISTS sessions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        app TEXT NOT NULL,
        purpose TEXT NOT NULL,
        goal TEXT,
        minutes INTEGER NOT NULL,
        elapsed INTEGER NOT NULL DEFAULT 0,
        category TEXT NOT NULL DEFAULT 'Other',
        status TEXT NOT NULL DEFAULT 'Planned',
        demo INTEGER NOT NULL DEFAULT 0,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS session_events (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        session_id INTEGER NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        event_type TEXT NOT NULL,
        payload TEXT NOT NULL DEFAULT '{}',
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS interventions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        session_id INTEGER NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
        prompt_type TEXT NOT NULL,
        prompt TEXT NOT NULL,
        response TEXT,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        responded_at TEXT
      );

      CREATE TABLE IF NOT EXISTS feedback (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        rating INTEGER,
        message TEXT,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS experiments (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        name TEXT NOT NULL,
        variants TEXT NOT NULL DEFAULT '["Gentle reminder","Goal reflection","Time check"]',
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      );

      CREATE TABLE IF NOT EXISTS assignments (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        experiment_id INTEGER NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        variant TEXT NOT NULL,
        outcome TEXT,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(experiment_id, user_id)
      );
    `);

    saveDatabase();
    return db;
  },

  prepare(sql) {
    if (!database) {
      throw new Error('Database is not initialized. Call db.init() first.');
    }

    return {
      get(...params) {
        const statement = database.prepare(sql);

        try {
          statement.bind(normalize(params));

          if (!statement.step()) return undefined;
          return statement.getAsObject();
        } finally {
          statement.free();
        }
      },

      all(...params) {
        const statement = database.prepare(sql);
        const rows = [];

        try {
          statement.bind(normalize(params));

          while (statement.step()) {
            rows.push(statement.getAsObject());
          }

          return rows;
        } finally {
          statement.free();
        }
      },

      run(...params) {
        database.run(sql, normalize(params));

        const changes = database.getRowsModified();
        const result = database.exec('SELECT last_insert_rowid() AS id');
        const lastInsertRowid =
          result.length && result[0].values.length
            ? result[0].values[0][0]
            : 0;

        saveDatabase();

        return { changes, lastInsertRowid };
      }
    };
  },

  exec(sql) {
    if (!database) {
      throw new Error('Database is not initialized. Call db.init() first.');
    }

    database.exec(sql);
    saveDatabase();
  },

  close() {
    if (database) {
      saveDatabase();
      database.close();
      database = undefined;
    }

    if (saveTimer) clearTimeout(saveTimer);
  }
};

module.exports = db;
