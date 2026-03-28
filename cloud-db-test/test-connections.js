const { Client } = require('pg');
const { MongoClient } = require('mongodb');
const neo4j = require('neo4j-driver');
const redis = require('redis');
const fs = require('fs');
const path = require('path');
const dotenv = require('dotenv');
const dns = require("node:dns/promises");
dns.setServers(["1.1.1.1"]);

// Load .env.cloud file from parent directory
const envPath = path.join(__dirname, '..', '.env.cloud');
if (fs.existsSync(envPath)) {
  const envConfig = dotenv.parse(fs.readFileSync(envPath));
  for (const k in envConfig) {
    process.env[k] = envConfig[k];
  }
} else {
  console.error('.env.cloud not found in parent directory!');
  process.exit(1);
}

const tests = [];

// 1. Test Neon PostgreSQL (Auth DB)
async function testPostgresAuth() {
  const client = new Client({
    host: process.env.AUTH_DB_HOST,
    database: process.env.AUTH_DB_NAME,
    user: process.env.AUTH_DB_USER,
    password: process.env.AUTH_DB_PASSWORD,
    ssl: { rejectUnauthorized: false }
  });
  try {
    await client.connect();
    const res = await client.query('SELECT version()');
    console.log('✅ Auth DB (Neon PostgreSQL) connected!');
    // console.log(`   Version: ${res.rows[0].version.substring(0, 30)}...`);
  } catch (err) {
    console.error('❌ Auth DB connection failed:', err.message);
  } finally {
    await client.end();
  }
}
tests.push(testPostgresAuth);

// 2. Test Neon PostgreSQL (Media DB)
async function testPostgresMedia() {
  const client = new Client({
    host: process.env.MEDIA_DB_HOST,
    database: process.env.MEDIA_DB_NAME,
    user: process.env.MEDIA_DB_USER,
    password: process.env.MEDIA_DB_PASSWORD,
    ssl: { rejectUnauthorized: false }
  });
  try {
    await client.connect();
    await client.query('SELECT 1');
    console.log('✅ Media DB (Neon PostgreSQL) connected!');
  } catch (err) {
    console.error('❌ Media DB connection failed:', err.message);
  } finally {
    await client.end();
  }
}
tests.push(testPostgresMedia);

// 3. Test Neon PostgreSQL (Recommend DB)
async function testPostgresRecommend() {
  const client = new Client({
    host: process.env.RECOMMEND_DB_HOST,
    database: process.env.RECOMMEND_DB_NAME,
    user: process.env.RECOMMEND_DB_USER,
    password: process.env.RECOMMEND_DB_PASSWORD,
    ssl: { rejectUnauthorized: false }
  });
  try {
    await client.connect();
    await client.query('SELECT 1');
    console.log('✅ Recommend DB (Neon PostgreSQL) connected!');
  } catch (err) {
    console.error('❌ Recommend DB connection failed:', err.message);
  } finally {
    await client.end();
  }
}
tests.push(testPostgresRecommend);

// 4. Test MongoDB Atlas (Post DB)
async function testMongoPost() {
  const client = new MongoClient(process.env.POST_MONGODB_URI);
  try {
    await client.connect();
    await client.db('admin').command({ ping: 1 });
    console.log('✅ Post DB (MongoDB Atlas) connected!');
  } catch (err) {
    console.error('❌ Post DB connection failed:', err.message);
  } finally {
    await client.close();
  }
}
tests.push(testMongoPost);

// 5. Test MongoDB Atlas (Chat DB)
async function testMongoChat() {
  const client = new MongoClient(process.env.CHAT_MONGODB_URI);
  try {
    await client.connect();
    await client.db('admin').command({ ping: 1 });
    console.log('✅ Chat DB (MongoDB Atlas) connected!');
  } catch (err) {
    console.error('❌ Chat DB connection failed:', err.message);
  } finally {
    await client.close();
  }
}
tests.push(testMongoChat);

// 6. Test Neo4j Aura
async function testNeo4j() {
  const driver = neo4j.driver(
    process.env.NEO4J_URI,
    neo4j.auth.basic(process.env.NEO4J_USERNAME, process.env.NEO4J_PASSWORD)
  );
  try {
    const serverInfo = await driver.getServerInfo();
    console.log('✅ Neo4j Aura connected!');
  } catch (err) {
    console.error('❌ Neo4j Aura connection failed:', err.message);
  } finally {
    await driver.close();
  }
}
tests.push(testNeo4j);

// 7. Test Upstash Redis (Global)
async function testRedisGlobal() {
  // Use redis[s]:// based URL
  const client = redis.createClient({
    url: process.env.GLOBAL_REDIS_URL,
    socket: {
      tls: true,
      rejectUnauthorized: false
    }
  });

  client.on('error', (err) => console.error('❌ Global Redis error:', err.message));

  try {
    await client.connect();
    await client.ping();
    console.log('✅ Global Redis (Upstash) connected!');
  } catch (err) {
    console.error('❌ Global Redis connection failed:', err.message);
  } finally {
    await client.quit();
  }
}
tests.push(testRedisGlobal);

// 8. Test Upstash Redis (Recommend)
async function testRedisRecommend() {
  const client = redis.createClient({
    url: process.env.RECOMMEND_REDIS_URL,
    socket: {
      tls: true,
      rejectUnauthorized: false
    }
  });

  client.on('error', (err) => console.error('❌ Recommend Redis error:', err.message));

  try {
    await client.connect();
    await client.ping();
    console.log('✅ Recommend Redis (Upstash) connected!');
  } catch (err) {
    console.error('❌ Recommend Redis connection failed:', err.message);
  } finally {
    await client.quit();
  }
}
tests.push(testRedisRecommend);

// Run all tests
async function runAll() {
  console.log('==================================================');
  console.log('   Testing Cloud Database Connections');
  console.log('==================================================\n');

  await Promise.allSettled(tests.map(t => t()));

  console.log('\n==================================================');
  console.log('   Test Completed');
  console.log('==================================================');
}

runAll();
