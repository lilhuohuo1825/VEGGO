/**
 * Health goal mapping – single source of truth.
 * Thêm mục tiêu mới: bổ sung entry vào HEALTH_GOAL_MAP và HEALTH_GOAL_ALIASES.
 */
const HEALTH_GOAL_MAP = {
  weight_loss: {
    key: 'weight_loss',
    label: 'Giảm cân',
    description: 'Thực phẩm ít calo, giàu chất xơ và protein nạc.',
    searchKeywords: ['rau xanh', 'cải', 'dưa leo', 'ức gà', 'cá', 'đậu', 'yến mạch', 'bí đao', 'salad'],
    tips: 'Ưu tiên rau xanh, protein nạc và thực phẩm ít calo.',
  },
  muscle_gain: {
    key: 'muscle_gain',
    label: 'Tăng cơ',
    description: 'Thực phẩm giàu protein và carb phức hợp.',
    searchKeywords: ['thịt bò', 'ức gà', 'trứng', 'cá hồi', 'đậu phụ', 'sữa', 'protein', 'hạt', 'whey'],
    tips: 'Bổ sung protein chất lượng cao và carb phức hợp.',
  },
  vitamin_c: {
    key: 'vitamin_c',
    label: 'Bổ sung vitamin C',
    description: 'Trái cây và rau củ giàu vitamin C.',
    keywordGroups: [
      ['chanh', 'bưởi', 'quýt', 'quyt', 'cam xoàn', 'cam sành', 'cam vàng', 'tangerine'],
      ['ớt chuông', 'ot chuong'],
      ['cải xoăn', 'cai xoan', 'cải thìa', 'cai thia', 'rau muống', 'rau muong', 'xà lách', 'xa lach', 'cải bẹ', 'cai be'],
    ],
    searchKeywords: ['cam', 'chanh', 'bưởi', 'quýt', 'ớt chuông', 'cải xoăn', 'cải thìa', 'rau muống', 'xà lách', 'ổi', 'kiwi', 'dâu'],
    tips: 'Chọn trái cây họ cam quýt, ớt chuông và rau lá xanh đậm.',
  },
  vitamin_boost: {
    key: 'vitamin_boost',
    label: 'Bổ sung vitamin',
    description: 'Thực phẩm đa dạng vitamin và khoáng chất.',
    searchKeywords: ['cam', 'ổi', 'bí đỏ', 'cà rốt', 'broccoli', 'rau muống', 'trái cây', 'cải xoăn', 'cải thìa'],
    tips: 'Kết hợp trái cây, rau củ nhiều màu để bổ sung vitamin A, C, K.',
  },
  digestion: {
    key: 'digestion',
    label: 'Hỗ trợ tiêu hóa',
    description: 'Thực phẩm giàu chất xơ và hỗ trợ đường ruột.',
    searchKeywords: ['dưa leo', 'cải thảo', 'dứa', 'sữa chua', 'gừng', 'chuối', 'rau mồng tơi', 'yến mạch'],
    tips: 'Ưu tiên thực phẩm giàu chất xơ và probiotic tự nhiên.',
  },
  immunity: {
    key: 'immunity',
    label: 'Tăng cường miễn dịch',
    description: 'Thực phẩm giàu vitamin C, kẽm và chất chống oxy hóa.',
    searchKeywords: ['tỏi', 'gừng', 'cam', 'ổi', 'cải xoăn', 'nghệ', 'chanh', 'dưa hấu', 'sữa chua'],
    tips: 'Bổ sung vitamin C, kẽm và chất chống oxy hóa từ thực phẩm tự nhiên.',
  },
  energy: {
    key: 'energy',
    label: 'Tăng năng lượng',
    description: 'Thực phẩm cung cấp năng lượng bền vững.',
    searchKeywords: ['chuối', 'yến mạch', 'khoai lang', 'táo', 'hạt', 'mật ong', 'cacao', 'gạo lứt'],
    tips: 'Chọn carb phức hợp và thực phẩm giàu sắt, magie.',
  },
};

const HEALTH_GOAL_ALIASES = {
  weight_loss: ['weight_loss', 'giam can', 'giảm cân', 'giam can', 'lose weight', 'diet'],
  muscle_gain: ['muscle_gain', 'tang co', 'tăng cơ', 'tang co', 'build muscle', 'protein'],
  vitamin_c: ['vitamin_c', 'vitamin c', 'vitaminc', 'vit c', 'bo sung vitamin c', 'bổ sung vitamin c'],
  vitamin_boost: ['vitamin_boost', 'vitamin', 'bo sung vitamin', 'bổ sung vitamin', 'vitamins'],
  digestion: ['digestion', 'tieu hoa', 'tiêu hóa', 'digest', 'fiber'],
  immunity: ['immunity', 'mien dich', 'miễn dịch', 'immune', 'de khang', 'đề kháng'],
  energy: ['energy', 'nang luong', 'năng lượng', 'energy boost', 'met moi', 'mệt mỏi'],
};

function normalizeGoalInput(rawGoal) {
  return String(rawGoal || '')
    .trim()
    .toLowerCase()
    .normalize('NFC');
}

function resolveHealthGoal(rawGoal) {
  const normalized = normalizeGoalInput(rawGoal);

  if (HEALTH_GOAL_MAP[normalized]) {
    return normalized;
  }

  for (const [goalKey, aliases] of Object.entries(HEALTH_GOAL_ALIASES)) {
    if (aliases.some((alias) => normalizeGoalInput(alias) === normalized)) {
      return goalKey;
    }
  }

  return 'vitamin_boost';
}

function getHealthGoalDefinition(rawGoal) {
  const goalKey = resolveHealthGoal(rawGoal);
  return HEALTH_GOAL_MAP[goalKey] || HEALTH_GOAL_MAP.vitamin_boost;
}

function getHealthGoalKeywords(rawGoal) {
  return getHealthGoalDefinition(rawGoal).searchKeywords;
}

function getHealthGoalKeywordGroups(rawGoal) {
  const definition = getHealthGoalDefinition(rawGoal);
  if (Array.isArray(definition.keywordGroups) && definition.keywordGroups.length) {
    return definition.keywordGroups;
  }
  return [definition.searchKeywords || []];
}

function listHealthGoals() {
  return Object.values(HEALTH_GOAL_MAP).map((goal) => ({
    key: goal.key,
    label: goal.label,
    description: goal.description,
    tips: goal.tips,
    searchKeywords: goal.searchKeywords,
  }));
}

module.exports = {
  HEALTH_GOAL_MAP,
  HEALTH_GOAL_ALIASES,
  HEALTH_GOALS: HEALTH_GOAL_MAP,
  resolveHealthGoal,
  getHealthGoalDefinition,
  getHealthGoalKeywords,
  getHealthGoalKeywordGroups,
  listHealthGoals,
};
