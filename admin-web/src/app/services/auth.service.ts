import { Injectable, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Auth, signInWithEmailAndPassword, signOut, authState, sendPasswordResetEmail } from '@angular/fire/auth';
import { User } from 'firebase/auth';
import { Firestore, doc, getDoc } from '@angular/fire/firestore';
import { Observable, from, of } from 'rxjs';
import { map, catchError, switchMap } from 'rxjs/operators';

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
  
  // Signal để track trạng thái đăng nhập
  isAuthenticated = signal<boolean>(false);
  currentUser = signal<AdminUser | null>(null);

  constructor() {
    // Tự động lắng nghe trạng thái đăng nhập từ Firebase Auth
    authState(this.auth).subscribe(async (user: User | null) => {
      if (user) {
        try {
          // Lấy thông tin user từ Firestore để check role
          const userDocRef = doc(this.firestore, `users/${user.uid}`);
          const userSnap = await getDoc(userDocRef);
          
          if (userSnap.exists()) {
            const userData = userSnap.data();
            if (userData['role'] === 'admin') {
              this.isAuthenticated.set(true);
              this.currentUser.set({
                id: user.uid,
                email: user.email || '',
                name: userData['name'] || user.email || 'Admin',
                role: 'admin',
                certificate: userData['certificate']
              });
            } else {
              // Không phải admin -> đăng xuất
              await signOut(this.auth);
              this.clearSession();
            }
          } else {
            // Document không tồn tại
            await signOut(this.auth);
            this.clearSession();
          }
        } catch (error) {
          console.error("Lỗi khi fetch dữ liệu user:", error);
          this.clearSession();
        }
      } else {
        this.clearSession();
      }
    });
  }

  /**
   * Đăng nhập bằng Firebase Auth
   */
  login(email: string, password: string): Observable<boolean> {
    return from(signInWithEmailAndPassword(this.auth, email, password)).pipe(
      switchMap(async (credential) => {
        const user = credential.user;
        // Check role trên Firestore
        const userDocRef = doc(this.firestore, `users/${user.uid}`);
        const userSnap = await getDoc(userDocRef);
        
        if (userSnap.exists() && userSnap.data()['role'] === 'admin') {
           return true;
        } else {
           await signOut(this.auth);
           throw new Error("Tài khoản này không có quyền quản trị (Admin).");
        }
      }),
      catchError(error => {
        console.error('❌ Lỗi đăng nhập:', error);
        throw error;
      })
    );
  }

  /**
   * Đăng xuất
   */
  logout(): void {
    signOut(this.auth).then(() => {
      this.clearSession();
      this.router.navigate(['/login']);
    });
  }

  /**
   * Xóa session
   */
  private clearSession(): void {
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
   * Gửi email reset password qua Firebase Auth
   */
  requestPasswordReset(email: string): Observable<any> {
    return from(sendPasswordResetEmail(this.auth, email)).pipe(
      map(() => ({ success: true, message: 'Email khôi phục đã được gửi.' })),
      catchError(error => {
        console.error('Password reset request error:', error);
        throw error;
      })
    );
  }

  // Giữ lại các hàm stub để giao diện cũ không bị lỗi biên dịch
  verifyOTP(email: string, otp: string): Observable<any> {
    return of({ success: true });
  }

  resetPassword(email: string, otp: string, newPassword: string): Observable<boolean> {
    return of(true);
  }
}
