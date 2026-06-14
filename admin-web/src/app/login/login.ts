import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
  standalone: true
})
export class Login {
  // Trạng thái hiện tại: 'login' | 'forgot-1' | 'forgot-2' | 'reset'
  currentView = signal<'login' | 'forgot-1' | 'forgot-2' | 'reset'>('login');
  
  // Form data
  loginForm = {
    email: '',
    password: '',
    rememberMe: false
  };

  forgotPasswordEmail = '';
  
  resetPasswordForm = {
    email: '',
    otp: ['', '', '', '', '', ''],
    newPassword: '',
    confirmPassword: ''
  };

  // UI state
  showPassword = signal(false);
  isLoading = signal(false);
  errorMessage = signal('');
  
  // Popup state
  showPopup = signal(false);
  popupMessage = signal('');
  popupType = signal<'success' | 'error' | 'info'>('success');

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  /**
   * Hiển thị popup thông báo
   */
  displayPopup(message: string, type: 'success' | 'error' | 'info' = 'success'): void {
    this.popupMessage.set(message);
    this.popupType.set(type);
    this.showPopup.set(true);
  }

  /**
   * Đóng popup
   */
  closePopup(): void {
    this.showPopup.set(false);
    this.popupMessage.set('');
  }

  /**
   * Xử lý đăng nhập
   * Lấy data từ MongoDB collection 'admins' hoặc 'users' với role admin
   */
  onLogin(): void {
    console.log('\n🔐 === FRONTEND LOGIN ATTEMPT ===');
    console.log(`📧 Email: ${this.loginForm.email}`);
    console.log(`🔑 Password: ${this.loginForm.password ? '***' : 'empty'}`);
    
    // Validate: kiểm tra trường bắt buộc
    if (!this.loginForm.email || !this.loginForm.password) {
      this.errorMessage.set('Vui lòng nhập đầy đủ email và mật khẩu');
      console.log('❌ Validation failed: Missing email or password');
      return;
    }

    // Validate: kiểm tra định dạng email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.loginForm.email)) {
      this.errorMessage.set('Email không đúng định dạng');
      console.log('❌ Validation failed: Invalid email format');
      return;
    }

    // Validate: kiểm tra độ dài password
    if (this.loginForm.password.length < 6) {
      this.errorMessage.set('Mật khẩu phải có ít nhất 6 ký tự');
      console.log('❌ Validation failed: Password too short');
      return;
    }

    console.log('✅ Validation passed');
    console.log('⏳ Sending login request to backend...');
    
    this.isLoading.set(true);
    this.errorMessage.set('');

    // Gọi AuthService để đăng nhập
    this.authService.login(this.loginForm.email, this.loginForm.password)
      .subscribe({
        next: (success) => {
          this.isLoading.set(false);
          
          if (success) {
            console.log('✅ Login successful!');
            console.log('👤 User info:', this.authService.currentUser());
            
            // Chuyển về trang được yêu cầu hoặc dashboard
            const redirectUrl = localStorage.getItem('redirectUrl') || '/dashboard';
            localStorage.removeItem('redirectUrl');
            
            console.log(`🚀 Redirecting to: ${redirectUrl}`);
            console.log('================================\n');
            
            this.router.navigate([redirectUrl]);
          } else {
            console.log('❌ Login failed: Invalid credentials');
            console.log('================================\n');
            this.errorMessage.set('Email hoặc mật khẩu không đúng');
          }
        },
        error: (error: any) => {
          this.isLoading.set(false);
          console.error('❌ Login error:', error);
          console.log('================================\n');
          
          // Xử lý các loại lỗi khác nhau
          if (error.status === 401) {
            this.errorMessage.set('Email hoặc mật khẩu không đúng');
          } else if (error.status === 500) {
            this.errorMessage.set('Lỗi server. Vui lòng thử lại sau.');
          } else if (error.status === 0) {
            this.errorMessage.set('Không kết nối được với server. Vui lòng kiểm tra backend đang chạy.');
          } else {
            this.errorMessage.set('Có lỗi xảy ra. Vui lòng thử lại.');
          }
        }
      });
  }

  /**
   * Chuyển sang màn hình quên mật khẩu bước 1
   */
  goToForgotPassword(): void {
    this.currentView.set('forgot-1');
    this.errorMessage.set('');
  }

  /**
   * Quay lại màn hình đăng nhập
   */
  backToLogin(): void {
    this.currentView.set('login');
    this.errorMessage.set('');
  }

  /**
   * Chuyển về màn hình đăng nhập (alias của backToLogin)
   */
  goToLogin(): void {
    this.currentView.set('login');
    this.errorMessage.set('');
    // Reset form khi quay lại
    this.forgotPasswordEmail = '';
    this.resetPasswordForm = {
      email: '',
      otp: ['', '', '', '', '', ''],
      newPassword: '',
      confirmPassword: ''
    };
  }

  /**
   * Xử lý gửi email reset password (Bước 1)
   */
  onSendResetEmail(): void {
    if (!this.forgotPasswordEmail) {
      this.errorMessage.set('Vui lòng nhập email');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.authService.requestPasswordReset(this.forgotPasswordEmail)
      .subscribe({
        next: (response: any) => {
          this.isLoading.set(false);
          
          console.log('📧 Password reset response:', response);
          
          if (response && response.success) {
            // Hiển thị popup thông báo gửi OTP thành công
            this.displayPopup('Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư.', 'success');
            
            // Chuyển sang bước 2: nhập OTP
            this.resetPasswordForm.email = this.forgotPasswordEmail;
            this.currentView.set('forgot-2');
          }
        },
        error: (error: any) => {
          this.isLoading.set(false);
          console.error('Reset password error:', error);
          
          // Get error message from backend response
          const errorMsg = error.error?.error || error.message || 'Có lỗi xảy ra. Vui lòng thử lại.';
          
          if (error.status === 400) {
            this.errorMessage.set(errorMsg);
          } else if (error.status === 404) {
            this.errorMessage.set('Email không tồn tại trong hệ thống');
          } else if (error.status === 500) {
            this.errorMessage.set(errorMsg);
          } else if (error.status === 503) {
            this.errorMessage.set('Database chưa sẵn sàng. Vui lòng thử lại sau.');
          } else if (error.status === 0 || error.status === undefined) {
            this.errorMessage.set('Không kết nối được với server. Vui lòng kiểm tra backend đang chạy.');
          } else {
            this.errorMessage.set(errorMsg);
          }
        }
      });
  }

  /**
   * Tiếp tục sau khi nhập OTP (Bước 2 -> Bước 3)
   * XÁC THỰC OTP với backend trước khi cho phép đổi mật khẩu
   */
  onContinueWithOTP(): void {
    const otp = this.resetPasswordForm.otp.join('');
    
    // Validate input
    if (otp.length !== 6) {
      this.errorMessage.set('Vui lòng nhập đầy đủ mã OTP (6 số)');
      return;
    }

    if (!/^\d{6}$/.test(otp)) {
      this.errorMessage.set('Mã OTP phải là 6 chữ số');
      return;
    }

    // GỌI API XÁC THỰC OTP
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.authService.verifyOTP(this.resetPasswordForm.email, otp)
      .subscribe({
        next: (response: any) => {
          this.isLoading.set(false);
          
          if (response && response.success) {
            console.log('✅ OTP verified successfully');
            // Chuyển sang màn hình đặt lại mật khẩu
            this.currentView.set('reset');
            this.errorMessage.set('');
          } else {
            this.errorMessage.set('Mã OTP không đúng. Vui lòng thử lại.');
            // Clear OTP inputs và focus vào ô đầu tiên
            this.clearOTPInputs();
          }
        },
        error: (error: any) => {
          this.isLoading.set(false);
          console.error('❌ OTP verification failed:', error);
          
          if (error.status === 400) {
            // OTP sai hoặc hết hạn
            this.errorMessage.set(error.error?.error || 'Mã OTP không đúng hoặc đã hết hạn');
          } else {
            this.errorMessage.set('Có lỗi xảy ra. Vui lòng thử lại.');
          }
          
          // Clear OTP inputs và focus vào ô đầu tiên
          this.clearOTPInputs();
        }
      });
  }

  /**
   * Xử lý đặt lại mật khẩu (Bước 3)
   */
  onResetPassword(): void {
    // Validate
    if (!this.resetPasswordForm.newPassword || !this.resetPasswordForm.confirmPassword) {
      this.errorMessage.set('Vui lòng nhập đầy đủ mật khẩu');
      return;
    }

    if (this.resetPasswordForm.newPassword !== this.resetPasswordForm.confirmPassword) {
      this.errorMessage.set('Mật khẩu xác nhận không khớp');
      return;
    }

    if (this.resetPasswordForm.newPassword.length < 6) {
      this.errorMessage.set('Mật khẩu phải có ít nhất 6 ký tự');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    const otp = this.resetPasswordForm.otp.join('');
    
    this.authService.resetPassword(
      this.resetPasswordForm.email,
      otp,
      this.resetPasswordForm.newPassword
    ).subscribe({
      next: (success) => {
        this.isLoading.set(false);
        if (success) {
          // Hiển thị popup thành công
          this.displayPopup('Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.', 'success');
          
          // Reset form quên mật khẩu
          this.resetPasswordForm = {
            email: '',
            otp: ['', '', '', '', '', ''],
            newPassword: '',
            confirmPassword: ''
          };
          this.forgotPasswordEmail = '';
          
          // Clear form đăng nhập
          this.loginForm.email = '';
          this.loginForm.password = '';
          this.loginForm.rememberMe = false;
          
          // Quay về trang login sau 1.5s
          setTimeout(() => {
            this.currentView.set('login');
            this.errorMessage.set('');
          }, 1500);
        }
      },
      error: (error: any) => {
        this.isLoading.set(false);
        this.errorMessage.set('Có lỗi xảy ra. Vui lòng thử lại.');
        console.error('Reset password error:', error);
      }
    });
  }

  /**
   * Toggle hiển thị mật khẩu
   */
  togglePasswordVisibility(): void {
    this.showPassword.set(!this.showPassword());
  }

  /**
   * Xử lý nhập OTP (tự động focus ô tiếp theo)
   */
  onOtpInput(index: number, event: any): void {
    const value = event.target.value;
    
    // Chỉ cho phép số
    if (value && !/^\d$/.test(value)) {
      event.target.value = '';
      return;
    }

    this.resetPasswordForm.otp[index] = value;

    // Tự động focus ô tiếp theo
    if (value && index < 5) {
      const nextInput = event.target.nextElementSibling;
      if (nextInput) {
        nextInput.focus();
      }
    }
  }

  /**
   * Xử lý xóa OTP (tự động focus ô trước đó)
   */
  onOtpKeydown(index: number, event: KeyboardEvent): void {
    if (event.key === 'Backspace' && !this.resetPasswordForm.otp[index] && index > 0) {
      const prevInput = (event.target as HTMLElement).previousElementSibling as HTMLInputElement;
      if (prevInput) {
        prevInput.focus();
      }
    }
  }

  /**
   * Clear tất cả OTP inputs và focus vào ô đầu tiên
   */
  clearOTPInputs(): void {
    // Reset OTP inputs
    this.resetPasswordForm.otp = ['', '', '', '', '', ''];
    
    // Focus vào ô đầu tiên sau một chút delay
    setTimeout(() => {
      const firstInput = document.querySelector('.otp-input') as HTMLInputElement;
      if (firstInput) {
        firstInput.focus();
      }
    }, 100);
  }

  /**
   * Gửi lại OTP
   */
  resendOTP(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.authService.requestPasswordReset(this.resetPasswordForm.email)
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          
          // Hiển thị popup thông báo gửi lại OTP thành công với icon tick màu xanh
          this.displayPopup('Mã OTP đã được gửi lại. Vui lòng kiểm tra email.', 'success');
          
          // Clear OTP inputs và focus vào ô đầu tiên
          this.clearOTPInputs();
        },
        error: (error: any) => {
          this.isLoading.set(false);
          this.errorMessage.set('Không thể gửi lại OTP. Vui lòng thử lại.');
        }
      });
  }
}

