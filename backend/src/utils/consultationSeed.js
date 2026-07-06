const fs = require('fs');
const path = require('path');
const Consultation = require('../models/Consultation');

const SEED_CANDIDATES = [
  path.join(__dirname, '..', 'data', 'consultations.seed.json'),
  path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'consultations.json'),
];

function resolveSeedFile() {
  return SEED_CANDIDATES.find((candidate) => fs.existsSync(candidate)) || null;
}

function normalizeSeedRecord(record) {
  if (!record || !record.sku) {
    return null;
  }

  const questions = Array.isArray(record.questions)
    ? record.questions
        .filter((item) => item && typeof item.question === 'string' && item.question.trim())
        .map((item) => ({
          question: item.question.trim(),
          customerId: item.customerId || 'anonymous',
          customerName: item.customerName || 'Khách hàng',
          answer: item.answer || '',
          answeredBy: item.answeredBy || '',
          answeredAt: item.answeredAt ? new Date(item.answeredAt) : null,
          status: item.status === 'answered' ? 'answered' : 'pending',
          createdAt: item.createdAt ? new Date(item.createdAt) : new Date(),
          updatedAt: item.updatedAt ? new Date(item.updatedAt) : new Date(),
        }))
    : [];

  if (questions.length === 0) {
    return null;
  }

  return {
    sku: String(record.sku).trim(),
    productName: record.productName || 'Sản phẩm không xác định',
    questions,
  };
}

async function seedConsultationsIfEmpty() {
  const existingCount = await Consultation.countDocuments();
  if (existingCount > 0) {
    return { seeded: false, count: existingCount };
  }

  const seedFile = resolveSeedFile();
  if (!seedFile) {
    console.warn('[consultationSeed] No seed file found, skipping.');
    return { seeded: false, count: 0 };
  }

  const raw = JSON.parse(fs.readFileSync(seedFile, 'utf8'));
  const records = Array.isArray(raw) ? raw : [];
  const normalized = records.map(normalizeSeedRecord).filter(Boolean);

  if (normalized.length === 0) {
    console.warn('[consultationSeed] Seed file has no valid records.');
    return { seeded: false, count: 0 };
  }

  await Consultation.insertMany(normalized, { ordered: false });
  console.log(`[consultationSeed] Seeded ${normalized.length} consultation document(s) from ${seedFile}`);
  return { seeded: true, count: normalized.length };
}

module.exports = { seedConsultationsIfEmpty };
