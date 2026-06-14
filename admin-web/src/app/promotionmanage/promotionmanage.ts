import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { ApiService } from '../services/api.service';
import { NotificationService } from '../services/notification.service';
import { forkJoin } from 'rxjs';

/**
 * ============================================================================
 * INTERFACES & TYPES
 * ============================================================================
 */

/**
 * Promotion JSON structure from data file
 */
interface PromotionJSON {
  promotion_id?: string;
  code: string;
  name: string;
  description?: string;
  type?: string;
  scope?: string;
  discount_type?: string;
  discountType?: string;
  discount_value?: number | string;
  discountValue?: number;
  max_discount_value?: number;
  maxDiscount?: number;
  min_order_value?: number;
  minPurchase?: number;
  usage_limit?: number | string;
  usageLimit?: number;
  user_limit?: number | string;
  userLimit?: number;
  usage_count?: number;
  usageCount?: number;
  is_first_order_only?: boolean;
  isFirstOrderOnly?: boolean;
  start_date?: string | { $date: string };
  startDate?: string | { $date: string };
  end_date?: string | { $date: string };
  endDate?: string | { $date: string };
  status?: string;
  created_by?: string;
  created_at?: string | { $date: string };
  updated_at?: string | { $date: string };
  updatedAt?: string | { $date: string };
}

/**
 * Promotion interface for application use
 */
export interface Promotion {
  id?: number;
  code: string;
  name: string;
  description?: string;
  type?: 'User' | 'Admin';
  scope?: 'Order' | 'Shipping' | 'Category' | 'Brand' | 'Product';
  discountType: 'percentage' | 'fixed' | 'buy1get1';
  discountValue: number;
  minPurchase?: number;
  maxDiscount?: number;
  startDate: string;
  startTime?: string;
  endDate: string;
  endTime?: string;
  usageLimit?: number;
  userLimit?: number;
  usageCount: number;
  isFirstOrderOnly?: boolean;
  status: 'active' | 'upcoming' | 'expired';
  targetProducts?: string[];
  targetCategories?: string[];
  updatedAt?: string;
  selected?: boolean;
  groups?: string[];
}

/**
 * Filter criteria interface
 */
export interface FilterCriteria {
  status?: 'active' | 'upcoming' | 'expired';
  type?: 'User' | 'Admin';
  discountType?: 'percentage' | 'fixed' | 'buy1get1';
  scope?: 'All' | 'Product' | 'Category';
  minDiscount?: number;
  maxDiscount?: number;
  group?: string;
}

/**
 * ============================================================================
 * COMPONENT
 * ============================================================================
 */

@Component({
  selector: 'app-promotionmanage',
  templateUrl: './promotionmanage.html',
  styleUrls: ['./promotionmanage.css'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class PromotionManage implements OnInit {
  private http = inject(HttpClient);
  private apiService = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  private notificationService = inject(NotificationService);

  // Data
  promotions: Promotion[] = [];
  filteredPromotions: Promotion[] = [];
  allPromotions: Promotion[] = [];

  // UI State
  isLoading: boolean = true;
  showSortDropdown: boolean = false;
  showGroupModal: boolean = false;
  showAddEditModal: boolean = false;
  showDetailModal: boolean = false;
  showConfirmModal: boolean = false;
  confirmMessage: string = '';
  confirmCallback: (() => void) | null = null;
  editMode: boolean = false;
  currentPromotion: Promotion | null = null;
  selectedPromotion: Promotion | null = null;

  // Popup state
  showPopup: boolean = false;
  popupMessage: string = '';
  popupType: 'success' | 'error' | 'info' = 'success';

  // Date validation state
  dateRangeError: boolean = false;
  detailDateRangeError: boolean = false;

  // Search & Sort
  searchQuery: string = '';
  currentSortBy: 'updated' | 'usage' = 'updated';
  currentSortOrder: 'asc' | 'desc' = 'desc';
  selectedSort: string = '';
  selectedStatFilter: 'total' | 'expired' | 'active' | 'upcoming' | null = null;

  // Stats
  totalCount: number = 0;
  activeCount: number = 0;
  upcomingCount: number = 0;
  expiredCount: number = 0;

  // Available filters
  availableGroups: string[] = [];
  
  // Flag to prevent infinite loop when auto-updating statuses
  private isUpdatingStatuses: boolean = false;
  
  // Target selection data (for add/edit modal)
  targetType: 'Category' | 'Subcategory' | 'Brand' | 'Product' = 'Category';
  selectedTargets: string[] = [];
  availableCategories: string[] = [];
  availableSubcategories: string[] = [];
  availableBrands: string[] = [];
  availableProducts: any[] = [];
  isLoadingTargets: boolean = false;
  targetSearchTerm: string = '';

  // Target selection data (for detail modal)
  detailTargetType: 'Category' | 'Subcategory' | 'Brand' | 'Product' = 'Category';
  detailSelectedTargets: string[] = [];
  detailTargetSearchTerm: string = '';

  /**
   * ============================================================================
   * LIFECYCLE HOOKS
   * ============================================================================
   */

  ngOnInit(): void {
    this.loadPromotions();
    this.loadTargetOptions();
    
    // Global click listener to close dropdowns
    document.addEventListener('click', () => {
      this.showSortDropdown = false;
      this.cdr.detectChanges();
    });
  }

  /**
   * ============================================================================
   * DATA LOADING
   * ============================================================================
   */

  /**
   * Load target options (categories, subcategories, brands, products)
   */
  loadTargetOptions(): void {
    this.isLoadingTargets = true;
    
    // Load categories
    this.http.get<any>(`${environment.apiUrl}/products/metadata/categories`).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.availableCategories = response.data || [];
        }
      },
      error: (error: any) => {
        console.error('Error loading categories:', error);
      }
    });
    
    // Load subcategories
    this.http.get<any>(`${environment.apiUrl}/products/metadata/subcategories`).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.availableSubcategories = response.data || [];
        }
      },
      error: (error: any) => {
        console.error('Error loading subcategories:', error);
      }
    });
    
    // Load brands
    this.http.get<any>(`${environment.apiUrl}/products/metadata/brands`).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.availableBrands = response.data || [];
        }
      },
      error: (error: any) => {
        console.error('Error loading brands:', error);
      }
    });
    
    // Load products
    this.http.get<any>(`${environment.apiUrl}/products/metadata/products`).subscribe({
      next: (response: any) => {
        if (response.success) {
          this.availableProducts = response.data || [];
        }
        this.isLoadingTargets = false;
      },
      error: (error: any) => {
        console.error('Error loading products:', error);
        this.isLoadingTargets = false;
      }
    });
  }

  /**
   * Get available target options based on target type
   */
  getAvailableTargetOptions(): string[] | any[] {
    if (!this.currentPromotion) return [];
    
    // If scope is Brand or Product, use scope directly
    if (this.currentPromotion.scope === 'Brand') {
      return this.availableBrands;
    } else if (this.currentPromotion.scope === 'Product') {
      return this.availableProducts;
    } else if (this.currentPromotion.scope === 'Category') {
      // Use targetType to determine Category or Subcategory
      return this.targetType === 'Subcategory' ? this.availableSubcategories : this.availableCategories;
    }
    
    return [];
  }

  /**
   * Get target value from option (for Product it's SKU, others it's the string itself)
   */
  getTargetValue(option: string | any): string {
    if (this.currentPromotion?.scope === 'Product' && typeof option === 'object') {
      return option.sku || option;
    }
    return option;
  }

  /**
   * Get target label for display
   */
  getTargetLabel(option: string | any): string {
    if (this.currentPromotion?.scope === 'Product' && typeof option === 'object') {
      return `${option.name} (${option.sku})`;
    }
    return option;
  }

  /**
   * Handle target search change
   */
  onTargetSearchChange(): void {
    // Filtering is done in getFilteredTargetOptions()
    this.cdr.detectChanges();
  }

  /**
   * Get search placeholder based on scope
   */
  getSearchPlaceholder(): string {
    if (!this.currentPromotion) return 'Tìm kiếm...';
    
    const scope = this.currentPromotion.scope;
    if (scope === 'Product') {
      return 'Tìm kiếm sản phẩm (tên hoặc SKU)...';
    } else if (scope === 'Brand') {
      return 'Tìm kiếm thương hiệu...';
    } else if (scope === 'Category') {
      return this.targetType === 'Subcategory' 
        ? 'Tìm kiếm danh mục phụ...' 
        : 'Tìm kiếm danh mục chính...';
    }
    return 'Tìm kiếm...';
  }

  /**
   * Get filtered target options based on search term
   */
  getFilteredTargetOptions(): string[] | any[] {
    const options = this.getAvailableTargetOptions();
    
    if (!this.targetSearchTerm || this.targetSearchTerm.trim() === '') {
      return options;
    }
    
    const searchTerm = this.targetSearchTerm.toLowerCase().trim();
    
    if (this.currentPromotion?.scope === 'Product') {
      return (options as any[]).filter(option => {
        const name = (option.name || '').toLowerCase();
        const sku = (option.sku || '').toLowerCase();
        return name.includes(searchTerm) || sku.includes(searchTerm);
      });
    } else {
      // For Category, Subcategory, Brand (string arrays)
      return (options as string[]).filter(option => 
        option.toLowerCase().includes(searchTerm)
      );
    }
  }

  /**
   * Get display name for a target value (for selected tags)
   */
  getTargetDisplayName(targetValue: string): string {
    if (this.currentPromotion?.scope === 'Product') {
      // Find the product by SKU
      const product = this.availableProducts.find(p => p.sku === targetValue);
      return product ? `${product.name} (${product.sku})` : targetValue;
    }
    // For Category, Subcategory, Brand - just return the value
    return targetValue;
  }

  /**
   * Remove a target from selected list
   */
  removeTarget(targetValue: string): void {
    const index = this.selectedTargets.indexOf(targetValue);
    if (index > -1) {
      this.selectedTargets.splice(index, 1);
    }
  }

  /**
   * Clear all selected targets
   */
  clearAllTargets(): void {
    this.selectedTargets = [];
  }

  /**
   * Handle target type change (Category/Subcategory)
   */
  onTargetTypeChange(): void {
    // Reset selected targets when switching between Category and Subcategory
    this.selectedTargets = [];
    this.targetSearchTerm = '';
  }

  /**
   * Toggle target selection
   */
  toggleTarget(target: string): void {
    const index = this.selectedTargets.indexOf(target);
    if (index > -1) {
      this.selectedTargets.splice(index, 1);
    } else {
      this.selectedTargets.push(target);
    }
  }

  /**
   * Check if target is selected
   */
  isTargetSelected(target: string): boolean {
    return this.selectedTargets.includes(target);
  }

  /**
   * Load promotion target for editing
   */
  loadPromotionTarget(promotionId: string): void {
    this.http.get<any>(`${environment.apiUrl}/promotion-targets/${promotionId}`).subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          const targetData = response.data;
          // Set target type (Category, Subcategory, Brand, or Product)
          if (targetData.target_type) {
            // For Category scope, target_type can be 'Category' or 'Subcategory'
            if (this.currentPromotion?.scope === 'Category') {
              this.targetType = targetData.target_type as any;
            } else {
              // For Brand and Product, target_type matches scope
              this.targetType = targetData.target_type as any;
            }
          }
          // Set selected targets
          this.selectedTargets = targetData.target_ref || [];
          console.log('✅ Loaded promotion target:', {
            targetType: this.targetType,
            selectedTargets: this.selectedTargets
          });
        } else {
          console.log('No target found for promotion:', promotionId);
          this.selectedTargets = [];
        }
      },
      error: (error: any) => {
        // Target doesn't exist, that's okay (promotion might not have target)
        console.log('No target found for promotion (error):', promotionId, error);
        this.selectedTargets = [];
      }
    });
  }

  /**
   * Handle scope change - reset target selection if needed
   */
  onScopeChange(): void {
    if (!this.currentPromotion) return;
    
    const scope = this.currentPromotion.scope;
    
    // Reset target selection when scope changes
    this.selectedTargets = [];
    this.targetSearchTerm = '';
    
    if (scope === 'Order' || scope === 'Shipping') {
      this.targetType = 'Category'; // Default
    } else if (scope === 'Category') {
      this.targetType = 'Category'; // Default to main category
    } else if (scope === 'Brand') {
      this.targetType = 'Brand';
    } else if (scope === 'Product') {
      this.targetType = 'Product';
    }
  }

  /**
   * Handle scope change for detail modal - reset target selection if needed
   */
  onDetailScopeChange(): void {
    if (!this.selectedPromotion) return;
    
    const scope = this.selectedPromotion.scope;
    
    // Reset target selection when scope changes
    this.detailSelectedTargets = [];
    this.detailTargetSearchTerm = '';
    
    if (scope === 'Order' || scope === 'Shipping') {
      this.detailTargetType = 'Category'; // Default
    } else if (scope === 'Category') {
      this.detailTargetType = 'Category'; // Default to main category
    } else if (scope === 'Brand') {
      this.detailTargetType = 'Brand';
    } else if (scope === 'Product') {
      this.detailTargetType = 'Product';
    }
  }

  /**
   * Load promotion target for detail modal
   */
  loadDetailPromotionTarget(promotionId: string): void {
    this.http.get<any>(`${environment.apiUrl}/promotion-targets/${promotionId}`).subscribe({
      next: (response: any) => {
        if (response.success && response.data) {
          const targetData = response.data;
          // Set target type (Category, Subcategory, Brand, or Product)
          if (targetData.target_type) {
            // For Category scope, target_type can be 'Category' or 'Subcategory'
            if (this.selectedPromotion?.scope === 'Category') {
              this.detailTargetType = targetData.target_type as any;
            } else {
              // For Brand and Product, target_type matches scope
              this.detailTargetType = targetData.target_type as any;
            }
          }
          // Set selected targets
          this.detailSelectedTargets = targetData.target_ref || [];
          console.log('✅ Loaded detail promotion target:', {
            targetType: this.detailTargetType,
            selectedTargets: this.detailSelectedTargets
          });
        } else {
          console.log('No target found for promotion:', promotionId);
          this.detailSelectedTargets = [];
        }
        this.cdr.detectChanges();
      },
      error: (error: any) => {
        // Target doesn't exist, that's okay (promotion might not have target)
        console.log('No target found for promotion (error):', promotionId, error);
        this.detailSelectedTargets = [];
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Handle target type change for detail modal (Category/Subcategory)
   */
  onDetailTargetTypeChange(): void {
    // Reset selected targets when switching between Category and Subcategory
    this.detailSelectedTargets = [];
    this.detailTargetSearchTerm = '';
  }

  /**
   * Get search placeholder for detail modal
   */
  getDetailSearchPlaceholder(): string {
    if (!this.selectedPromotion) return 'Tìm kiếm...';
    
    const scope = this.selectedPromotion.scope;
    if (scope === 'Product') {
      return 'Tìm kiếm sản phẩm (tên hoặc SKU)...';
    } else if (scope === 'Brand') {
      return 'Tìm kiếm thương hiệu...';
    } else if (scope === 'Category') {
      return this.detailTargetType === 'Subcategory' 
        ? 'Tìm kiếm danh mục phụ...' 
        : 'Tìm kiếm danh mục chính...';
    }
    return 'Tìm kiếm...';
  }

  /**
   * Get available target options for detail modal
   */
  getDetailAvailableTargetOptions(): string[] | any[] {
    if (!this.selectedPromotion) return [];
    
    const scope = this.selectedPromotion.scope;
    
    if (scope === 'Category') {
      return this.detailTargetType === 'Subcategory' 
        ? this.availableSubcategories 
        : this.availableCategories;
    } else if (scope === 'Brand') {
      return this.availableBrands;
    } else if (scope === 'Product') {
      return this.availableProducts;
    }
    
    return [];
  }

  /**
   * Get filtered target options for detail modal
   */
  getDetailFilteredTargetOptions(): string[] | any[] {
    const options = this.getDetailAvailableTargetOptions();
    
    if (!this.detailTargetSearchTerm || this.detailTargetSearchTerm.trim() === '') {
      return options;
    }
    
    const searchTerm = this.detailTargetSearchTerm.toLowerCase().trim();
    
    if (this.selectedPromotion?.scope === 'Product') {
      return (options as any[]).filter(option => {
        const name = (option.name || '').toLowerCase();
        const sku = (option.sku || '').toLowerCase();
        return name.includes(searchTerm) || sku.includes(searchTerm);
      });
    } else {
      // For Category, Subcategory, Brand (string arrays)
      return (options as string[]).filter(option => 
        option.toLowerCase().includes(searchTerm)
      );
    }
  }

  /**
   * Get target value for detail modal
   */
  getDetailTargetValue(option: string | any): string {
    if (this.selectedPromotion?.scope === 'Product') {
      return option.sku || option;
    }
    return option;
  }

  /**
   * Get target label for detail modal
   */
  getDetailTargetLabel(option: string | any): string {
    if (this.selectedPromotion?.scope === 'Product') {
      return `${option.name} (${option.sku})`;
    }
    return option;
  }

  /**
   * Get display name for a target value in detail modal
   */
  getDetailTargetDisplayName(targetValue: string): string {
    if (this.selectedPromotion?.scope === 'Product') {
      // Find the product by SKU
      const product = this.availableProducts.find(p => p.sku === targetValue);
      return product ? `${product.name} (${product.sku})` : targetValue;
    }
    // For Category, Subcategory, Brand - just return the value
    return targetValue;
  }

  /**
   * Check if target is selected in detail modal
   */
  isDetailTargetSelected(target: string): boolean {
    return this.detailSelectedTargets.includes(target);
  }

  /**
   * Toggle target selection in detail modal
   */
  toggleDetailTarget(target: string): void {
    const index = this.detailSelectedTargets.indexOf(target);
    if (index > -1) {
      this.detailSelectedTargets.splice(index, 1);
    } else {
      this.detailSelectedTargets.push(target);
    }
  }

  /**
   * Remove a target from selected list in detail modal
   */
  removeDetailTarget(targetValue: string): void {
    const index = this.detailSelectedTargets.indexOf(targetValue);
    if (index > -1) {
      this.detailSelectedTargets.splice(index, 1);
    }
  }

  /**
   * Clear all selected targets in detail modal
   */
  clearAllDetailTargets(): void {
    this.detailSelectedTargets = [];
  }

  /**
   * Handle target search change for detail modal
   */
  onDetailTargetSearchChange(): void {
    // Filtering is done in getDetailFilteredTargetOptions()
    this.cdr.detectChanges();
  }

  loadPromotions(): void {
    this.isLoading = true;
    
    console.log('🔄 Loading promotions from MongoDB API (promotions, promotion_usage, promotion_target)...');
    // Load promotions, usage counts, and targets in parallel from 3 collections
    forkJoin({
      promotions: this.apiService.getPromotions(),
      usage: this.apiService.getPromotionUsage(),
      targets: this.apiService.getPromotionTargets()
    }).subscribe({
      next: ({ promotions: data, usage: usageMap, targets: targetsData }) => {
        console.log(`✅ Loaded ${data.length} promotions from MongoDB`);
        console.log(`✅ Loaded usage counts for ${Object.keys(usageMap).length} promotions`);
        console.log(`✅ Loaded ${targetsData.length} promotion targets from MongoDB`);
        
        // Tự động kiểm tra và cập nhật status theo thời gian thực (chỉ khi không đang update)
        if (!this.isUpdatingStatuses) {
          this.autoUpdatePromotionStatuses(data);
        }
        
        // Transform promotions data and merge data from 3 collections
        this.allPromotions = this.transformPromotionsData(data, usageMap, targetsData);
        this.promotions = [...this.allPromotions];
        this.sortByUpdated('desc'); // Default sort
        this.calculateStats();
        this.extractAvailableGroups();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error: any) => {
        console.error('❌ Error loading promotions from MongoDB:', error);
        this.isLoading = false;
        // Don't fallback to JSON - only use MongoDB data
        this.allPromotions = [];
        this.promotions = [];
        this.calculateStats();
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * REMOVED: No longer using JSON fallback - MongoDB only!
   * Fallback: Load promotions from JSON file (deprecated - should not be called)
   */
  private loadPromotionsFromJSON(): void {
    // This method is kept for reference but should not be called
    // All data should come from MongoDB only
    console.warn('⚠️ loadPromotionsFromJSON() is deprecated. Use MongoDB only.');
    return; // Early return to prevent execution
    
    this.isLoading = true;
    
    console.log('🔄 Loading promotions from JSON file... (deprecated)');
    // Load from data/promotion/promotions.json (deprecated)
    this.http.get<PromotionJSON[]>('data/promotion/promotions.json').subscribe({
      next: (data) => {
        console.log(`✅ Loaded ${data.length} promotions from JSON`);
        console.log('🗄️ Data source: JSON file from /data/');
        this.allPromotions = this.transformPromotionsData(data);
        this.promotions = [...this.allPromotions];
        this.sortByUpdated('desc'); // Default sort
        this.calculateStats();
        this.extractAvailableGroups();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error: any) => {
        console.error('❌ Error loading promotions:', error);
        console.error('   Check if data/promotion/promotions.json exists in the unified data folder');
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  transformPromotionsData(data: PromotionJSON[], usageMap: any = {}, targetsData: any[] = []): Promotion[] {
    // Create a map of targets by promotion_id for quick lookup
    const targetsMap: { [key: string]: any } = {};
    targetsData.forEach(target => {
      const promotionId = target.promotion_id || target._id?.toString();
      if (promotionId) {
        targetsMap[promotionId] = target;
      }
    });
    
    return data.map((item, index) => {
      // Support both old format (start_date, end_date) and MongoDB format (startDate, endDate)
      const startDate = this.extractDate(item.startDate || item.start_date);
      const endDate = this.extractDate(item.endDate || item.end_date);
      
      const status = this.getPromotionStatus(startDate, endDate, item.status);
      
      // Convert discount_type - support both formats
      let discountType: 'percentage' | 'fixed' | 'buy1get1' = 'percentage';
      const discountTypeValue = item.discountType || item.discount_type || '';
      
      if (discountTypeValue === 'percent' || discountTypeValue === 'percentage') {
        discountType = 'percentage';
      } else if (discountTypeValue === 'fixed') {
        discountType = 'fixed';
      } else if (discountTypeValue === 'buy1get1') {
        discountType = 'buy1get1';
      }
      
      // Support both formats for discount value
      const discountValue = item.discountValue || item.discount_value || 0;
      const finalDiscountValue = typeof discountValue === 'number' ? discountValue : parseFloat(discountValue.toString()) || 0;
      
      // Support both formats for usage limit
      const usageLimit = item.usageLimit || item.usage_limit || 0;
      const finalUsageLimit = typeof usageLimit === 'number' ? usageLimit : parseInt(usageLimit.toString()) || 0;
      
      // Support both formats for user limit
      const userLimit = item.userLimit || item.user_limit || 0;
      const finalUserLimit = typeof userLimit === 'number' ? userLimit : parseInt(userLimit.toString()) || 0;
      
      // Get promotion_id for mapping usage count
      const promotionId = (item as any).promotion_id || (item as any)._id?.toString() || '';
      const mongoId = (item as any)._id?.toString() || '';
      
      // Get usage count from promotion_usage collection (MongoDB)
      // Priority: usageMap (from MongoDB) > item.usageCount > 0
      let usageCount = 0;
      if (promotionId && usageMap[promotionId] !== undefined) {
        usageCount = usageMap[promotionId];
      } else if (mongoId && usageMap[mongoId] !== undefined) {
        usageCount = usageMap[mongoId];
      } else {
        // Fallback to item data if usageMap doesn't have this promotion
        usageCount = item.usageCount || item.usage_count || 0;
      }
      
      // Support both formats for updated date
      const updatedAt = this.extractDate(item.updatedAt || item.updated_at || '');
      
      // Format dates for HTML date input (YYYY-MM-DD)
      let startDateFormatted = '';
      let endDateFormatted = '';
      if (startDate) {
        const start = new Date(startDate);
        if (!isNaN(start.getTime())) {
          startDateFormatted = start.toISOString().split('T')[0];
        }
      }
      if (endDate) {
        const end = new Date(endDate);
        if (!isNaN(end.getTime())) {
          endDateFormatted = end.toISOString().split('T')[0];
        }
      }

      const promotion: any = {
        id: index + 1,
        code: item.code,
        name: item.name,
        description: item.description,
        type: (item.type as 'User' | 'Admin') || 'User',
        scope: (item.scope as any) || 'Order',
        discountType: discountType,
        discountValue: finalDiscountValue,
        minPurchase: item.minPurchase || item.min_order_value,
        maxDiscount: item.maxDiscount || item.max_discount_value,
        startDate: startDateFormatted || startDate, // Use formatted date for input
        endDate: endDateFormatted || endDate, // Use formatted date for input
        usageLimit: finalUsageLimit,
        userLimit: finalUserLimit,
        usageCount: usageCount,
        isFirstOrderOnly: item.isFirstOrderOnly || item.is_first_order_only || false,
        status: status,
        updatedAt: updatedAt,
        selected: false,
        groups: []
      };

      // Store promotion_id from MongoDB for update operations (reuse promotionId from above)
      if (promotionId) {
        promotion.promotion_id = promotionId;
        
        // Merge target data from promotion_target collection
        const targetData = targetsMap[promotionId];
        if (targetData) {
          promotion.targetProducts = targetData.target_type === 'Product' ? (targetData.target_ref || []) : [];
          promotion.targetCategories = targetData.target_type === 'Category' ? (targetData.target_ref || []) : [];
          promotion.targetType = targetData.target_type;
          promotion.targetRef = targetData.target_ref || [];
        }
      }

      return promotion;
    });
  }

  /**
   * Extract date string from various formats (string, { $date: string }, { "$date": string }, Date, etc.)
   */
  private extractDate(dateValue: any): string {
    if (!dateValue) {
      return '';
    }
    
    // Nếu là string, trả về luôn
    if (typeof dateValue === 'string') {
      return dateValue;
    }
    
    // Nếu là Date object, convert sang ISO string
    if (dateValue instanceof Date) {
      return dateValue.toISOString();
    }
    
    // Nếu là object với $date hoặc "$date"
    if (typeof dateValue === 'object') {
      // Xử lý format { $date: "..." } hoặc { "$date": "..." }
      if (dateValue.$date) {
        return dateValue.$date;
      }
      if (dateValue['$date']) {
        return dateValue['$date'];
      }
      
      // Nếu có các field khác như year, month, day (MongoDB extended JSON)
      if (dateValue.year && dateValue.month !== undefined && dateValue.day) {
        const year = dateValue.year;
        const month = String(dateValue.month + 1).padStart(2, '0'); // month is 0-indexed
        const day = String(dateValue.day).padStart(2, '0');
        return `${year}-${month}-${day}T00:00:00.000Z`;
      }
    }
    
    return '';
  }

  /**
   * Tính toán status cho MongoDB dựa trên thời gian thực
   * @param startDate - Ngày bắt đầu
   * @param endDate - Ngày kết thúc
   * @returns Status string cho MongoDB: "đang diễn ra" | "sắp diễn ra" | "đã kết thúc"
   */
  calculateStatusByRealTime(startDate: string, endDate: string): string {
    if (!startDate || !endDate) {
      console.warn(`⚠️ [calculateStatusByRealTime] Missing dates: start=${startDate}, end=${endDate}`);
      return 'đang diễn ra';
    }
    
    // Lấy thời gian hiện tại và normalize (chỉ so sánh ngày, không so sánh giờ)
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    
    // Parse dates
    const start = new Date(startDate);
    const end = new Date(endDate);
    
    // Kiểm tra nếu dates không hợp lệ
    if (isNaN(start.getTime()) || isNaN(end.getTime())) {
      console.warn(`⚠️ [calculateStatusByRealTime] Invalid dates: start=${startDate}, end=${endDate}`);
      return 'đang diễn ra';
    }
    
    // Normalize dates để so sánh chính xác
    start.setHours(0, 0, 0, 0);
    end.setHours(23, 59, 59, 999); // End date là cuối ngày
    
    // Logic tính toán status:
    // - Nếu now < start: sắp diễn ra (chưa đến ngày bắt đầu)
    // - Nếu now >= start && now <= end: đang diễn ra (trong khoảng thời gian)
    // - Nếu now > end: đã kết thúc (qua ngày kết thúc)
    let status: string;
    if (now < start) {
      status = 'sắp diễn ra';
    } else if (now > end) {
      status = 'đã kết thúc';
    } else {
      status = 'đang diễn ra';
    }
    
    // Debug log
    console.log(`🔍 [calculateStatusByRealTime]`, {
      now: now.toISOString().split('T')[0],
      start: start.toISOString().split('T')[0],
      end: end.toISOString().split('T')[0],
      nowLessThanStart: now < start,
      nowGreaterThanEnd: now > end,
      calculatedStatus: status
    });
    
    return status;
  }

  /**
   * Tự động cập nhật status của tất cả promotions trong MongoDB dựa trên thời gian thực
   * @param data - Dữ liệu promotions từ MongoDB (để tránh gọi API 2 lần)
   */
  autoUpdatePromotionStatuses(data: any[]): void {
    // Tránh gọi lại khi đang update
    if (this.isUpdatingStatuses) {
      console.log('⚠️ [Auto Update] Đang cập nhật status, bỏ qua lần gọi này...');
      return;
    }
    
    console.log('🔄 [Auto Update] Bắt đầu kiểm tra và cập nhật status theo thời gian thực...');
    
    // Lấy thời gian hiện tại
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    console.log(`📅 [Auto Update] Thời gian hiện tại: ${now.getDate()}/${now.getMonth() + 1}/${now.getFullYear()}`);
    console.log(`📋 [Auto Update] Đang kiểm tra ${data.length} promotions...`);
    
    const updates: Array<{ id: string; code: string; name: string; currentStatus: string; newStatus: string; startDate: string; endDate: string }> = [];
    
    // Kiểm tra từng promotion
    data.forEach((item) => {
      const startDateRaw = item.startDate || item.start_date;
      const endDateRaw = item.endDate || item.end_date;
      const startDate = this.extractDate(startDateRaw);
      const endDate = this.extractDate(endDateRaw);
      const currentStatus = item.status || '';
      
      // Debug log để kiểm tra dates
      console.log(`🔍 [Auto Update] Promotion ${item.code}:`, {
        startDateRaw: startDateRaw,
        startDateExtracted: startDate,
        endDateRaw: endDateRaw,
        endDateExtracted: endDate,
        currentStatus: currentStatus
      });
      
      if (!startDate || !endDate) {
        console.warn(`⚠️ [Auto Update] Promotion ${item.code} thiếu dates - bỏ qua`);
        console.warn(`   startDateRaw:`, startDateRaw);
        console.warn(`   endDateRaw:`, endDateRaw);
        return;
      }
      
      // Tính toán status nên có dựa trên thời gian thực
      const shouldBeStatus = this.calculateStatusByRealTime(startDate, endDate);
      
      // Debug log để kiểm tra tính toán
      console.log(`🔍 [Auto Update] Promotion ${item.code} - Tính toán:`, {
        startDate: startDate,
        endDate: endDate,
        shouldBeStatus: shouldBeStatus,
        currentStatus: currentStatus
      });
      
      // Map các status tương đương (để so sánh)
      const statusEquivalents: { [key: string]: string[] } = {
        'đang diễn ra': ['Active', 'đang diễn ra', 'active'],
        'sắp diễn ra': ['sắp diễn ra'],
        'đã kết thúc': ['Expired', 'expired', 'đã kết thúc', 'Inactive', 'inactive']
      };
      
      // Kiểm tra xem currentStatus có khớp với shouldBeStatus không
      const equivalents = statusEquivalents[shouldBeStatus] || [];
      const currentStatusMatches = equivalents.includes(currentStatus);
      
      if (!currentStatusMatches) {
        const promotionId = item.promotion_id || item._id;
        console.log(`🔄 [Auto Update] Promotion ${item.code} (${item.name}):`);
        console.log(`   Status hiện tại: "${currentStatus}"`);
        console.log(`   Status nên có: "${shouldBeStatus}"`);
        console.log(`   Ngày bắt đầu: ${startDate}`);
        console.log(`   Ngày kết thúc: ${endDate}`);
        
        updates.push({
          id: promotionId,
          code: item.code,
          name: item.name || '',
          currentStatus: currentStatus,
          newStatus: shouldBeStatus,
          startDate: startDate,
          endDate: endDate
        });
      }
    });
    
    // Cập nhật các promotions cần update
    if (updates.length > 0) {
      console.log(`🔄 [Auto Update] Cần cập nhật ${updates.length} promotions trong MongoDB...`);
      this.isUpdatingStatuses = true;
      
      // Update sequentially để tránh quá tải server
      let updateIndex = 0;
      const updateNext = () => {
        if (updateIndex >= updates.length) {
          console.log(`✅ [Auto Update] Đã cập nhật xong tất cả ${updates.length} promotions. Đang reload...`);
          this.isUpdatingStatuses = false;
          
          // Reload promotions sau khi update xong (không gọi autoUpdate lại)
          setTimeout(() => {
            this.isUpdatingStatuses = true; // Tạm thời set flag để tránh auto update
            this.apiService.getPromotions().subscribe({
              next: (newData) => {
                this.allPromotions = this.transformPromotionsData(newData);
                this.promotions = [...this.allPromotions];
                this.sortByUpdated('desc');
                this.calculateStats();
                this.extractAvailableGroups();
                this.isLoading = false;
                this.isUpdatingStatuses = false; // Reset flag
                this.cdr.detectChanges();
                this.displayPopup('Thao tác thành công', 'success');
              },
              error: (error: any) => {
                console.error('❌ Error reloading promotions:', error);
                this.isUpdatingStatuses = false;
                this.isLoading = false;
                this.cdr.detectChanges();
              }
            });
          }, 500);
          return;
        }
        
        const update = updates[updateIndex];
        console.log(`🔄 [Auto Update] Đang cập nhật ${update.code}: "${update.currentStatus}" → "${update.newStatus}"`);
        
        this.http.put(`${environment.apiUrl}/promotions/${update.id}`, {
          status: update.newStatus
        }).subscribe({
          next: (response: any) => {
            console.log(`✅ [Auto Update] Đã cập nhật ${update.code} thành công trong MongoDB`);
            console.log(`   Response:`, response);
            updateIndex++;
            updateNext();
          },
          error: (error: any) => {
            console.error(`❌ [Auto Update] Lỗi khi cập nhật ${update.code}:`, error);
            console.error(`   Error details:`, {
              status: error.status,
              statusText: error.statusText,
              message: error.message,
              error: error.error
            });
            updateIndex++;
            updateNext(); // Tiếp tục với promotion tiếp theo dù có lỗi
          }
        });
      };
      
      // Bắt đầu update
      updateNext();
    } else {
      console.log(`✅ [Auto Update] Tất cả promotions đã có status đúng. Không cần cập nhật.`);
    }
  }

  getPromotionStatus(startDate: string, endDate: string, jsonStatus?: string): 'active' | 'upcoming' | 'expired' {
    // If status is Draft, treat as upcoming regardless of dates
    if (jsonStatus === 'Draft' || jsonStatus === 'draft') {
      return 'upcoming';
    }
    
    if (!startDate || !endDate) {
      return 'active'; // Default if dates are missing
    }
    
    // Lấy thời gian hiện tại và normalize (chỉ so sánh ngày, không so sánh giờ)
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    
    const start = new Date(startDate);
    const end = new Date(endDate);
    
    // Normalize dates để so sánh chính xác
    start.setHours(0, 0, 0, 0);
    end.setHours(23, 59, 59, 999); // End date là cuối ngày
    
    // Check if dates are valid
    if (isNaN(start.getTime()) || isNaN(end.getTime())) {
      return 'active'; // Default if dates are invalid
    }

    // Tính toán status dựa trên thời gian thực
    if (now < start) {
      return 'upcoming';
    } else if (now > end) {
      return 'expired';
    } else {
      return 'active';
    }
  }

  /**
   * ============================================================================
   * STATISTICS
   * ============================================================================
   */

  calculateStats(): void {
    this.totalCount = this.promotions.length;
    this.activeCount = this.promotions.filter(p => p.status === 'active').length;
    this.upcomingCount = this.promotions.filter(p => p.status === 'upcoming').length;
    this.expiredCount = this.promotions.filter(p => p.status === 'expired').length;
  }

  /**
   * ============================================================================
   * SELECTION
   * ============================================================================
   */

  get selectedCount(): number {
    return this.filteredPromotions.filter(p => p.selected).length;
  }

  get allSelected(): boolean {
    return this.filteredPromotions.length > 0 && 
           this.filteredPromotions.every(p => p.selected);
  }

  toggleSelectAll(): void {
    const newState = !this.allSelected;
    this.filteredPromotions.forEach(p => p.selected = newState);
  }

  toggleSelect(promotion: Promotion): void {
    promotion.selected = !promotion.selected;
  }

  /**
   * ============================================================================
   * SEARCH & SORT
   * ============================================================================
   */

  searchPromotions(event: Event): void {
    const query = (event.target as HTMLInputElement).value.toLowerCase();
    this.searchQuery = query;
    this.applySearch();
  }

  applySearch(): void {
    let result = [...this.promotions];

    // Apply search
    if (this.searchQuery) {
      result = result.filter(p => 
        p.code.toLowerCase().includes(this.searchQuery) ||
        p.name.toLowerCase().includes(this.searchQuery) ||
        (p.description && p.description.toLowerCase().includes(this.searchQuery))
      );
    }

    this.filteredPromotions = result;
  }

  /**
   * Sort by updated date
   */
  sortByUpdated(order: 'asc' | 'desc' = 'desc'): void {
    this.currentSortBy = 'updated';
    this.currentSortOrder = order;
    
    this.promotions.sort((a, b) => {
      const dateA = new Date(a.updatedAt || a.startDate).getTime();
      const dateB = new Date(b.updatedAt || b.startDate).getTime();
      
      return order === 'asc' ? dateA - dateB : dateB - dateA;
    });
    
    console.log(`📊 Sorted promotions by date: ${order}`);
    this.applySearch();
    this.closeSortDropdown();
  }

  /**
   * Sort by usage count
   */
  sortByUsage(order: 'asc' | 'desc' = 'desc'): void {
    this.currentSortBy = 'usage';
    this.currentSortOrder = order;
    
    this.promotions.sort((a, b) => {
      const usageA = a.usageCount || 0;
      const usageB = b.usageCount || 0;
      
      return order === 'asc' ? usageA - usageB : usageB - usageA;
    });
    
    console.log(`📊 Sorted promotions by usage: ${order}`);
    this.applySearch();
    this.closeSortDropdown();
  }

  /**
   * Toggle sort order (asc <-> desc)
   */
  toggleSortOrder(): void {
    const newOrder = this.currentSortOrder === 'asc' ? 'desc' : 'asc';
    
    if (this.currentSortBy === 'updated') {
      this.sortByUpdated(newOrder);
    } else if (this.currentSortBy === 'usage') {
      this.sortByUsage(newOrder);
    }
  }

  /**
   * Filter by stat card
   */
  filterByStatus(status: 'total' | 'expired' | 'active' | 'upcoming'): void {
    if (this.selectedStatFilter === status) {
      // Unselect if clicking the same card
      this.selectedStatFilter = null;
      this.promotions = [...this.allPromotions];
    } else {
      // Select new status
      this.selectedStatFilter = status;
      if (status === 'total') {
        // Show all promotions
        this.promotions = [...this.allPromotions];
      } else {
        // Filter by status
        this.promotions = this.allPromotions.filter(p => p.status === status);
      }
    }
    
    // Re-apply current sort
    if (this.currentSortBy === 'updated') {
      this.sortByUpdated(this.currentSortOrder);
    } else {
      this.sortByUsage(this.currentSortOrder);
    }
  }

  /**
   * ============================================================================
   * ACTIONS
   * ============================================================================
   */

  addPromotion(): void {
    this.editMode = false;
    // Get today's date in YYYY-MM-DD format for date inputs
    const today = new Date().toISOString().split('T')[0];
    // Set end date to 30 days from now
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 30);
    const endDate = futureDate.toISOString().split('T')[0];
    
    this.currentPromotion = {
      code: '',
      name: '',
      description: '',
      type: 'User',
      scope: 'Order',
      discountType: 'percentage',
      discountValue: 0,
      minPurchase: 0,
      maxDiscount: 0,
      startDate: today,
      endDate: endDate,
      usageLimit: 0,
      userLimit: 1,
      usageCount: 0,
      isFirstOrderOnly: false,
      status: 'upcoming'
    };
    
    // Reset target selection
    this.selectedTargets = [];
    this.targetType = 'Category';
    this.targetSearchTerm = '';
    
    this.showAddEditModal = true;
  }

  editPromotions(): void {
    const selected = this.filteredPromotions.filter(p => p.selected);
    if (selected.length === 1) {
      this.editMode = true;
      const selectedPromotion = selected[0];
      
      // Copy promotion và đảm bảo promotion_id được copy
      this.currentPromotion = { ...selectedPromotion };
      
      // Đảm bảo promotion_id được copy vào currentPromotion
      if ((selectedPromotion as any).promotion_id) {
        (this.currentPromotion as any).promotion_id = (selectedPromotion as any).promotion_id;
      }
      
      console.log('🔄 [Edit Promotion] Editing promotion:', {
        code: this.currentPromotion.code,
        promotion_id: (this.currentPromotion as any).promotion_id,
        originalPromotion: selectedPromotion
      });
      
      // Reset target selection before loading
      this.selectedTargets = [];
      this.targetSearchTerm = '';
      this.targetType = 'Category'; // Default, will be updated by loadPromotionTarget
      
      // Load promotion target if exists
      const promotionId = (selectedPromotion as any).promotion_id;
      if (promotionId) {
        // Wait a bit to ensure currentPromotion is set
        setTimeout(() => {
          this.loadPromotionTarget(promotionId);
        }, 100);
      }
      
      this.showAddEditModal = true;
    } else if (selected.length > 1) {
      this.notificationService.showWarning('Vui lòng chỉ chọn 1 khuyến mãi để chỉnh sửa');
    }
  }

  deletePromotions(): void {
    const selected = this.filteredPromotions.filter(p => p.selected);
    if (selected.length === 0) return;

    // Show confirmation modal
    this.confirmMessage = `Bạn có chắc muốn xóa ${selected.length} khuyến mãi đã chọn?`;
    this.confirmCallback = () => {
      // Delete promotions via API
      const deletePromises = selected.map(promotion => {
        const promotionId = (promotion as any).promotion_id || promotion.id?.toString() || promotion.code;
        if (!promotionId) {
          console.warn('⚠️ Promotion missing ID:', promotion);
          return Promise.resolve(null);
        }
        return this.http.delete(`${environment.apiUrl}/promotions/${promotionId}`).toPromise();
      });

      Promise.all(deletePromises).then(results => {
        console.log('📊 Delete results:', results);
        
        // Filter out null results and check for errors
        const validResults = results.filter(r => r !== null && r !== undefined);
        const successResults = validResults.filter(r => {
          // Check if result has success property
          if (r && typeof r === 'object') {
            const result = r as any;
            return result.success !== false;
          }
          return true;
        });
        
        const successCount = successResults.length;
        const failedCount = validResults.length - successCount;
        
        console.log(`✅ Deleted ${successCount} promotions successfully`);
        if (failedCount > 0) {
          console.warn(`⚠️ Failed to delete ${failedCount} promotions`);
        }
        
        // Reload promotions from MongoDB to get updated list
        this.loadPromotions();
        
        this.applySearch();
        this.calculateStats();
        
        if (failedCount > 0) {
          this.displayPopup(`Đã xóa ${successCount} khuyến mãi, ${failedCount} khuyến mãi lỗi`, 'error');
        } else {
          this.displayPopup('Thao tác thành công', 'success');
        }
        this.closeConfirmModal();
      }).catch(error => {
        console.error('❌ Error deleting promotions:', error);
        console.error('❌ Error details:', {
          status: error.status,
          statusText: error.statusText,
          message: error.message,
          error: error.error,
          url: error.url
        });
        
        const errorMessage = error.error?.message || error.error?.error || error.message || 'Lỗi không xác định';
        this.displayPopup('Lỗi khi xóa khuyến mãi: ' + errorMessage, 'error');
        
        // Still reload to sync with server
        this.loadPromotions();
        this.closeConfirmModal();
      });
    };
    this.showConfirmModal = true;
  }

  /**
   * Show confirmation modal
   */
  showConfirmation(message: string, callback: () => void): void {
    this.confirmMessage = message;
    this.confirmCallback = callback;
    this.showConfirmModal = true;
  }

  /**
   * Close confirmation modal
   */
  closeConfirmModal(): void {
    this.showConfirmModal = false;
    this.confirmMessage = '';
    this.confirmCallback = null;
  }

  /**
   * Display popup notification
   */
  displayPopup(message: string, type: 'success' | 'error' | 'info' = 'success'): void {
    this.popupMessage = message;
    this.popupType = type;
    this.showPopup = true;
  }

  /**
   * Close popup notification
   */
  closePopup(): void {
    this.showPopup = false;
    this.popupMessage = '';
  }

  /**
   * Confirm action
   */
  onConfirm(): void {
    if (this.confirmCallback) {
      this.confirmCallback();
    }
  }

  savePromotion(): void {
    if (!this.currentPromotion) return;
    
    // Validate required fields
    if (!this.currentPromotion.code || !this.currentPromotion.name || 
        !this.currentPromotion.discountValue || !this.currentPromotion.startDate || 
        !this.currentPromotion.endDate) {
      this.notificationService.showWarning('Vui lòng điền đầy đủ các trường bắt buộc (*)');
      return;
    }

    // Validate date range: end date must be >= start date
    if (this.currentPromotion.startDate && this.currentPromotion.endDate) {
      const startDate = new Date(this.currentPromotion.startDate);
      const endDate = new Date(this.currentPromotion.endDate);
      
      if (endDate < startDate) {
        this.notificationService.showWarning('Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu');
        return;
      }
    }

    // Validate target selection for Category/Product/Brand scope
    if ((this.currentPromotion.scope === 'Category' || 
         this.currentPromotion.scope === 'Product' || 
         this.currentPromotion.scope === 'Brand') && 
        (!this.selectedTargets || this.selectedTargets.length === 0)) {
      this.notificationService.showWarning('Vui lòng chọn ít nhất một ' + 
        (this.currentPromotion.scope === 'Category' ? 'danh mục' : 
         this.currentPromotion.scope === 'Brand' ? 'thương hiệu' : 'sản phẩm'));
      return;
    }

    // Map frontend format to backend format
    const promotionData: any = {
      code: this.currentPromotion.code.trim(),
      name: this.currentPromotion.name.trim(),
      description: this.currentPromotion.description || '',
      type: this.currentPromotion.type || 'User',
      scope: this.currentPromotion.scope || 'Order',
      discount_type: this.currentPromotion.discountType === 'percentage' ? 'percent' : 
                     this.currentPromotion.discountType || 'fixed',
      discount_value: Number(this.currentPromotion.discountValue) || 0,
      max_discount_value: Number(this.currentPromotion.maxDiscount) || 0,
      min_order_value: Number(this.currentPromotion.minPurchase) || 0,
      usage_limit: Number(this.currentPromotion.usageLimit) || 0,
      user_limit: Number(this.currentPromotion.userLimit) || 1,
      is_first_order_only: this.currentPromotion.isFirstOrderOnly || false,
      start_date: new Date(this.currentPromotion.startDate),
      end_date: new Date(this.currentPromotion.endDate),
      status: this.mapStatusToBackend(this.currentPromotion.status || 'active'),
      created_by: 'admin',
      created_at: new Date(),
      updated_at: new Date()
    };

    // Don't generate promotion_id in frontend - let backend generate it with format PROMOxxx
    // Backend will automatically generate promotion_id: PROMO001, PROMO002, etc.

    if (this.editMode) {
      // Update existing promotion
      this.updatePromotionInMongoDB(promotionData);
    } else {
      // Add new promotion to MongoDB
      this.createPromotionInMongoDB(promotionData);
    }
  }

  /**
   * Map frontend status to backend status format
   */
  mapStatusToBackend(status: string): string {
    if (status === 'active') return 'Active';
    if (status === 'upcoming') return 'Active'; // Upcoming promotions are still Active in backend
    if (status === 'expired') return 'Expired';
    if (status === 'Active' || status === 'Expired' || status === 'Inactive') return status;
    return 'Active'; // Default
  }

  /**
   * Create promotion target if needed
   */
  async createPromotionTarget(promotionId: string): Promise<void> {
    // Only create target if scope is Category, Product, or Brand
    const scope = this.currentPromotion?.scope;
    if (scope !== 'Category' && scope !== 'Product' && scope !== 'Brand') {
      return;
    }

    // Check if targets are selected
    if (!this.selectedTargets || this.selectedTargets.length === 0) {
      console.log('⚠️ No targets selected, skipping promotion_target creation');
      return;
    }

    // Map scope to target_type
    let targetType: 'Category' | 'Subcategory' | 'Brand' | 'Product';
    if (scope === 'Category') {
      targetType = this.targetType === 'Subcategory' ? 'Subcategory' : 'Category';
    } else if (scope === 'Brand') {
      targetType = 'Brand';
    } else if (scope === 'Product') {
      targetType = 'Product';
    } else {
      return;
    }

    // Prepare target data
    const targetData = {
      promotion_id: promotionId,
      target_type: targetType,
      target_ref: this.selectedTargets
    };

    // Create promotion target
    this.http.post(`${environment.apiUrl}/promotion-targets`, targetData).subscribe({
      next: (response: any) => {
        if (response.success) {
          console.log('✅ Promotion target created successfully:', response.data);
        }
      },
      error: (error: any) => {
        console.error('❌ Error creating promotion target:', error);
        // Don't fail the entire operation if target creation fails
      }
    });
  }

  /**
   * Create new promotion in MongoDB
   */
  createPromotionInMongoDB(promotionData: any): void {
    this.http.post(`${environment.apiUrl}/promotions`, promotionData).subscribe({
      next: (response: any) => {
        if (response.success) {
          console.log('✅ Promotion created successfully:', response.data);
          
          // Get promotion_id from response
          const promotionId = response.data.promotion_id || response.data._id?.toString();
          
          // Create promotion target if needed
          if (promotionId && (promotionData.scope === 'Category' || promotionData.scope === 'Product' || promotionData.scope === 'Brand')) {
            this.createPromotionTarget(promotionId);
          }
          
          // Show success popup
          this.displayPopup('Thao tác thành công', 'success');
          
          // Reload promotions from MongoDB
          this.loadPromotions();
          
          // Close modal
          this.closeAddEditModal();
        } else {
          this.notificationService.showError(response.message || 'Lỗi khi tạo khuyến mãi');
        }
      },
      error: (error: any) => {
        console.error('❌ Error creating promotion:', error);
        const errorMessage = error.error?.message || error.error?.error || 'Lỗi khi tạo khuyến mãi';
        this.notificationService.showError(errorMessage);
      }
    });
  }

  /**
   * Update promotion in MongoDB
   */
  updatePromotionInMongoDB(promotionData: any): void {
    console.log('🔄 [Update Promotion] Bắt đầu cập nhật promotion...');
    console.log('📋 [Update Promotion] Current promotion:', this.currentPromotion);
    console.log('📋 [Update Promotion] Promotion data to update:', promotionData);
    
    if (!this.currentPromotion?.code) {
      console.error('❌ [Update Promotion] Không tìm thấy mã khuyến mãi');
      this.notificationService.showError('Không tìm thấy mã khuyến mãi');
      return;
    }

    // Find promotion_id from current promotion
    // When editing, we need to find the original promotion to get its promotion_id
    const originalPromotion = this.allPromotions.find(p => p.code === this.currentPromotion!.code);
    
    console.log('🔍 [Update Promotion] Original promotion found:', originalPromotion);
    
    if (!originalPromotion) {
      console.error('❌ [Update Promotion] Không tìm thấy khuyến mãi trong danh sách để cập nhật');
      console.error('   Code tìm kiếm:', this.currentPromotion.code);
      console.error('   All promotions codes:', this.allPromotions.map(p => p.code));
      this.notificationService.showError('Không tìm thấy khuyến mãi để cập nhật');
      return;
    }

    // Use promotion_id or code to identify the promotion
    // Backend can find by either promotion_id or code
    const promotionId = (originalPromotion as any).promotion_id;
    const identifier = promotionId || this.currentPromotion.code;
    
    console.log('🔍 [Update Promotion] Promotion ID:', promotionId);
    console.log('🔍 [Update Promotion] Identifier to use:', identifier);
    
    // Keep the original promotion_id if editing
    if (promotionId) {
      promotionData.promotion_id = promotionId;
    }
    
    // Tự động tính toán status dựa trên dates khi update
    if (promotionData.start_date && promotionData.end_date) {
      const startDateStr = promotionData.start_date instanceof Date 
        ? promotionData.start_date.toISOString().split('T')[0]
        : promotionData.start_date;
      const endDateStr = promotionData.end_date instanceof Date
        ? promotionData.end_date.toISOString().split('T')[0]
        : promotionData.end_date;
      
      const calculatedStatus = this.calculateStatusByRealTime(startDateStr, endDateStr);
      promotionData.status = calculatedStatus;
      console.log('🔄 [Update Promotion] Tự động tính toán status:', calculatedStatus);
    }
    
    console.log('📤 [Update Promotion] Sending PUT request to:', `${environment.apiUrl}/promotions/${identifier}`);
    console.log('📤 [Update Promotion] Request body:', promotionData);
    
    this.http.put(`${environment.apiUrl}/promotions/${identifier}`, promotionData).subscribe({
      next: (response: any) => {
        console.log('✅ [Update Promotion] Response received:', response);
        
        if (response.success) {
          console.log('✅ [Update Promotion] Promotion updated successfully:', response.data);
          
          // Update promotion target if needed
          // Wait a bit to ensure promotion is updated first
          setTimeout(() => {
            if (promotionId && (promotionData.scope === 'Category' || promotionData.scope === 'Product' || promotionData.scope === 'Brand')) {
              // Use POST which will create or update
              console.log('🔄 [Update Promotion] Updating promotion target...');
              this.createPromotionTarget(promotionId);
            } else if (promotionId && (promotionData.scope === 'Order' || promotionData.scope === 'Shipping')) {
              // Delete target if scope changed to Order/Shipping
              console.log('🔄 [Update Promotion] Deleting promotion target...');
              this.http.delete(`${environment.apiUrl}/promotion-targets/${promotionId}`).subscribe({
                next: () => console.log('✅ Promotion target deleted'),
                error: (err) => {
                  // 404 is okay - target might not exist
                  if (err.status !== 404) {
                    console.log('⚠️ Could not delete target:', err);
                  }
                }
              });
            }
          }, 100);
          
          // Show success popup
          this.displayPopup('Thao tác thành công', 'success');
          
          // Reload promotions from MongoDB
          this.loadPromotions();
          
          // Close modal
          this.closeAddEditModal();
        } else {
          console.error('❌ [Update Promotion] Update failed:', response.message);
          this.displayPopup(response.message || 'Lỗi khi cập nhật khuyến mãi', 'error');
        }
      },
      error: (error: any) => {
        console.error('❌ [Update Promotion] Error updating promotion:', error);
        console.error('❌ [Update Promotion] Error details:', {
          status: error.status,
          statusText: error.statusText,
          message: error.message,
          error: error.error,
          url: error.url
        });
        
        const errorMessage = error.error?.message || error.error?.error || error.message || 'Lỗi khi cập nhật khuyến mãi';
        this.displayPopup('Lỗi khi cập nhật khuyến mãi: ' + errorMessage, 'error');
      }
    });
  }

  /**
   * ============================================================================
   * GROUPING
   * ============================================================================
   */

  openGroupModal(): void {
    const selected = this.filteredPromotions.filter(p => p.selected);
    if (selected.length < 2) {
      this.notificationService.showWarning('Vui lòng chọn ít nhất 2 khuyến mãi để nhóm');
      return;
    }
    this.showGroupModal = true;
  }

  closeGroupModal(): void {
    this.showGroupModal = false;
  }

  createGroup(groupName: string): void {
    if (!groupName.trim()) {
      this.notificationService.showWarning('Vui lòng nhập tên nhóm');
      return;
    }

    const selected = this.filteredPromotions.filter(p => p.selected);
    selected.forEach(promotion => {
      if (!promotion.groups) {
        promotion.groups = [];
      }
      if (!promotion.groups.includes(groupName)) {
        promotion.groups.push(groupName);
      }
    });

    this.extractAvailableGroups();
    this.closeGroupModal();
    this.displayPopup('Thao tác thành công', 'success');
  }

  extractAvailableGroups(): void {
    const groups = new Set<string>();
    this.promotions.forEach(p => {
      if (p.groups) {
        p.groups.forEach(g => groups.add(g));
      }
    });
    this.availableGroups = Array.from(groups);
  }

  /**
   * ============================================================================
   * UI HELPERS
   * ============================================================================
   */

  toggleSortDropdown(event: Event): void {
    event.stopPropagation();
    this.showSortDropdown = !this.showSortDropdown;
    console.log('🔄 Toggle dropdown:', this.showSortDropdown);
    this.cdr.detectChanges();
  }

  closeSortDropdown(): void {
    this.showSortDropdown = false;
    this.cdr.detectChanges();
  }

  closeDropdowns(event: Event): void {
    this.showSortDropdown = false;
  }

  /**
   * Sort promotions based on selected option
   */
  sortPromotions(): void {
    if (!this.selectedSort) {
      // Reset to original order if no sort selected
      this.filteredPromotions = [...this.promotions];
      return;
    }

    const sorted = [...this.filteredPromotions];

    switch (this.selectedSort) {
      case 'date_desc':
        // Sort by updated date - newest first
        sorted.sort((a, b) => {
          const dateA = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
          const dateB = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
          return dateB - dateA;
        });
        break;

      case 'date_asc':
        // Sort by updated date - oldest first
        sorted.sort((a, b) => {
          const dateA = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
          const dateB = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
          return dateA - dateB;
        });
        break;

      case 'usage_desc':
        // Sort by usage count - highest first
        sorted.sort((a, b) => b.usageCount - a.usageCount);
        break;

      case 'usage_asc':
        // Sort by usage count - lowest first
        sorted.sort((a, b) => a.usageCount - b.usageCount);
        break;
    }

    this.filteredPromotions = sorted;
    console.log(`📊 Sorted promotions by: ${this.selectedSort}`);
  }

  closeAddEditModal(): void {
    this.showAddEditModal = false;
    this.currentPromotion = null;
    this.dateRangeError = false;
  }

  /**
   * Handle start date change in add/edit modal
   */
  onStartDateChange(): void {
    if (this.currentPromotion?.startDate && this.currentPromotion?.endDate) {
      const startDate = new Date(this.currentPromotion.startDate);
      const endDate = new Date(this.currentPromotion.endDate);
      this.dateRangeError = endDate < startDate;
      
      // If end date is invalid, reset it to start date
      if (this.dateRangeError) {
        this.currentPromotion.endDate = this.currentPromotion.startDate;
        this.dateRangeError = false;
      }
    } else {
      this.dateRangeError = false;
    }
  }

  /**
   * Handle end date change in add/edit modal
   */
  onEndDateChange(): void {
    if (this.currentPromotion?.startDate && this.currentPromotion?.endDate) {
      const startDate = new Date(this.currentPromotion.startDate);
      const endDate = new Date(this.currentPromotion.endDate);
      this.dateRangeError = endDate < startDate;
    } else {
      this.dateRangeError = false;
    }
  }

  /**
   * Handle start date change in detail modal
   */
  onDetailStartDateChange(): void {
    if (this.selectedPromotion?.startDate && this.selectedPromotion?.endDate) {
      const startDate = new Date(this.selectedPromotion.startDate);
      const endDate = new Date(this.selectedPromotion.endDate);
      this.detailDateRangeError = endDate < startDate;
      
      // If end date is invalid, reset it to start date
      if (this.detailDateRangeError) {
        this.selectedPromotion.endDate = this.selectedPromotion.startDate;
        this.detailDateRangeError = false;
      }
    } else {
      this.detailDateRangeError = false;
    }
  }

  /**
   * Handle end date change in detail modal
   */
  onDetailEndDateChange(): void {
    if (this.selectedPromotion?.startDate && this.selectedPromotion?.endDate) {
      const startDate = new Date(this.selectedPromotion.startDate);
      const endDate = new Date(this.selectedPromotion.endDate);
      this.detailDateRangeError = endDate < startDate;
    } else {
      this.detailDateRangeError = false;
    }
  }

  /**
   * View promotion detail
   */
  viewPromotionDetail(promotion: Promotion): void {
    this.selectedPromotion = { ...promotion };
    
    // Reset detail target selection
    this.detailSelectedTargets = [];
    this.detailTargetSearchTerm = '';
    this.detailTargetType = 'Category';
    
    // Load promotion target if exists
    const promotionId = (promotion as any).promotion_id;
    if (promotionId) {
      // Wait a bit to ensure selectedPromotion is set
      setTimeout(() => {
        this.loadDetailPromotionTarget(promotionId);
      }, 100);
    }
    
    // Set default target type based on scope
    if (this.selectedPromotion.scope === 'Category') {
      this.detailTargetType = 'Category';
    } else if (this.selectedPromotion.scope === 'Brand') {
      this.detailTargetType = 'Brand';
    } else if (this.selectedPromotion.scope === 'Product') {
      this.detailTargetType = 'Product';
    }
    
    this.showDetailModal = true;
    this.cdr.detectChanges();
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedPromotion = null;
    // Reset detail target selection
    this.detailSelectedTargets = [];
    this.detailTargetSearchTerm = '';
    this.detailTargetType = 'Category';
    this.detailDateRangeError = false;
  }

  /**
   * Save changes from detail modal
   */
  saveDetailChanges(): void {
    if (!this.selectedPromotion) return;

    // Validate required fields
    if (!this.selectedPromotion.code || !this.selectedPromotion.name || 
        !this.selectedPromotion.discountValue || !this.selectedPromotion.startDate || 
        !this.selectedPromotion.endDate) {
      this.notificationService.showWarning('Vui lòng điền đầy đủ các trường bắt buộc (*)');
      return;
    }

    // Validate date range: end date must be >= start date
    if (this.selectedPromotion.startDate && this.selectedPromotion.endDate) {
      const startDate = new Date(this.selectedPromotion.startDate);
      const endDate = new Date(this.selectedPromotion.endDate);
      
      if (endDate < startDate) {
        this.notificationService.showWarning('Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu');
        return;
      }
    }

    // Validate target selection for Category/Product/Brand scope
    if ((this.selectedPromotion.scope === 'Category' || 
         this.selectedPromotion.scope === 'Product' || 
         this.selectedPromotion.scope === 'Brand') && 
        (!this.detailSelectedTargets || this.detailSelectedTargets.length === 0)) {
      this.notificationService.showWarning('Vui lòng chọn ít nhất một ' + 
        (this.selectedPromotion.scope === 'Category' ? 'danh mục' : 
         this.selectedPromotion.scope === 'Brand' ? 'thương hiệu' : 'sản phẩm'));
      return;
    }

    // Map frontend format to backend format
    const promotionData: any = {
      code: this.selectedPromotion.code.trim(),
      name: this.selectedPromotion.name.trim(),
      description: this.selectedPromotion.description || '',
      type: this.selectedPromotion.type || 'User',
      scope: this.selectedPromotion.scope || 'Order',
      discount_type: this.selectedPromotion.discountType === 'percentage' ? 'percent' : 
                     this.selectedPromotion.discountType || 'fixed',
      discount_value: Number(this.selectedPromotion.discountValue) || 0,
      max_discount_value: Number(this.selectedPromotion.maxDiscount) || 0,
      min_order_value: Number(this.selectedPromotion.minPurchase) || 0,
      usage_limit: Number(this.selectedPromotion.usageLimit) || 0,
      user_limit: Number(this.selectedPromotion.userLimit) || 1,
      is_first_order_only: this.selectedPromotion.isFirstOrderOnly || false,
      start_date: new Date(this.selectedPromotion.startDate),
      end_date: new Date(this.selectedPromotion.endDate),
      status: this.mapStatusToBackend(this.selectedPromotion.status || 'active'),
      updated_at: new Date()
    };

    // Get promotion_id from selectedPromotion
    const promotionId = (this.selectedPromotion as any).promotion_id || this.selectedPromotion.code;

    // Update promotion in MongoDB
    this.updateDetailPromotionInMongoDB(promotionData, promotionId);
  }

  /**
   * Update promotion in MongoDB from detail modal
   */
  updateDetailPromotionInMongoDB(promotionData: any, promotionId: string): void {
    // First, try to update by promotion_id
    this.http.put<any>(`${environment.apiUrl}/promotions/${promotionId}`, promotionData).subscribe({
      next: (response: any) => {
        if (response.success) {
          console.log('✅ Updated promotion in MongoDB:', response.data);
          
          // Update promotion target if needed
          this.updateDetailPromotionTarget(promotionId);
          
          // Reload promotions to get latest data
          this.loadPromotions();
          
          this.closeDetailModal();
          this.displayPopup('Thao tác thành công', 'success');
        } else {
          console.error('❌ Error updating promotion:', response.message);
          this.displayPopup('Lỗi khi cập nhật khuyến mãi: ' + (response.message || 'Unknown error'), 'error');
        }
      },
      error: (error: any) => {
        console.error('❌ Error updating promotion:', error);
        this.displayPopup('Lỗi khi cập nhật khuyến mãi: ' + (error.error?.message || error.message || 'Unknown error'), 'error');
      }
    });
  }

  /**
   * Update promotion target for detail modal
   */
  updateDetailPromotionTarget(promotionId: string): void {
    // Only update target if scope is Category, Product, or Brand
    const scope = this.selectedPromotion?.scope;
    if (scope !== 'Category' && scope !== 'Product' && scope !== 'Brand') {
      // If scope changed to non-target, try to delete existing target
      this.http.delete(`${environment.apiUrl}/promotion-targets/${promotionId}`).subscribe({
        next: () => {
          console.log('✅ Deleted promotion target (scope changed to non-target)');
        },
        error: (error: any) => {
          // Target doesn't exist, that's okay
          console.log('No target to delete');
        }
      });
      return;
    }

    // Check if targets are selected
    if (!this.detailSelectedTargets || this.detailSelectedTargets.length === 0) {
      console.log('⚠️ No targets selected, skipping promotion_target update');
      return;
    }

    // Map scope to target_type
    let targetType: 'Category' | 'Subcategory' | 'Brand' | 'Product';
    if (scope === 'Category') {
      targetType = this.detailTargetType === 'Subcategory' ? 'Subcategory' : 'Category';
    } else if (scope === 'Brand') {
      targetType = 'Brand';
    } else if (scope === 'Product') {
      targetType = 'Product';
    } else {
      return;
    }

    // Prepare target data
    const targetData = {
      promotion_id: promotionId,
      target_type: targetType,
      target_ref: this.detailSelectedTargets
    };

    // Update promotion target
    this.http.put<any>(`${environment.apiUrl}/promotion-targets/${promotionId}`, targetData).subscribe({
      next: (response: any) => {
        if (response.success) {
          console.log('✅ Updated promotion target:', response.data);
        } else {
          console.error('❌ Error updating promotion target:', response.message);
        }
      },
      error: (error: any) => {
        // If target doesn't exist, create it
        if (error.status === 404) {
          this.http.post<any>(`${environment.apiUrl}/promotion-targets`, targetData).subscribe({
            next: (response: any) => {
              if (response.success) {
                console.log('✅ Created promotion target:', response.data);
              } else {
                console.error('❌ Error creating promotion target:', response.message);
              }
            },
            error: (createError) => {
              console.error('❌ Error creating promotion target:', createError);
            }
          });
        } else {
          console.error('❌ Error updating promotion target:', error);
        }
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'active':
        return 'status-active';
      case 'upcoming':
        return 'status-upcoming';
      case 'expired':
        return 'status-expired';
      default:
        return '';
    }
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'active':
        return 'Đang diễn ra';
      case 'upcoming':
        return 'Sắp diễn ra';
      case 'expired':
        return 'Đã kết thúc';
      default:
        return status;
    }
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  }
}


