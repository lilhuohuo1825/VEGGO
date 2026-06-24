const express = require('express');
const router = express.Router();
const promotionController = require('../controllers/promotionController');

router.get('/', promotionController.getAllPromotions);
router.get('/:code', promotionController.getPromotionByCode);

module.exports = router;
