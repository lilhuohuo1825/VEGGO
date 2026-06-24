const Promotion = require('../models/Promotion');
const PromotionTarget = require('../models/PromotionTarget');
const PromotionUsage = require('../models/PromotionUsage');

exports.getAllPromotions = async (req, res) => {
  try {
    const promotions = await Promotion.find({ status: 'Active' }).lean();

    // Fetch targets and usages for each promotion
    const enrichedPromotions = await Promise.all(promotions.map(async (promo) => {
      const targets = await PromotionTarget.find({ promotion_id: promo.promotion_id }).lean();
      const usage = await PromotionUsage.findOne({ promotion_id: promo.promotion_id }).lean();

      return {
        ...promo,
        targets,
        usage: usage || { promotion_id: promo.promotion_id, user_id: [], order_id: [] }
      };
    }));

    res.json(enrichedPromotions);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

exports.getPromotionByCode = async (req, res) => {
  try {
    const { code } = req.params;
    const promotion = await Promotion.findOne({ code, status: 'Active' }).lean();

    if (!promotion) {
      return res.status(404).json({ message: 'Promotion not found or inactive' });
    }

    const targets = await PromotionTarget.find({ promotion_id: promotion.promotion_id }).lean();
    const usage = await PromotionUsage.findOne({ promotion_id: promotion.promotion_id }).lean();

    res.json({
      ...promotion,
      targets,
      usage: usage || { promotion_id: promotion.promotion_id, user_id: [], order_id: [] }
    });
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};
