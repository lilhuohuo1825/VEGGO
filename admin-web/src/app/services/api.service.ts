import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  // ==================== USERS / CUSTOMERS ====================
  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/users`);
  }

  getUserById(id: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/users/id/${id}`);
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
    return this.http.get<any[]>(`${this.apiUrl}/orders`);
  }

  getOrderById(id: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/orders/id/${id}`);
  }

  getOrdersByUserId(userId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/orders/${userId}`);
  }

  createOrder(orderData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/orders`, orderData);
  }

  updateOrder(orderId: string, orderData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/orders/${orderId}`, orderData);
  }

  updateOrderStatus(orderId: string, status: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/orders/${orderId}`, { status });
  }

  deleteOrder(orderId: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/orders/${orderId}`);
  }

  // ==================== PRODUCTS ====================
  getProducts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/products?all=true`);
  }

  getProductById(id: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/products/${id}`);
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
    return this.http.get<any[]>(`${this.apiUrl}/warehouses`);
  }

  // ==================== PROMOTIONS ====================
  getPromotions(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/promotions`);
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
    return this.http.get<any[]>(`${this.apiUrl}/blogs`);
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
    return this.http.get<any[]>(`${this.apiUrl}/reviews`);
  }
  getTree(): Observable<any[]> { return of([]); }
  getCategories(): Observable<string[]> { return of(['Rau củ', 'Trái cây', 'Thực phẩm khô']); }
  getProductGroups(): Observable<any[]> { return of([]); }
  
  updateProductField(productId: string, field: string, value: any): Observable<any> { return of({ success: true }); }
  createProductGroup(groupName: string, skus: string[]): Observable<any> { return of({ success: true }); }
  updateProductGroup(sku: string, action: string, groupName: string): Observable<any> { return of({ success: true }); }
  getPromotionUsage(): Observable<Record<string, number>> {
    return this.http.get<any>(`${this.apiUrl}/promotion-usages`).pipe(
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
    return this.http.get<any>(`${this.apiUrl}/promotion-targets`).pipe(
      map((response) => Array.isArray(response) ? response : response?.data || []),
      catchError(() => of([]))
    );
  }

  getCertificateRequests(): Observable<any[]> {
    return this.http.get<any>(`${this.apiUrl}/certificates/requests`).pipe(
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
