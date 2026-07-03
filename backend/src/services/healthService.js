const productService = require('./productService');
const {
  HEALTH_GOAL_MAP,
  resolveHealthGoal,
  getHealthGoalDefinition,
  getHealthGoalKeywordGroups,
  listHealthGoals,
} = require('../config/healthGoals');

const DEFAULT_LIMIT = 12;

/**
 * HealthService – tư vấn sản phẩm theo mục tiêu sức khỏe.
 * Không truy vấn MongoDB trực tiếp; mọi dữ liệu sản phẩm đi qua ProductService.
 */
async function getProductsForGoal(rawGoal, options = {}) {
  const goal = getHealthGoalDefinition(rawGoal);
  const limit = Math.max(1, Math.min(Number(options.limit) || DEFAULT_LIMIT, 50));

  const products = await productService.searchByKeywordGroups(
    getHealthGoalKeywordGroups(rawGoal),
    { limit }
  );

  return {
    goal: goal.key,
    goalLabel: goal.label,
    description: goal.description,
    tips: goal.tips,
    searchKeywords: goal.searchKeywords,
    products,
  };
}

async function recommendProductsForGoal(rawGoal, limit = DEFAULT_LIMIT) {
  return getProductsForGoal(rawGoal, { limit });
}

function getAvailableGoals() {
  return listHealthGoals();
}

function getGoalMapping(rawGoal) {
  const goalKey = resolveHealthGoal(rawGoal);
  return HEALTH_GOAL_MAP[goalKey];
}

module.exports = {
  getProductsForGoal,
  recommendProductsForGoal,
  getAvailableGoals,
  getGoalMapping,
  resolveHealthGoal,
  HEALTH_GOAL_MAP,
  HEALTH_GOALS: HEALTH_GOAL_MAP,
};
