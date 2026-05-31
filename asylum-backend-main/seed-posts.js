/**
 * seed-posts.js — создаёт тестовые посты через API.
 * Запустить: node seed-posts.js
 */
const http = require('http');

function request(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const headers = {
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(data),
    };
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const req = http.request(
      { hostname: 'localhost', port: 3000, path, method, headers },
      (res) => {
        let raw = '';
        res.on('data', (c) => { raw += c; });
        res.on('end', () => {
          try { resolve({ status: res.statusCode, body: JSON.parse(raw) }); }
          catch { resolve({ status: res.statusCode, body: raw }); }
        });
      }
    );
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

const accounts = [
  { login: 'test',  password: 'test123' },
  { login: 'alice', password: 'alice123' },
  { login: 'bob',   password: 'bob123' },
];

const posts = [
  {
    account: 'test',
    title: 'Никто не хочет общаться...',
    text: 'Каждый раз когда я захожу в приложение — тишина. Может, здесь кто-нибудь есть?',
    tags: 'грусть одиночество',
    isAnonymous: false,
    visibility: 'PUBLIC',
    images: [],
  },
  {
    account: 'alice',
    title: 'Сегодня был хороший день',
    text: 'Впервые за долгое время почувствовала себя нормально. Выпила кофе, прогулялась, написала в дневник.',
    tags: 'позитив день жизнь',
    isAnonymous: false,
    visibility: 'PUBLIC',
    images: [],
  },
  {
    account: 'bob',
    title: 'Совет дня',
    text: 'Если тебе тяжело — напиши об этом. Даже если никто не ответит, стало чуть легче.',
    tags: 'совет поддержка',
    isAnonymous: false,
    visibility: 'PUBLIC',
    images: [],
  },
  {
    account: 'test',
    title: 'Анонимный крик в пустоту',
    text: 'Иногда просто нужно написать что-то в никуда. Никто не знает кто я, и это освобождает.',
    tags: 'анонимно мысли',
    isAnonymous: true,
    visibility: 'PUBLIC',
    images: [],
  },
];

async function run() {
  console.log('=== Seed Posts ===\n');

  // Логинимся под всеми
  const tokens = {};
  for (const acc of accounts) {
    const res = await request('POST', '/auth/login', { login: acc.login, password: acc.password });
    if (res.status === 200 || res.status === 201) {
      tokens[acc.login] = res.body.accessToken;
      console.log(`✅ Logged in as ${acc.login}`);
    } else {
      console.log(`❌ Login failed for ${acc.login}:`, res.body);
    }
  }

  console.log('');

  // Создаём посты
  for (const p of posts) {
    const token = tokens[p.account];
    if (!token) {
      console.log(`⚠️  Skipping post "${p.title}" — no token for ${p.account}`);
      continue;
    }

    const res = await request('POST', '/posts', {
      title: p.title,
      text: p.text,
      tags: p.tags,
      isAnonymous: p.isAnonymous,
      visibility: p.visibility,
      images: p.images,
    }, token);

    if (res.status === 200 || res.status === 201) {
      console.log(`✅ Post created: "${p.title}"`);
    } else {
      console.log(`❌ Post failed "${p.title}":`, res.body);
    }
  }

  console.log('\n=== Готово! ===');
  console.log('Данные для входа в Android:');
  console.log('  test / test123');
  console.log('  alice / alice123');
  console.log('  bob / bob123');
}

run();
