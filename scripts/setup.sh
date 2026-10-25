#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/../server"
[ -f .env ] || cp .env.example .env
npm install
echo "Backend dependencies installed. Run npm start in server/."
