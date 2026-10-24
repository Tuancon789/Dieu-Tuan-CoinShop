-- Cloudflare D1 migration, apply before deployment.
CREATE TABLE IF NOT EXISTS users(
  id TEXT PRIMARY KEY,
  username TEXT NOT NULL UNIQUE COLLATE NOCASE,
  salt TEXT NOT NULL,passhash TEXT NOT NULL,
  role TEXT NOT NULL CHECK(role IN ('admin','member')),
  credits INTEGER NOT NULL DEFAULT 0 CHECK(credits>=0),
  vip_until INTEGER NOT NULL DEFAULT 0,
  vip_lifetime INTEGER NOT NULL DEFAULT 0,
  locked INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS sessions(
  token_hash TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  expires_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_session_user ON sessions(user_id);
CREATE TABLE IF NOT EXISTS tryons(
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id),
  provider_id TEXT NOT NULL,
  status TEXT NOT NULL,
  credits_spent INTEGER NOT NULL DEFAULT 1,
  result_url TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_tryons_user ON tryons(user_id,created_at DESC);
