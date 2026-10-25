const { test } = require('node:test');
const assert = require('node:assert/strict');
const app = require('../src/app');
const http = require('node:http');

test('health endpoint returns service status', async (t) => {
  const server = http.createServer(app); await new Promise(r=>server.listen(0,'127.0.0.1',r)); t.after(()=>server.close());
  const port=server.address().port; const response=await fetch(`http://127.0.0.1:${port}/api/health`);
  assert.equal(response.status,200); const body=await response.json(); assert.equal(body.ok,true); assert.equal(body.service,'intent-api');
});
