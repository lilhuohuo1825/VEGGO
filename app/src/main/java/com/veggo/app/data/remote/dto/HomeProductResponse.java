package com.veggo.app.data.remote.dto;

import java.util.List;

/**
 * DTO wrapper cho response của GET /api/products/home:
 * {
 *   "success": true,
 *   "message": "Products retrieved successfully",
 *   "count": 20,
 *   "tab": "popular",
 *   "data": [...]
 * }
 */
public class HomeProductResponse {
    private boolean success;
    private String message;
    private int count;
    private String tab;
    private List<ProductDto> data;

    public boolean isSuccess()             { return success; }
    public String getMessage()             { return message; }
    public int getCount()                  { return count; }
    public String getTab()                 { return tab; }
    public List<ProductDto> getData()      { return data; }
}
