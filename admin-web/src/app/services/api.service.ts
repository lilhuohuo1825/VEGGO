import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;
  private noCacheHeaders = new HttpHeaders({
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache',
  });

  private freshUrl(path: string): string {
    const separator = path.includes('?') ? '&' : '?';
    return `${this.apiUrl}${path}${separator}_=${Date.now()}`;
  }

  private getFresh<T>(path: string): Observable<T> {
    return this.http.get<T>(this.freshUrl(path), { headers: this.noCacheHeaders }).pipe(
      timeout(20000)
    );
  }

  // ==================== USERS / CUSTOMERS ====================
  getUsers(): Observable<any[]> {
    return this.getFresh<any[]>('/users');
  }

  getUserById(id: string): Observable<any> {
    return this.getFresh<any>(`/users/id/${id}`);
  }

  updateUser(id: string, userData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/users/${id}`, userData);
  }

  deleteUser(id: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/users/${id}`);
  }

  deleteCustomer(customerId: string): Observable<any> {
    return this.deleteUser(customerId);
  }

  // ==================== ORDERS ====================
  getOrders(): Observable<any[]> {
    return this.getFresh<any[]>('/orders');
  }

  getOrderById(id: string): Observable<any> {
    return this.getFresh<any>(`/orders/id/${id}`);
  }

  getOrdersByUserId(userId: string): Observable<any[]> {
    return this.getFresh<any[]>(`/orders/${userId}`);
  }

  getOrdersByCustomerId(customerId: string): Observable<any[]> {
    return this.getFresh<any>(`/orders/customer/${customerId}`).pipe(
      map((response) => Array.isArray(response) ? response : response?.orders || []),
      catchError(() => of([]))
    );
  }

  createOrder(orderData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/orders`, orderData);
  }

  updateOrder(orderId: string, orderData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/orders/${orderId}`, orderData);
  }

  updateOrderStatus(orderId: string, status: string, extraData: Record<string, any> = {}): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/orders/${orderId}/status`, {
      status,
      ...extraData,
    });
  }

  deleteOrder(orderId: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/orders/${orderId}`);
  }

  // ==================== PRODUCTS ====================
  getProducts(): Observable<any[]> {
    return this.getFresh<any>('/products?all=true&lite=true').pipe(
      map((response) => Array.isArray(response) ? response : response?.data || response?.products || [])
    );
  }

  getProductById(id: string): Observable<any> {
    return this.getFresh<any>(`/products/${id}`);
  }

  createProduct(productData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/products`, productData);
  }

  updateProduct(id: string, productData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/products/${id}`, productData);
  }

  deleteProduct(productId: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/products/${productId}`);
  }

  // ==================== WAREHOUSES ====================
  getWarehouses(): Observable<any[]> {
    return this.getFresh<any[]>('/warehouses');
  }

  // ==================== PROMOTIONS ====================
  getPromotions(): Observable<any[]> {
    return this.getFresh<any[]>('/promotions');
  }
  
  createPromotion(promoData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/promotions`, promoData);
  }

  updatePromotion(id: string, promoData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/promotions/${id}`, promoData);
  }

  deletePromotion(promotionId: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/promotions/${promotionId}`);
  }

  // ==================== BLOGS ====================
  getBlogs(): Observable<any[]> {
    return this.getFresh<any[]>('/blogs');
  }

  createBlog(blogData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/blogs`, blogData);
  }

  updateBlog(id: string, blogData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/blogs/${id}`, blogData);
  }

  deleteBlog(id: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/blogs/${id}`);
  }
  getReviews(): Observable<any[]> {
    return this.getFresh<any[]>('/reviews');
  }
  getTree(): Observable<any[]> { return of([]); }
  getCategories(): Observable<string[]> {
    return this.getFresh<any>('/products/metadata/categories').pipe(
      map((response) => {
        const categories = Array.isArray(response) ? response : response?.data || [];
        return categories.map((category: any) => category.name || category).filter(Boolean);
      }),
      catchError(() => of([]))
    );
  }
  getProductGroups(): Observable<string[]> {
    return this.getFresh<any>('/products/groups').pipe(
      map((response) => Array.isArray(response) ? response : response?.data || []),
      catchError(() => of([]))
    );
  }
  
  updateProductField(productId: string, field: string, value: any): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/products/${encodeURIComponent(productId)}/field`, {
      field,
      value
    });
  }
  createProductGroup(groupName: string, skus: string[]): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/products/groups`, {
      groupName,
      skus
    });
  }
  updateProductGroup(sku: string, action: string, groupName: string): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/products/${encodeURIComponent(sku)}/groups`, {
      action,
      groupName
    });
  }
  getPromotionUsage(): Observable<Record<string, number>> {
    return this.getFresh<any>('/promotion-usages').pipe(
      map((response) => {
        const rows = Array.isArray(response) ? response : response?.data || [];
        return rows.reduce((acc: Record<string, number>, usage: any) => {
          const promotionId = usage.promotion_id || usage.promotionId;
          if (!promotionId) return acc;
          acc[promotionId] = Array.isArray(usage.order_id)
            ? usage.order_id.length
            : Number(usage.usage_count || usage.usageCount || 0);
          return acc;
        }, {});
      }),
      catchError(() => of({}))
    );
  }

  getPromotionTargets(): Observable<any[]> {
    return this.getFresh<any>('/promotion-targets').pipe(
      map((response) => Array.isArray(response) ? response : response?.data || []),
      catchError(() => of([]))
    );
  }

  getCertificateRequests(): Observable<any[]> {
    return this.getFresh<any>('/certificates/requests').pipe(
      map((response) => Array.isArray(response) ? response : response?.data || []),
      catchError(() => of([]))
    );
  }

  evaluateCertificateRequests(): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/certificates/requests/evaluate-all`, {});
  }

  approveCertificateRequest(requestId: string, userId: string, newCer: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/certificates/requests/${requestId}/approve`, {
      userId,
      newCer
    });
  }

  rejectCertificateRequest(requestId: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/certificates/requests/${requestId}/reject`, {});
  }
}
