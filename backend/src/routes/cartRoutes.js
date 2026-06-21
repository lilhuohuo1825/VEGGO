const express = require('express');
const cartController = require('../controllers/cartController');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

// GET /cart/{customerId}
router.get('/:customerId', asyncHandler(cartController.getCart));

// POST /cart/{customerId}/items
router.post('/:customerId/items', asyncHandler(cartController.addItem));

// PATCH /cart/{customerId}/items/:sku
router.patch('/:customerId/items/:sku', asyncHandler(cartController.updateItemQuantity));

// DELETE /cart/{customerId}/items/:sku
router.delete('/:customerId/items/:sku', asyncHandler(cartController.removeItem));

// DELETE /cart/{customerId} (Clear cart)
router.delete('/:customerId', asyncHandler(cartController.clearCart));

module.exports = router;
