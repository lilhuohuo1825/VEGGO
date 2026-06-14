import { Injectable, inject } from '@angular/core';
import { Firestore, collection, doc, collectionData, docData, addDoc, updateDoc, deleteDoc, query, where, getDocs, getDoc } from '@angular/fire/firestore';
import { Timestamp } from 'firebase/firestore';
import { Observable, from, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private firestore = inject(Firestore);

  // ==================== USERS / CUSTOMERS ====================
  getUsers(): Observable<any[]> {
    const usersRef = collection(this.firestore, 'users');
    return collectionData(usersRef, { idField: 'id' }).pipe(
      map(users => users.filter(u => u['isActive'] !== false && u['role'] !== 'admin'))
    );
  }

  getUserById(id: string): Observable<any> {
    const userRef = doc(this.firestore, `users/${id}`);
    return docData(userRef, { idField: 'id' });
  }

  updateUser(id: string, userData: any): Observable<any> {
    const userRef = doc(this.firestore, `users/${id}`);
    return from(updateDoc(userRef, userData));
  }

  deleteCustomer(customerId: string): Observable<any> {
    const userRef = doc(this.firestore, `users/${customerId}`);
    // SOFT DELETE
    return from(updateDoc(userRef, { isActive: false }));
  }

  deleteUser(id: string): Observable<any> {
    return this.deleteCustomer(id);
  }

  // ==================== ORDERS ====================
  getOrders(): Observable<any[]> {
    const ordersRef = collection(this.firestore, 'orders');
    return collectionData(ordersRef, { idField: 'id' }).pipe(
      map(orders => orders.filter(o => o['isActive'] !== false))
    );
  }

  getOrderById(id: string): Observable<any> {
    const orderRef = doc(this.firestore, `orders/${id}`);
    return docData(orderRef, { idField: 'id' });
  }

  getOrdersByUserId(userId: string): Observable<any[]> {
    const ordersRef = collection(this.firestore, 'orders');
    const q = query(ordersRef, where('userId', '==', userId));
    return collectionData(q, { idField: 'id' });
  }

  createOrder(orderData: any): Observable<any> {
    orderData.isActive = true;
    orderData.createdAt = Timestamp.now();
    return from(addDoc(collection(this.firestore, 'orders'), orderData));
  }

  updateOrder(orderId: string, orderData: any): Observable<any> {
    const orderRef = doc(this.firestore, `orders/${orderId}`);
    return from(updateDoc(orderRef, orderData));
  }

  updateOrderStatus(orderId: string, status: string): Observable<any> {
    const orderRef = doc(this.firestore, `orders/${orderId}`);
    return from(updateDoc(orderRef, { status })).pipe(
      map(() => {
        // Trigger logic nâng cấp Cer khi giao thành công
        if (status === 'completed' || status === 'delivered') {
          this.triggerCertificateLogic(orderId);
        }
        return { success: true };
      })
    );
  }

  deleteOrder(orderId: string): Observable<any> {
    const orderRef = doc(this.firestore, `orders/${orderId}`);
    // SOFT DELETE
    return from(updateDoc(orderRef, { isActive: false }));
  }

  // ==================== PRODUCTS ====================
  getProducts(): Observable<any[]> {
    const productsRef = collection(this.firestore, 'products');
    return collectionData(productsRef, { idField: 'id' }).pipe(
      map(products => products.filter(p => p['isActive'] !== false))
    );
  }

  getProductById(id: string): Observable<any> {
    const productRef = doc(this.firestore, `products/${id}`);
    return docData(productRef, { idField: 'id' });
  }

  createProduct(productData: any): Observable<any> {
    productData.isActive = true;
    return from(addDoc(collection(this.firestore, 'products'), productData));
  }

  updateProduct(id: string, productData: any): Observable<any> {
    const productRef = doc(this.firestore, `products/${id}`);
    return from(updateDoc(productRef, productData));
  }

  deleteProduct(productId: string): Observable<any> {
    const productRef = doc(this.firestore, `products/${productId}`);
    // SOFT DELETE
    return from(updateDoc(productRef, { isActive: false }));
  }

  // ==================== PROMOTIONS ====================
  getPromotions(): Observable<any[]> {
    const promosRef = collection(this.firestore, 'promotions');
    return collectionData(promosRef, { idField: 'id' }).pipe(
      map(promos => promos.filter(p => p['isActive'] !== false))
    );
  }
  
  createPromotion(promoData: any): Observable<any> {
    promoData.isActive = true;
    return from(addDoc(collection(this.firestore, 'promotions'), promoData));
  }

  updatePromotion(id: string, promoData: any): Observable<any> {
    const promoRef = doc(this.firestore, `promotions/${id}`);
    return from(updateDoc(promoRef, promoData));
  }

  deletePromotion(promotionId: string): Observable<any> {
    const promoRef = doc(this.firestore, `promotions/${promotionId}`);
    // SOFT DELETE
    return from(updateDoc(promoRef, { isActive: false }));
  }

  // ==================== CERTIFICATE LOGIC ====================
  // Khi đơn giao thành công, tính toán tổng điểm xanh và tạo request nếu đủ điều kiện
  private async triggerCertificateLogic(orderId: string) {
    try {
      const orderRef = doc(this.firestore, `orders/${orderId}`);
      const orderSnap = await getDoc(orderRef);
      if (!orderSnap.exists()) return;
      
      const orderData = orderSnap.data();
      const userId = orderData['userId'];
      if (!userId) return;

      // Tính tổng hệ số xanh (giả lập đơn giản: lấy tất cả đơn completed của user)
      const ordersRef = collection(this.firestore, 'orders');
      const q = query(ordersRef, where('userId', '==', userId), where('status', 'in', ['completed', 'delivered']));
      const querySnapshot = await getDocs(q);
      
      let totalGreenPoints = 0;
      querySnapshot.forEach((doc: any) => {
        // Giả sử mỗi đơn có trường greenPoints (hệ số xanh) hoặc tính qua total amount
        const data = doc.data();
        totalGreenPoints += data['greenPoints'] || (data['totalAmount'] ? data['totalAmount'] / 1000 : 0);
      });

      // Ràng buộc nâng cấp Cer (ví dụ: > 1000 điểm -> Vàng)
      let targetCer = '';
      if (totalGreenPoints > 5000) targetCer = 'Diamond';
      else if (totalGreenPoints > 2000) targetCer = 'Gold';
      else if (totalGreenPoints > 500) targetCer = 'Silver';
      
      if (targetCer !== '') {
        // Tạo request lên certificate_requests
        const requestsRef = collection(this.firestore, 'certificate_requests');
        await addDoc(requestsRef, {
          userId: userId,
          requestedCer: targetCer,
          totalPoints: totalGreenPoints,
          status: 'pending',
          createdAt: Timestamp.now()
        });
        console.log(`Đã tạo yêu cầu cấp Cer ${targetCer} cho user ${userId}`);
      }
    } catch (e) {
      console.error("Lỗi khi trigger Certificate logic", e);
    }
  }

  getCertificateRequests(): Observable<any[]> {
    const reqRef = collection(this.firestore, 'certificate_requests');
    const q = query(reqRef, where('status', '==', 'pending'));
    return collectionData(q, { idField: 'id' });
  }

  approveCertificateRequest(requestId: string, userId: string, newCer: string): Observable<any> {
    const reqRef = doc(this.firestore, `certificate_requests/${requestId}`);
    const userRef = doc(this.firestore, `users/${userId}`);
    
    // Cập nhật cả 2 đồng thời
    const updateReq = updateDoc(reqRef, { status: 'approved' });
    const updateUserCer = updateDoc(userRef, { certificate: newCer });
    
    return from(Promise.all([updateReq, updateUserCer]));
  }
  
  rejectCertificateRequest(requestId: string): Observable<any> {
    const reqRef = doc(this.firestore, `certificate_requests/${requestId}`);
    return from(updateDoc(reqRef, { status: 'rejected' }));
  }

  // ==================== STUBS FOR COMPATIBILITY ====================
  getBlogs(): Observable<any[]> { return of([]); }
  createBlog(blogData: any): Observable<any> { return of({ success: true, data: { id: 'stub-blog' } }); }
  updateBlog(id: string, blogData: any): Observable<any> { return of({ success: true }); }
  deleteBlog(id: string): Observable<any> { return of(true); }
  getReviews(): Observable<any[]> { return of([]); }
  getTree(): Observable<any[]> { return of([]); }
  getCategories(): Observable<string[]> { return of(['Rau củ', 'Trái cây', 'Thực phẩm khô']); }
  getProductGroups(): Observable<any[]> { return of([]); }
  
  updateProductField(productId: string, field: string, value: any): Observable<any> { return of({ success: true }); }
  createProductGroup(groupName: string, skus: string[]): Observable<any> { return of({ success: true }); }
  updateProductGroup(sku: string, action: string, groupName: string): Observable<any> { return of({ success: true }); }
  getPromotionUsage(): Observable<any[]> { return of([]); }
  getPromotionTargets(): Observable<any[]> { return of([]); }
}
