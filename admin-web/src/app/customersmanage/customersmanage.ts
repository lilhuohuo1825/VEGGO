import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { Router, NavigationEnd } from '@angular/router';
import { ApiService } from '../services/api.service';
import { Subscription, filter } from 'rxjs';

interface UserJSON {
  _id: any;
  user_id?: number;
  CustomerID?: string;
  FullName?: string;
  Phone?: string;
  Email?: string;
  Address?: string;
  RegisterDate?: string | { $date: string };
  customer_type?: string;
  CustomerTiering?: string;
  TotalSpent?: number;
  full_name?: string;
  phone?: string;
  email?: string;
  address?: string;
  register_date?: string;
  password?: string;
}

interface Customer {
  id: string;
  joinDate: string;
  name: string;
  phone: string;
  email: string;
  address: string;
  memberTier: string;
  totalOrders: string;
  selected: boolean;
  group?: string;
}

@Component({
  selector: 'app-customersmanage',
  imports: [CommonModule, FormsModule],
  templateUrl: './customersmanage.html',
  styleUrl: './customersmanage.css',
  standalone: true,
})
export class CustomersManage implements OnInit, OnDestroy {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiService = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  private routerSubscription?: Subscription;
  private previousUrl: string = '';
  private addressTree: any = null; // Cache for tree_complete.json data

  customers: Customer[] = [];
  allCustomers: Customer[] = []; // Keep original data for search/filter
  customerIdMap = new Map<string, string>(); // Map customer.id (KH format) to original CustomerID (CUS format)
  isLoading = false;
  loadError = '';

  currentTab: string = 'customers';
  cerRequests: any[] = [];

  selectedCount = 0;
  selectAll = false;

  // Filter states
  currentFilter: string = 'all';
  showFilterDialog = false;
  showFilterDropdown = false;
  showSortDropdown = false;
  showFilterModal = false;
  showSortModal = false;
  filterOptions = {
    memberTier: 'all',
    group: 'all',
    minSpending: 0,
    maxSpending: 999999999,
  };

  // Temporary filter options for modal
  tempFilterOptions = {
    memberTier: 'all',
    group: 'all',
    minSpending: 0,
    maxSpending: 999999999,
  };

  // Group modal
  showGroupModal = false;
  newGroupName = '';
  allGroupNames: string[] = [];

  // Customer detail modal
  showCustomerDetailModal = false;
  currentCustomer: Customer | null = null;
  editingCustomer = false;

  // Sort state
  currentSortField: keyof Customer = 'joinDate';
  currentSortOrder: 'asc' | 'desc' = 'desc';

  // Popup notification
  showPopup: boolean = false;
  popupMessage: string = '';
  popupType: 'success' | 'error' | 'info' = 'success';

  // Confirmation dialog
  showConfirmDialog: boolean = false;
  confirmMessage: string = '';
  confirmCallback: (() => void) | null = null;

  // Sort options for modal
  sortOptions = [
    { field: 'name' as keyof Customer, label: 'Tên khách hàng', icon: '👤' },
    { field: 'joinDate' as keyof Customer, label: 'Ngày tham gia', icon: '📅' },
    { field: 'totalOrders' as keyof Customer, label: 'Tổng đơn hàng', icon: '📦' },
    { field: 'memberTier' as keyof Customer, label: 'Hạng thành viên', icon: '⭐' },
  ];

  // Batch edit modal
  showBatchEditModal = false;
  batchEditOption: string = '';
  selectedTier: string = '';
  selectedGroupForBatch: string = '';

  // CERTIFICATE METHODS
  loadCerRequests() {
    this.apiService.getCertificateRequests().subscribe((data) => {
      this.cerRequests = data || [];
      this.cdr.detectChanges();
    });
  }

  approveRequest(req: any) {
    this.apiService.approveCertificateRequest(req.id, req.userId, req.requestedCer).subscribe(() => {
      this.popupMessage = 'Đã duyệt Chứng nhận thành công';
      this.popupType = 'success';
      this.showPopup = true;
      this.loadCerRequests();
      this.loadCustomers(); // Reload to get updated users
      setTimeout(() => this.showPopup = false, 3000);
    });
  }

  rejectRequest(req: any) {
    this.apiService.rejectCertificateRequest(req.id).subscribe(() => {
      this.popupMessage = 'Đã từ chối yêu cầu';
      this.popupType = 'info';
      this.showPopup = true;
      this.loadCerRequests();
      setTimeout(() => this.showPopup = false, 3000);
    });
  }

  evaluateCertificateRequests() {
    this.apiService.evaluateCertificateRequests().subscribe({
      next: (result) => {
        this.popupMessage = `Đã quét ${result?.evaluated || 0} khách hàng, tạo ${result?.created || 0} yêu cầu mới`;
        this.popupType = 'success';
        this.showPopup = true;
        this.loadCerRequests();
        this.loadCustomers();
        setTimeout(() => this.showPopup = false, 3000);
      },
      error: (error: any) => {
        this.popupMessage = 'Lỗi khi quét lại chứng nhận: ' + (error.error?.message || error.message);
        this.popupType = 'error';
        this.showPopup = true;
        setTimeout(() => this.showPopup = false, 4000);
      }
    });
  }

  formatCertificateDate(value: any): string {
    if (!value) return '';
    if (value.toDate && typeof value.toDate === 'function') {
      value = value.toDate();
    } else if (value.$date) {
      value = value.$date;
    }

    const date = new Date(value);
    if (isNaN(date.getTime())) return String(value);
    return date.toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  ngOnInit(): void {
    // Load address tree data for Vietnamese name formatting
    this.loadAddressTree();
    this.loadCustomers();
    this.extractAllGroupNames();
    this.loadCerRequests();

    // Track previous URL and reload customers when navigating back from customer detail
    this.previousUrl = this.router.url;

    // Reload customers when navigating back from customer detail page
    this.routerSubscription = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        const currentUrl = event.url;

        // Reload customers when navigating to customers page from customer detail page
        if (
          (currentUrl === '/customers' || currentUrl.startsWith('/customers')) &&
          this.previousUrl?.includes('/customers/') &&
          !this.previousUrl.includes('/customers/new')
        ) {
          // Reload customers to get updated data
          console.log('🔄 Reloading customers after navigation from customer detail...');
          setTimeout(() => {
            this.loadCustomers();
          }, 100);
        }

        this.previousUrl = currentUrl;
      });
  }

  ngOnDestroy(): void {
    // Clean up subscription
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
  }

  /**
   * Load customers from MongoDB via API
   */
  loadCustomers(): void {
    this.isLoading = true;
    this.loadError = '';

    console.log('🔄 Loading customers from MongoDB...');

    // Load users from MongoDB API
    this.apiService.getUsers().subscribe({
      next: (data) => {
        console.log('✅ SUCCESS: Loaded customers from MongoDB!');
        console.log(`📊 Total customers: ${data.length}`);
        console.log('🗄️ Data source: MongoDB');

        // Map MongoDB data to Customer interface and build CustomerID mapping
        this.customerIdMap.clear();
        this.allCustomers = data.map((user) => {
          const customer = this.mapUserToCustomer(user);
          // Store mapping: customer.id (CUS format) -> original CustomerID (CUS format) - same format now
          if (user.CustomerID) {
            this.customerIdMap.set(customer.id, user.CustomerID);
          }
          return customer;
        });
        this.customers = [...this.allCustomers];

        // Load orders to calculate total spending from MongoDB
        this.loadOrdersForCustomers();

        this.isLoading = false;
        console.log('✅ Customers displayed:', this.customers.length);
      },
      error: (error: any) => {
        console.error('❌ ERROR loading from MongoDB:', error);
        console.error('❌ Error details:', {
          status: error.status,
          statusText: error.statusText,
          message: error.message,
          error: error.error,
        });

        this.loadError = '❌ Không thể tải dữ liệu từ MongoDB. Vui lòng kiểm tra kết nối database.';
        this.isLoading = false;

        // Set empty arrays - NO JSON fallback
        this.allCustomers = [];
        this.customers = [];
        this.cdr.detectChanges();

        console.error('❌ Failed to load customers from MongoDB - NO fallback to JSON');
      },
    });
  }

  /**
   * Load orders for customers to calculate total spending from MongoDB
   */
  private loadOrdersForCustomers(): void {
    console.log('🔄 Loading orders and users data from MongoDB...');

    // Load both orders and users data from MongoDB
    forkJoin({
      orders: this.apiService.getOrders(),
      users: this.apiService.getUsers(),
    }).subscribe({
      next: ({ orders, users }) => {
        console.log('✅ SUCCESS: Loaded orders and users data from MongoDB!');
        console.log(`📊 Total orders: ${orders.length}`);
        console.log(`📊 Total users: ${users.length}`);
        this.processOrders(orders, users);
      },
      error: (error: any) => {
        console.error('❌ ERROR loading data from MongoDB:', error);
        console.error('⚠️ Cannot calculate customer spending without orders data');
        // Set total orders to 0 if orders not found
        this.allCustomers.forEach((customer) => {
          customer.totalOrders = '0đ';
        });
        this.customers = [...this.allCustomers];
      },
    });
  }

  /**
   * Process orders data to calculate customer spending - only count completed (paid) orders
   */
  private processOrders(orders: any[], users: any[]): void {
    // Build customer lookup maps by phone and email for fallback matching
    const customerByPhone = new Map<string, string>(); // phone -> CustomerID
    const customerByEmail = new Map<string, string>(); // email -> CustomerID

    users.forEach((user) => {
      if (user.CustomerID) {
        if (user.Phone) {
          customerByPhone.set(user.Phone, user.CustomerID);
        }
        if (user.Email) {
          customerByEmail.set(user.Email, user.CustomerID);
        }
      }
    });

    // Calculate total spending for each customer - only count orders with status "completed" (paid orders)
    const customerTotals = new Map<string, number>();

    orders.forEach((order) => {
      // Only count orders that have been paid (status = "completed" or "delivered" - both are considered completed)
      if (order.status !== 'completed' && order.status !== 'delivered') {
        return;
      }

      // Get CustomerID from order
      let customerId = order.CustomerID || order.user_id?.toString() || '';
      const totalAmount = order.totalAmount || order.total_amount || 0;

      // If CustomerID doesn't exist in our customer list, try to match by phone or email
      if (!customerId || !Array.from(this.customerIdMap.values()).includes(customerId)) {
        const orderPhone = order.shippingInfo?.phone || order.phone || '';
        const orderEmail = order.shippingInfo?.email || order.email || '';

        // Try to find matching CustomerID by phone
        if (orderPhone && customerByPhone.has(orderPhone)) {
          customerId = customerByPhone.get(orderPhone)!;
          console.log(`🔍 Matched order by phone ${orderPhone} → ${customerId}`);
        }
        // Try to find matching CustomerID by email
        else if (orderEmail && customerByEmail.has(orderEmail)) {
          customerId = customerByEmail.get(orderEmail)!;
          console.log(`🔍 Matched order by email ${orderEmail} → ${customerId}`);
        }
      }

      if (customerId) {
        const current = customerTotals.get(customerId) || 0;
        customerTotals.set(customerId, current + totalAmount);
      }
    });

    console.log('💰 Customer totals calculated (completed orders only):', customerTotals);

    // Update customer total orders
    this.allCustomers.forEach((customer) => {
      // Get the original CustomerID from the mapping (CUS format)
      const originalCustomerID = this.customerIdMap.get(customer.id);

      // Find matching total from orders using original CustomerID
      const matchedTotal = originalCustomerID ? customerTotals.get(originalCustomerID) || 0 : 0;

      // Update total orders with matched value
      customer.totalOrders = this.formatCurrency(matchedTotal);
    });

    // Update displayed customers
    this.customers = [...this.allCustomers];
    console.log('✅ Customer totals updated');
  }

  /**
   * Format currency
   */
  private formatCurrency(amount: number): string {
    return amount.toLocaleString('vi-VN') + 'đ';
  }

  /**
   * Map UserJSON to Customer - supports both old format and MongoDB format
   */
  private mapUserToCustomer(user: UserJSON): Customer {
    // Handle date - support both old format and MongoDB format
    // Format output: dd-mm-yyyy
    let formattedDate = '';
    const registerDate = user.RegisterDate || user.register_date;

    if (registerDate) {
      let dateObj: Date | null = null;

      // Try to parse date from various formats
      if (typeof registerDate === 'string') {
        // Try parsing as ISO string first
        dateObj = new Date(registerDate);

        // If invalid, try parsing as YYYY-MM-DD format
        if (isNaN(dateObj.getTime()) && registerDate.includes('-')) {
          const dateParts = registerDate.split('-');
          if (dateParts.length === 3) {
            // Assume format: YYYY-MM-DD
            dateObj = new Date(
              parseInt(dateParts[0]),
              parseInt(dateParts[1]) - 1,
              parseInt(dateParts[2])
            );
          }
        }

        // If still invalid, try parsing format like "02T09:25:26.738Z/11/2025"
        if (isNaN(dateObj.getTime()) && registerDate.includes('T') && registerDate.includes('/')) {
          // Extract date parts from format like "02T09:25:26.738Z/11/2025"
          const parts = registerDate.split('/');
          if (parts.length === 3) {
            const dayPart = parts[0].split('T')[0];
            const month = parts[1];
            const year = parts[2];
            dateObj = new Date(parseInt(year), parseInt(month) - 1, parseInt(dayPart));
          }
        }
      } else if (registerDate.$date) {
        // MongoDB format: { $date: "ISO string" }
        dateObj = new Date(registerDate.$date);
      } else if (registerDate instanceof Date) {
        // Already a Date object
        dateObj = registerDate;
      }

      // Format date as dd-mm-yyyy
      if (dateObj && !isNaN(dateObj.getTime())) {
        const day = String(dateObj.getDate()).padStart(2, '0');
        const month = String(dateObj.getMonth() + 1).padStart(2, '0');
        const year = dateObj.getFullYear();
        formattedDate = `${day}-${month}-${year}`;
      }
    }

    if (!formattedDate) {
      formattedDate = 'N/A';
    }

    // Map customer_type or CustomerTiering to memberTier
    let memberTier = 'bronze';
    const tiering = user.CustomerTiering || user.customer_type || '';

    if (tiering === 'Bạch Kim' || tiering === 'Platinum') {
      memberTier = 'platinum';
    } else if (tiering === 'VIP' || tiering === 'Vàng' || tiering === 'Gold') {
      memberTier = 'gold';
    } else if (tiering === 'Premium' || tiering === 'Bạc' || tiering === 'Silver') {
      memberTier = 'silver';
    } else if (tiering === 'Regular' || tiering === 'Đồng' || tiering === 'Bronze') {
      memberTier = 'bronze';
    }

    // Get customer ID - use CUS format directly
    let customerId = '';
    if (user.CustomerID) {
      // Use CustomerID directly (CUS format)
      customerId = user.CustomerID;
    } else if (user.user_id) {
      customerId = 'CUS' + String(user.user_id).padStart(6, '0');
    } else {
      customerId = 'CUS' + String(user._id).substring(0, 6).padStart(6, '0');
    }

    // Get customer name - support both formats
    const name = user.FullName || user.full_name || '(Chưa cập nhật)';
    const phone = user.Phone || user.phone || '';
    const email = user.Email || user.email || '';
    const rawAddress = user.Address || user.address || '(Chưa cập nhật)';

    // Format address to Vietnamese with proper diacritics and capitalization
    const address = this.formatAddressToVietnamese(rawAddress);

    // Calculate total orders from TotalSpent if available
    const totalSpent = user.TotalSpent || 0;
    const totalOrdersDisplay = totalSpent > 0 ? this.formatCurrency(totalSpent) : '0đ';

    return {
      id: customerId,
      joinDate: formattedDate,
      name: name,
      phone: phone,
      email: email,
      address: address,
      memberTier: memberTier,
      totalOrders: totalOrdersDisplay,
      selected: false,
      group: undefined,
    };
  }

  /**
   * Load address tree data from tree_complete.json for Vietnamese name formatting
   */
  private loadAddressTree(): void {
    this.http.get<any>('data/address/tree_complete.json').subscribe({
      next: (treeData: any) => {
        this.addressTree = treeData;
        console.log('✅ [CustomersManage] Loaded address tree data for Vietnamese name formatting');
      },
      error: (error: any) => {
        console.error('❌ [CustomersManage] Error loading address tree:', error);
      },
    });
  }

  /**
   * Format address string to Vietnamese with proper diacritics and capitalization
   * Input format: "39 Nguyễn Huệ, p1, q2, hcm" or "143 Nguyễn Thị Minh Khai, 3, soc-trang, soc-trang"
   * Output format: "39 Nguyễn Huệ, Phường 1, Quận 2, Thành phố Hồ Chí Minh"
   */
  private formatAddressToVietnamese(addressString: string): string {
    if (!addressString || addressString === '(Chưa cập nhật)') {
      return addressString;
    }

    // Split address by comma
    const parts = addressString
      .split(',')
      .map((part) => part.trim())
      .filter((part) => part);

    if (parts.length === 0) {
      return addressString;
    }

    // First part is usually the street address (keep as is)
    const formattedParts: string[] = [parts[0]];

    // Process remaining parts (ward, district, city)
    for (let i = 1; i < parts.length; i++) {
      const part = parts[i];
      let formattedPart = part;

      // Try to identify and format each part
      if (i === 1 && parts.length >= 2) {
        // This is likely ward
        formattedPart = this.formatWardName(part);
      } else if (i === 2 && parts.length >= 3) {
        // This is likely district
        formattedPart = this.formatDistrictName(part);
      } else if (i === 3 || (i === 2 && parts.length === 3)) {
        // This is likely city/province (last part)
        formattedPart = this.formatCityName(part);
      } else {
        // Try to format as Vietnamese name
        formattedPart = this.formatVietnameseName(part);
      }

      formattedParts.push(formattedPart);
    }

    return formattedParts.join(', ');
  }

  /**
   * Format city/province name
   */
  private formatCityName(cityIdOrSlug: string): string {
    if (!this.addressTree || !cityIdOrSlug) return this.formatVietnameseName(cityIdOrSlug);

    // Try to find by code first
    if (this.addressTree[cityIdOrSlug]) {
      return (
        this.addressTree[cityIdOrSlug].name_with_type ||
        this.addressTree[cityIdOrSlug].name ||
        cityIdOrSlug
      );
    }

    // Try to find by slug
    for (const province of Object.values(this.addressTree) as any[]) {
      if (province.slug === cityIdOrSlug || province.code === cityIdOrSlug) {
        return province.name_with_type || province.name || cityIdOrSlug;
      }
    }

    // Common mappings
    const cityMap: { [key: string]: string } = {
      hcm: 'Thành phố Hồ Chí Minh',
      hn: 'Hà Nội',
      'ha-noi': 'Hà Nội',
      dn: 'Đà Nẵng',
      'da-nang': 'Đà Nẵng',
      'soc-trang': 'Sóc Trăng',
      'can-tho': 'Cần Thơ',
    };

    if (cityMap[cityIdOrSlug.toLowerCase()]) {
      return cityMap[cityIdOrSlug.toLowerCase()];
    }

    return this.formatVietnameseName(cityIdOrSlug);
  }

  /**
   * Format district name
   */
  private formatDistrictName(districtIdOrSlug: string): string {
    if (!districtIdOrSlug) return '';

    // Common mappings
    const districtMap: { [key: string]: string } = {
      q1: 'Quận 1',
      q2: 'Quận 2',
      q3: 'Quận 3',
      'quan-1': 'Quận 1',
      'quan-2': 'Quận 2',
      'quan-3': 'Quận 3',
      'dong-mai': 'Đông Mai',
      'ha-dong': 'Hà Đông',
    };

    if (districtMap[districtIdOrSlug.toLowerCase()]) {
      return districtMap[districtIdOrSlug.toLowerCase()];
    }

    // Try to format from slug
    if (districtIdOrSlug.includes('-')) {
      return this.formatVietnameseName(districtIdOrSlug);
    }

    return districtIdOrSlug;
  }

  /**
   * Format ward name
   */
  private formatWardName(wardIdOrSlug: string): string {
    if (!wardIdOrSlug) return '';

    // Common mappings
    const wardMap: { [key: string]: string } = {
      p1: 'Phường 1',
      p2: 'Phường 2',
      p3: 'Phường 3',
      'phuong-1': 'Phường 1',
      'phuong-2': 'Phường 2',
      'phuong-3': 'Phường 3',
    };

    if (wardMap[wardIdOrSlug.toLowerCase()]) {
      return wardMap[wardIdOrSlug.toLowerCase()];
    }

    // If it's just a number, assume it's a ward number
    if (/^\d+$/.test(wardIdOrSlug)) {
      return `Phường ${wardIdOrSlug}`;
    }

    // Try to format from slug
    if (wardIdOrSlug.includes('-')) {
      return this.formatVietnameseName(wardIdOrSlug);
    }

    return wardIdOrSlug;
  }

  /**
   * Format Vietnamese name from slug (e.g., "soc-trang" -> "Sóc Trăng")
   */
  private formatVietnameseName(slug: string): string {
    if (!slug) return '';

    // If already looks like Vietnamese (has Vietnamese characters), return as is but capitalize
    if (/[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]/i.test(slug)) {
      return this.capitalizeVietnamese(slug);
    }

    // Common Vietnamese location name mappings
    const nameMap: { [key: string]: string } = {
      'soc-trang': 'Sóc Trăng',
      'can-tho': 'Cần Thơ',
      'phuong-1': 'Phường 1',
      'phuong-2': 'Phường 2',
      'phuong-3': 'Phường 3',
      'quan-1': 'Quận 1',
      'quan-2': 'Quận 2',
      'quan-3': 'Quận 3',
      'dong-mai': 'Đông Mai',
      'ha-dong': 'Hà Đông',
    };

    if (nameMap[slug.toLowerCase()]) {
      return nameMap[slug.toLowerCase()];
    }

    // If not in map, try to capitalize words
    return this.capitalizeVietnamese(slug);
  }

  /**
   * Capitalize Vietnamese text properly
   */
  private capitalizeVietnamese(text: string): string {
    if (!text) return '';

    // Split by common separators and capitalize each word
    return text
      .split(/[-_\s]+/)
      .map((word) => {
        if (!word) return '';
        // Capitalize first letter, keep rest lowercase
        return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
      })
      .join(' ');
  }

  /**
   * Load sample data as fallback
   */
  /**
   * REMOVED: loadSampleData() - No longer using JSON/sample data fallback
   * All customer data must be loaded from MongoDB only!
   */

  /**
   * Toggle select all customers
   */
  toggleSelectAll(): void {
    this.selectAll = !this.selectAll;
    this.customers.forEach((customer) => (customer.selected = this.selectAll));
    this.updateSelectedCount();
  }

  /**
   * Toggle individual customer selection
   */
  toggleCustomer(customer: any): void {
    customer.selected = !customer.selected;
    this.updateSelectedCount();
    this.selectAll = this.customers.every((c) => c.selected);
  }

  /**
   * Update selected count
   */
  updateSelectedCount(): void {
    this.selectedCount = this.customers.filter((c) => c.selected).length;
  }

  /**
   * Manage customer groups - Open group modal
   */
  manageGroups(): void {
    const selected = this.customers.filter((c) => c.selected);

    if (selected.length < 2) {
      return; // Do nothing if less than 2 customers selected
    }

    this.newGroupName = '';
    this.showGroupModal = true;
  }

  /**
   * Close group modal
   */
  closeGroupModal(): void {
    this.showGroupModal = false;
    this.newGroupName = '';
  }

  /**
   * Create group and assign to selected customers
   */
  createGroup(): void {
    if (!this.newGroupName || this.newGroupName.trim() === '') {
      return; // Do nothing if empty
    }

    const groupName = this.newGroupName.trim();
    const selected = this.customers.filter((c) => c.selected);

    selected.forEach((customer) => {
      customer.group = groupName;
      const allCustomer = this.allCustomers.find((c) => c.id === customer.id);
      if (allCustomer) {
        allCustomer.group = groupName;
      }
    });

    this.extractAllGroupNames();

    // Deselect all
    this.customers.forEach((c) => (c.selected = false));
    this.selectAll = false;
    this.updateSelectedCount();

    this.closeGroupModal();
  }

  /**
   * Extract all unique group names from customers
   */
  extractAllGroupNames(): void {
    const groupsSet = new Set<string>();
    this.allCustomers.forEach((customer) => {
      if (customer.group) {
        groupsSet.add(customer.group);
      }
    });
    this.allGroupNames = Array.from(groupsSet).sort();
  }

  /**
   * Edit selected customers - Navigate to detail page with edit mode
   * Only allows editing when exactly 1 customer is selected
   */
  editCustomers(): void {
    const selected = this.customers.filter((c) => c.selected);

    // Only allow editing when exactly 1 customer is selected
    if (selected.length !== 1) {
      return; // Do nothing if not exactly 1 selection
    }

    // Navigate to detail page with edit mode
    // Use both state and queryParams for reliability
    this.router.navigate(['/customers', selected[0].id], {
      queryParams: { edit: 'true' },
      state: { editMode: true },
    });
  }

  /**
   * Close batch edit modal
   */
  closeBatchEditModal(): void {
    this.showBatchEditModal = false;
    this.batchEditOption = '';
  }

  /**
   * Apply batch edit
   */
  applyBatchEdit(): void {
    const selected = this.customers.filter((c) => c.selected);

    if (this.batchEditOption === 'tier' && this.selectedTier) {
      selected.forEach((customer) => {
        customer.memberTier = this.selectedTier;
        const allCustomer = this.allCustomers.find((c) => c.id === customer.id);
        if (allCustomer) allCustomer.memberTier = this.selectedTier;
      });
    } else if (this.batchEditOption === 'group' && this.selectedGroupForBatch) {
      selected.forEach((customer) => {
        customer.group = this.selectedGroupForBatch;
        const allCustomer = this.allCustomers.find((c) => c.id === customer.id);
        if (allCustomer) allCustomer.group = this.selectedGroupForBatch;
      });
      this.extractAllGroupNames();
    } else if (this.batchEditOption === 'removeGroup') {
      selected.forEach((customer) => {
        delete customer.group;
        const allCustomer = this.allCustomers.find((c) => c.id === customer.id);
        if (allCustomer) delete allCustomer.group;
      });
    }

    // Deselect all
    this.customers.forEach((c) => (c.selected = false));
    this.selectAll = false;
    this.updateSelectedCount();

    this.closeBatchEditModal();
  }

  /**
   * Delete selected customers
   */
  deleteCustomers(): void {
    // Check if any customers are selected
    const selected = this.customers.filter((c) => c.selected);

    if (selected.length === 0) {
      this.displayPopup('Vui lòng chọn khách hàng cần xóa', 'error');
      return;
    }

    // Show confirmation dialog
    this.showConfirmation(`Bạn có chắc chắn muốn xóa ${selected.length} khách hàng?`, () => {
      // Delete customers via API
      const deletePromises = selected.map((customer) => {
        const customerId = customer.id || '';
        if (!customerId) {
          console.warn('⚠️ Customer missing ID:', customer);
          return Promise.resolve(null);
        }
        return this.apiService.deleteCustomer(customerId).toPromise();
      });

      Promise.all(deletePromises)
        .then((results) => {
          const successCount = results.filter((r) => r !== null).length;
          console.log(`✅ Deleted ${successCount} customers successfully`);

          // Reload customers from MongoDB to get updated list
          this.loadCustomers();

          this.selectedCount = 0;
          this.selectAll = false;
          this.displayPopup(`Đã xóa ${successCount} khách hàng thành công`, 'success');
        })
        .catch((error) => {
          console.error('❌ Error deleting customers:', error);
          this.displayPopup(
            'Lỗi khi xóa khách hàng: ' + (error.error?.message || error.message),
            'error'
          );
          // Still reload to sync with server
          this.loadCustomers();
        });
    });
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
   * Close popup
   */
  closePopup(): void {
    this.showPopup = false;
    this.popupMessage = '';
  }

  /**
   * Show confirmation dialog
   */
  showConfirmation(message: string, callback: () => void): void {
    this.confirmMessage = message;
    this.confirmCallback = callback;
    this.showConfirmDialog = true;

    // Force change detection to ensure popup shows
    if (this.cdr) {
      this.cdr.detectChanges();
    }
  }

  /**
   * Confirm action
   */
  confirmDelete(): void {
    if (this.confirmCallback) {
      this.confirmCallback();
      this.confirmCallback = null;
    }
    this.showConfirmDialog = false;
  }

  /**
   * Cancel action
   */
  cancelDelete(): void {
    this.showConfirmDialog = false;
    this.confirmCallback = null;
    this.confirmMessage = '';
  }

  /**
   * Open filter dialog
   */
  openFilter(): void {
    const filterChoice = prompt(
      'Chọn bộ lọc:\n\n' +
        '1 - Lọc theo hạng thành viên\n' +
        '2 - Lọc theo tổng chi tiêu\n' +
        '3 - Lọc theo nhóm\n' +
        '4 - Xóa tất cả bộ lọc\n\n' +
        'Nhập số (1-4):'
    );

    if (filterChoice === '1') {
      this.filterByMemberTier();
    } else if (filterChoice === '2') {
      this.filterBySpending();
    } else if (filterChoice === '3') {
      this.filterByGroup();
    } else if (filterChoice === '4') {
      this.clearFilters();
    }
  }

  /**
   * Filter by member tier
   */
  private filterByMemberTier(): void {
    const tier = prompt('Chọn hạng thành viên:\n1 - Vàng\n2 - Bạc\n3 - Đồng\n\nNhập số (1-3):');

    let filterTier = '';
    if (tier === '1') filterTier = 'gold';
    else if (tier === '2') filterTier = 'silver';
    else if (tier === '3') filterTier = 'bronze';

    if (filterTier) {
      this.customers = this.allCustomers.filter((c) => c.memberTier === filterTier);
      this.currentFilter = `memberTier:${filterTier}`;
      alert(
        `Đã lọc ${this.customers.length} khách hàng hạng ${this.getMemberTierLabel(filterTier)}.`
      );
    }
  }

  /**
   * Filter by spending
   */
  private filterBySpending(): void {
    const minStr = prompt('Nhập tổng chi tiêu tối thiểu (VNĐ):\n(Ví dụ: 1000000 cho 1 triệu đồng)');
    const maxStr = prompt('Nhập tổng chi tiêu tối đa (VNĐ):\n(Để trống nếu không giới hạn)');

    const min = minStr ? parseFloat(minStr) : 0;
    const max = maxStr ? parseFloat(maxStr) : 999999999;

    if (!isNaN(min) && !isNaN(max)) {
      this.customers = this.allCustomers.filter((customer) => {
        const total = parseFloat(customer.totalOrders.replace(/[.đ]/g, ''));
        return total >= min && total <= max;
      });

      this.currentFilter = `spending:${min}-${max}`;
      alert(
        `Đã lọc ${this.customers.length} khách hàng với chi tiêu từ ${min.toLocaleString(
          'vi-VN'
        )}đ đến ${max.toLocaleString('vi-VN')}đ.`
      );
    }
  }

  /**
   * Filter by group
   */
  private filterByGroup(): void {
    // Get unique groups from customers
    const groups = Array.from(
      new Set(this.allCustomers.filter((c) => c.group).map((c) => c.group))
    );

    if (groups.length === 0) {
      alert('Chưa có khách hàng nào được phân nhóm.');
      return;
    }

    const groupList = groups.map((g, i) => `${i + 1} - ${g}`).join('\n');
    const choice = prompt(`Chọn nhóm:\n\n${groupList}\n\nNhập số (1-${groups.length}):`);

    const index = choice ? parseInt(choice) - 1 : -1;
    if (index >= 0 && index < groups.length) {
      const selectedGroup = groups[index];
      this.customers = this.allCustomers.filter((c) => c.group === selectedGroup);
      this.currentFilter = `group:${selectedGroup}`;
      alert(`Đã lọc ${this.customers.length} khách hàng trong nhóm "${selectedGroup}".`);
    }
  }

  /**
   * Clear all filters
   */
  private clearFilters(): void {
    this.customers = [...this.allCustomers];
    this.currentFilter = 'all';
    this.filterOptions = {
      memberTier: 'all',
      group: 'all',
      minSpending: 0,
      maxSpending: 999999999,
    };
    alert('Đã xóa tất cả bộ lọc.');
  }

  /**
   * Search customers
   */
  searchCustomers(event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    console.log('Search:', query);

    if (!query || query.trim() === '') {
      // Reset to all customers if search is empty
      this.customers = [...this.allCustomers];
    } else {
      // Search customers by query
      const searchTerm = query.toLowerCase().trim();
      this.customers = this.allCustomers.filter((customer) => {
        return (
          customer.name.toLowerCase().includes(searchTerm) ||
          customer.id.toLowerCase().includes(searchTerm) ||
          customer.phone.includes(searchTerm) ||
          customer.email.toLowerCase().includes(searchTerm) ||
          customer.address.toLowerCase().includes(searchTerm)
        );
      });
      console.log(`Search results: ${this.customers.length} customers`);
    }

    // Reset selection
    this.selectedCount = 0;
    this.selectAll = false;
  }

  /**
   * Get member tier label
   */
  getMemberTierLabel(tier: string): string {
    const labels: any = {
      platinum: 'Bạch Kim',
      gold: 'Vàng',
      silver: 'Bạc',
      bronze: 'Đồng',
    };
    return labels[tier] || tier;
  }

  /**
   * Get member tier class
   */
  getMemberTierClass(tier: string): string {
    return `tier-${tier}`;
  }

  /**
   * View customer detail
   */
  viewCustomerDetail(customer: Customer): void {
    // Navigate to customer detail page with customer ID
    this.router.navigate(['/customers', customer.id]);
  }

  /**
   * Toggle filter dropdown
   */
  toggleFilterDropdown(event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.showFilterDropdown = !this.showFilterDropdown;
    if (this.showFilterDropdown) {
      this.showSortDropdown = false;
    }
  }

  /**
   * Toggle sort dropdown
   */
  toggleSortDropdown(event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.showSortDropdown = !this.showSortDropdown;
    if (this.showSortDropdown) {
      this.showFilterDropdown = false;
    }
  }

  /**
   * Close all dropdowns when clicking outside
   */
  closeDropdowns(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.dropdown-container')) {
      this.showFilterDropdown = false;
      this.showSortDropdown = false;
    }
  }

  /**
   * Open filter modal
   */
  openFilterModal(): void {
    this.showFilterModal = true;
  }

  /**
   * Close filter modal
   */
  closeFilterModal(): void {
    this.showFilterModal = false;
  }

  /**
   * Apply filters from modal
   */
  applyFilterModal(): void {
    this.applyAllFilters();
    this.closeFilterModal();
  }

  /**
   * Reset filters
   */
  resetFilters(): void {
    this.filterOptions = {
      memberTier: 'all',
      group: 'all',
      minSpending: 0,
      maxSpending: 999999999,
    };
    this.applyAllFilters();
  }

  /**
   * Open sort modal
   */
  openSortModal(): void {
    this.showSortModal = true;
  }

  /**
   * Close sort modal
   */
  closeSortModal(): void {
    this.showSortModal = false;
  }

  /**
   * Apply sort from modal
   */
  applySortModal(field: keyof Customer, order: 'asc' | 'desc'): void {
    this.currentSortField = field;
    this.currentSortOrder = order;
    this.applyAllFilters();
    this.closeSortModal();
  }

  /**
   * Set member tier filter (radio-like behavior)
   */
  toggleMemberTierFilter(tier: string): void {
    // Simply set the filter, don't toggle
    console.log('Filter by member tier:', tier);
    this.filterOptions.memberTier = tier;
    this.applyAllFilters();
    console.log('Filtered customers:', this.customers.length);
  }

  /**
   * Set group filter (radio-like behavior)
   */
  toggleGroupFilterDropdown(group: string): void {
    // Simply set the filter, don't toggle
    this.filterOptions.group = group;
    this.applyAllFilters();
  }

  /**
   * Apply all filters
   */
  private applyAllFilters(): void {
    let filtered = [...this.allCustomers];
    console.log('Total customers before filter:', filtered.length);

    // Filter by member tier
    if (this.filterOptions.memberTier !== 'all') {
      console.log('Filtering by memberTier:', this.filterOptions.memberTier);
      filtered = filtered.filter((c) => {
        console.log(`Customer ${c.id} has memberTier: ${c.memberTier}`);
        return c.memberTier === this.filterOptions.memberTier;
      });
      console.log('After memberTier filter:', filtered.length);
    }

    // Filter by group
    if (this.filterOptions.group !== 'all') {
      filtered = filtered.filter((c) => c.group === this.filterOptions.group);
    }

    // Filter by spending
    if (this.filterOptions.minSpending > 0 || this.filterOptions.maxSpending < 999999999) {
      filtered = filtered.filter((customer) => {
        const total = parseFloat(customer.totalOrders.replace(/[.đ]/g, ''));
        return total >= this.filterOptions.minSpending && total <= this.filterOptions.maxSpending;
      });
    }

    // Apply sort
    if (this.currentSortField) {
      filtered = this.sortCustomersByField(filtered, this.currentSortField, this.currentSortOrder);
    }

    this.customers = filtered;
    console.log('Final filtered customers:', this.customers.length);
  }

  /**
   * Sort customers by field
   */
  private sortCustomersByField(
    customers: Customer[],
    field: keyof Customer,
    order: 'asc' | 'desc' = 'asc'
  ): Customer[] {
    const sorted = [...customers].sort((a, b) => {
      let aVal: any = a[field];
      let bVal: any = b[field];

      // Handle string comparison
      if (typeof aVal === 'string') {
        aVal = aVal.toLowerCase();
        bVal = bVal.toLowerCase();
      }

      if (aVal < bVal) return order === 'asc' ? -1 : 1;
      if (aVal > bVal) return order === 'asc' ? 1 : -1;
      return 0;
    });

    return sorted;
  }

  /**
   * Sort by field (from dropdown)
   */
  sortBy(field: keyof Customer): void {
    if (this.currentSortField === field) {
      this.currentSortOrder = this.currentSortOrder === 'asc' ? 'desc' : 'asc';
    } else {
      this.currentSortField = field;
      this.currentSortOrder = 'desc';
    }
    this.applyAllFilters();

    // Keep dropdown open after sorting
    // setTimeout(() => {
    //   this.showSortDropdown = false;
    // }, 150);
  }

  /**
   * Clear all filters (new version)
   */
  clearAllFilters(): void {
    this.filterOptions = {
      memberTier: 'all',
      group: 'all',
      minSpending: 0,
      maxSpending: 999999999,
    };
    this.currentFilter = 'all';
    this.applyAllFilters();
  }
}
