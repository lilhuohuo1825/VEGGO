import { Injectable, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Auth, signInWithEmailAndPassword, signOut, authState, sendPasswordResetEmail } from '@angular/fire/auth';
import { User } from 'firebase/auth';
import { Firestore, doc, getDoc } from '@angular/fire/firestore';
import { Observable, from, of } from 'rxjs';
import { map, catchError, switchMap } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

export interface AdminUser {
  id: string;
  email: string;
  name: string;
  role: string;
  certificate?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private auth = inject(Auth);
  private firestore = inject(Firestore);
  private router = inject(Router);
  private http = inject(HttpClient);
  
  // Signal để track trạng thái đăng nhập
  isAuthenticated = signal<boolean>(false);
  currentUser = signal<AdminUser | null>(null);

  constructor() {
    // 1. Phục hồi session đã đăng nhập qua MongoDB nếu có
    const savedUserJson = localStorage.getItem('adminUser');
    if (savedUserJson) {
      try {
        const savedUser = JSON.parse(savedUserJson);
        this.isAuthenticated.set(true);
        this.currentUser.set(savedUser);
      } catch (e) {
        console.error('Lỗi khi khôi phục session adminUser:', e);
      }
    }

    // 2. Tự động lắng nghe trạng thái đăng nhập từ Firebase Auth
    authState(this.auth).subscribe(async (user: User | null) => {
      if (user) {
        try {
          const userDocRef = doc(this.firestore, `users/${user.uid}`);
          const userSnap = await getDoc(userDocRef);
          
          if (userSnap.exists()) {
            const userData = userSnap.data();
            if (userData['role'] === 'admin') {
              const adminInfo = {
                id: user.uid,
                email: user.email || '',
                name: userData['name'] || user.email || 'Admin',
                role: 'admin',
                certificate: userData['certificate']
              };
              localStorage.setItem('adminUser', JSON.stringify(adminInfo));
              this.isAuthenticated.set(true);
              this.currentUser.set(adminInfo);
            } else {
              await signOut(this.auth);
              this.clearSession();
            }
          } else {
            await signOut(this.auth);
            this.clearSession();
          }
        } catch (error) {
          console.error("Lỗi khi fetch dữ liệu user:", error);
          this.clearSession();
        }
      } else {
        // Chỉ xóa session khi Firebase báo logout và đồng thời không có session local
        if (!localStorage.getItem('adminUser')) {
          this.clearSession();
        }
      }
    });
  }

  /**
   * Đăng nhập thông qua MongoDB và fallback Firebase Auth
   */
  login(email: string, password: string): Observable<boolean> {
    // Thử đăng nhập qua backend MongoDB trước
    return this.http.post(`${environment.apiUrl}/users/admin/login`, { email, password }).pipe(
      map((response: any) => {
        if (response && response.success && response.user) {
          localStorage.setItem('adminUser', JSON.stringify(response.user));
          if (response.accessToken) {
            localStorage.setItem('adminAccessToken', String(response.accessToken));
          }
          this.isAuthenticated.set(true);
          this.currentUser.set(response.user);
          return true;
        }
        return false;
      }),
      catchError(mongoError => {
        console.warn('⚠️ Đăng nhập bằng MongoDB thất bại, thử đăng nhập bằng Firebase Auth...', mongoError.message);
        
        // Thử đăng nhập bằng Firebase Auth nếu MongoDB không phản hồi hoặc có lỗi
        return from(signInWithEmailAndPassword(this.auth, email, password)).pipe(
          switchMap(async (credential) => {
            const user = credential.user;
            const userDocRef = doc(this.firestore, `users/${user.uid}`);
            const userSnap = await getDoc(userDocRef);
            
            if (userSnap.exists() && userSnap.data()['role'] === 'admin') {
              const adminInfo = {
                id: user.uid,
                email: user.email || '',
                name: userSnap.data()['name'] || user.email || 'Admin',
                role: 'admin',
                certificate: userSnap.data()['certificate']
              };
              localStorage.setItem('adminUser', JSON.stringify(adminInfo));
              this.isAuthenticated.set(true);
              this.currentUser.set(adminInfo);
              return true;
            } else {
              await signOut(this.auth);
              throw new Error("Tài khoản này không có quyền quản trị (Admin).");
            }
          }),
          catchError(fbError => {
            console.error('❌ Lỗi đăng nhập Firebase:', fbError);
            throw fbError;
          })
        );
      })
    );
  }

  /**
   * Đăng xuất
   */
  logout(): void {
    localStorage.removeItem('adminUser');
    localStorage.removeItem('adminAccessToken');
    signOut(this.auth).then(() => {
      this.clearSession();
      this.router.navigate(['/login']);
    }).catch(() => {
      // Đề phòng lỗi Firebase Auth signOut (khi đăng nhập qua MongoDB đơn thuần)
      this.clearSession();
      this.router.navigate(['/login']);
    });
  }

  /**
   * Xóa session
   */
  private clearSession(): void {
    localStorage.removeItem('adminUser');
    localStorage.removeItem('adminAccessToken');
    this.isAuthenticated.set(false);
    this.currentUser.set(null);
  }

  /**
   * Lấy token (Firebase SDK tự động quản lý, hàm này có thể bỏ qua hoặc trả về null)
   */
  getToken(): string | null {
    return null;
  }

  /**
   * Gửi email reset password qua Backend API để nhận OTP
   */
  requestPasswordReset(email: string): Observable<any> {
    return this.http.post(`${environment.apiUrl}/users/admin/forgot-password`, { email }).pipe(
      catchError(error => {
        console.error('Password reset request error:', error);
        throw error;
      })
    );
  }

  /**
   * Xác thực OTP qua Backend API
   */
  verifyOTP(email: string, otp: string): Observable<any> {
    return this.http.post(`${environment.apiUrl}/users/admin/verify-otp`, { email, otp }).pipe(
      catchError(error => {
        console.error('OTP verification failed:', error);
        throw error;
      })
    );
  }

  /**
   * Đổi mật khẩu qua Backend API bằng OTP và mật khẩu mới
   */
  resetPassword(email: string, otp: string, newPassword: string): Observable<boolean> {
    return this.http.post(`${environment.apiUrl}/users/admin/reset-password`, { email, otp, newPassword }).pipe(
      map((response: any) => {
        return !!(response && response.success);
      }),
      catchError(error => {
        console.error('Reset password error:', error);
        throw error;
      })
    );
  }
}
