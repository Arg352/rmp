/**
 * seed.js — создаёт тестовых пользователей в базе данных через API.
 * Запустить: node seed.js (пока бэкенд запущен на :3000)
 */

const http = require('http');

const BASE_URL = 'http://localhost:3000';

function post(path, body) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const options = {
      hostname: 'localhost',
      port: 3000,
      path,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data),
      },
    };
    const req = http.request(options, (res) => {
      let raw = '';
      res.on('data', (chunk) => { raw += chunk; });
      res.on('end', () => {
        try { resolve({ status: res.statusCode, body: JSON.parse(raw) }); }
        catch { resolve({ status: res.statusCode, body: raw }); }
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

const users = [
  { username: 'test',  email: 'test@asylum.app',  password: 'test123', confirmPassword: 'test123' },
  { username: 'alice', email: 'alice@asylum.app',  password: 'alice123', confirmPassword: 'alice123' },
  { username: 'bob',   email: 'bob@asylum.app',    password: 'bob123',  confirmPassword: 'bob123'  },
];

async function seed() {
  console.log('=== Asylum Seed ===\n');
  for (const u of users) {
    try {
      const res = await post('/auth/register', u);
      if (res.status === 201 || res.status === 200) {
        console.log(`✅ ${u.username} / ${u.password}  → зарегистрирован`);
      } else if (res.status === 409) {
        console.log(`⚠️  ${u.username}  → уже существует (пропуск)`);
      } else {
        console.log(`❌ ${u.username}  → ошибка ${res.status}:`, res.body);
      }
    } catch (e) {
      console.error(`💥 ${u.username} → нет соединения:`, e.message);
    }
  }
  console.log('\n=== Данные для входа ===');
  console.log('  Логин: test   Пароль: test123');
  console.log('  Логин: alice  Пароль: alice123');
  console.log('  Логин: bob    Пароль: bob123');
}

seed();
