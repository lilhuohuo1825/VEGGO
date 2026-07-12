import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject, interval, of } from 'rxjs';
import { catchError, startWith, switchMap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { io, Socket } from 'socket.io-client';

export interface AdminNotification {
  _id?: string;
  id?: string;
  type: 'order_cancellation_request' | 'new_order' | 'return_request' | 'scheduled_delivery_reminder' | 'consultation' | 'system' | 'other';
  customerId?: string;
  customerName?: string;
  orderId?: string;
  orderTotal?: number;
  reason?: string;
  title?: string;
  message?: string;
  sku?: string;
  productName?: string;
  questionId?: string;
  status: 'pending' | 'approved' | 'rejected' | 'active';
  read: boolean;
  createdAt: Date | string;
  updatedAt?: Date | string;
}

export interface RealtimeEnvelope<T = any> {
  type: string;
  entity?: string | null;
  action?: string | null;
  data: T;
  occurredAt: string;
}

function socketBaseUrlFromApiUrl(apiUrl: string): string {
  const trimmed = (apiUrl || '').replace(/\/+$/, '');
  return trimmed.endsWith('/api') ? trimmed.slice(0, -4) : trimmed;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = `${environment.apiUrl}/notifications`;
  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$: Observable<number> = this.unreadCountSubject.asObservable();

  private notificationsSubject = new BehaviorSubject<AdminNotification[]>([]);
  public notifications$: Observable<AdminNotification[]> = this.notificationsSubject.asObservable();

  private newNotificationSubject = new BehaviorSubject<AdminNotification | null>(null);
  public newNotification$: Observable<AdminNotification | null> = this.newNotificationSubject.asObservable();

  private orderChangedSubject = new Subject<RealtimeEnvelope>();
  public orderChanged$: Observable<RealtimeEnvelope> = this.orderChangedSubject.asObservable();

  private promotionChangedSubject = new Subject<RealtimeEnvelope>();
  public promotionChanged$: Observable<RealtimeEnvelope> = this.promotionChangedSubject.asObservable();

  private realtimeEventSubject = new Subject<RealtimeEnvelope>();
  public realtimeEvent$: Observable<RealtimeEnvelope> = this.realtimeEventSubject.asObservable();

  private pollingInterval = 30000; // 30s – tránh spam API khi admin mở lâu
  private previousNotificationIds: Set<string> = new Set();
  private socket: Socket | null = null;

  constructor(private http: HttpClient) {
    this.ensureSocket();
    // Start polling for notifications
    this.startPolling();
  }

  private getAccessToken(): string | null {
    const token = localStorage.getItem('adminAccessToken');
    return token && token.trim() ? token.trim() : null;
  }

  private ensureSocket(): void {
    if (this.socket) return;
    const token = this.getAccessToken();
    if (!token) return;

    this.socket = io(socketBaseUrlFromApiUrl(environment.apiUrl), {
      auth: { token },
      transports: ['websocket', 'polling'],
      reconnection: true,
    });

    this.socket.on('admin:notification', (envelope: RealtimeEnvelope<AdminNotification>) => {
      this.applyIncomingNotification(envelope?.data);
    });

    this.socket.on('admin:notification-updated', (envelope: RealtimeEnvelope<AdminNotification>) => {
      this.applyUpdatedNotification(envelope?.data);
    });

    this.socket.on('order:created', (envelope: RealtimeEnvelope) => this.emitOrderChanged(envelope));
    this.socket.on('order:updated', (envelope: RealtimeEnvelope) => this.emitOrderChanged(envelope));
    this.socket.on('order:status-updated', (envelope: RealtimeEnvelope) => this.emitOrderChanged(envelope));
    this.socket.on('order:payment-updated', (envelope: RealtimeEnvelope) => this.emitOrderChanged(envelope));
    this.socket.on('order:deleted', (envelope: RealtimeEnvelope) => this.emitOrderChanged(envelope));

    this.socket.on('promotion:created', (envelope: RealtimeEnvelope) => this.emitPromotionChanged(envelope));
    this.socket.on('promotion:updated', (envelope: RealtimeEnvelope) => this.emitPromotionChanged(envelope));
    this.socket.on('promotion:deleted', (envelope: RealtimeEnvelope) => this.emitPromotionChanged(envelope));
    this.socket.on('promotion:changed', (envelope: RealtimeEnvelope) => this.emitPromotionChanged(envelope));

    this.socket.on('realtime:event', (envelope: RealtimeEnvelope) => {
      if (!envelope?.type) return;
      this.realtimeEventSubject.next(envelope);
      if (envelope.type.startsWith('order:')) {
        this.emitOrderChanged(envelope);
      } else if (envelope.type.startsWith('promotion:')) {
        this.emitPromotionChanged(envelope);
      }
    });

    this.socket.on('connect_error', () => {
      // Polling remains the fallback source of truth.
    });
  }

  private emitOrderChanged(envelope: RealtimeEnvelope | null | undefined): void {
    if (!envelope) return;
    this.orderChangedSubject.next(envelope);
  }

  private emitPromotionChanged(envelope: RealtimeEnvelope | null | undefined): void {
    if (!envelope) return;
    this.promotionChangedSubject.next(envelope);
  }

  private notificationId(notification: AdminNotification | null | undefined): string {
    return String(notification?._id || notification?.id || '').trim();
  }

  private applyIncomingNotification(notification: AdminNotification | null | undefined): void {
    const id = this.notificationId(notification);
    if (!notification || !id) return;

    const current = this.notificationsSubject.value;
    if (current.some(item => this.notificationId(item) === id)) return;

    const next = [notification, ...current].sort((a, b) => {
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });

    this.previousNotificationIds.add(id);
    this.notificationsSubject.next(next);
    this.updateUnreadCount(next);
    if (!notification.read) {
      this.newNotificationSubject.next(notification);
    }
  }

  private applyUpdatedNotification(notification: AdminNotification | null | undefined): void {
    const id = this.notificationId(notification);
    if (!notification || !id) return;

    const current = this.notificationsSubject.value;
    const found = current.some(item => this.notificationId(item) === id);
    const next = found
      ? current.map(item => (this.notificationId(item) === id ? { ...item, ...notification } : item))
      : [notification, ...current];

    this.previousNotificationIds.add(id);
    this.notificationsSubject.next(next);
    this.updateUnreadCount(next);
  }

  /**
   * Start polling for notifications
   */
  private startPolling(): void {
    interval(this.pollingInterval)
      .pipe(
        startWith(0),
        switchMap(() => {
          return this.http.get<{ success: boolean; data: AdminNotification[] }>(
            `${this.apiUrl}`
          ).pipe(
            catchError(error => {
              console.error('Error fetching notifications:', error);
              return of({ success: false, data: [] });
            })
          );
        })
      )
      .subscribe(response => {
        if (response && response.success && response.data) {
          const currentNotifications = response.data;
          const currentIds = new Set(
            currentNotifications.map(n => n._id || n.id || '').filter(id => id)
          );

          // Detect new notifications
          const newNotifications = currentNotifications.filter(n => {
            const id = n._id || n.id || '';
            return id && !this.previousNotificationIds.has(id) && !n.read;
          });

          // If there are new notifications, emit them
          if (newNotifications.length > 0) {
            // Emit the most recent new notification
            const latestNewNotification = newNotifications.sort((a, b) => {
              const dateA = new Date(a.createdAt).getTime();
              const dateB = new Date(b.createdAt).getTime();
              return dateB - dateA;
            })[0];

            this.newNotificationSubject.next(latestNewNotification);
            console.log('🔔 New notification detected:', latestNewNotification);
          }

          // Update previous IDs
          this.previousNotificationIds = currentIds;

          // Update notifications
          this.notificationsSubject.next(currentNotifications);
          this.updateUnreadCount(currentNotifications);
        }
      });
  }

  /**
   * Load notifications from API
   */
  loadNotifications(): void {
    this.ensureSocket();
    this.http.get<{ success: boolean; data: AdminNotification[] }>(
      `${this.apiUrl}`
    ).pipe(
      catchError(error => {
        console.error('Error loading notifications:', error);
        return of({ success: false, data: [] });
      })
    ).subscribe(response => {
      if (response && response.success && response.data) {
        // Initialize previous IDs with current notifications to avoid showing popup for existing notifications
        const currentIds = new Set(
          response.data.map(n => n._id || n.id || '').filter(id => id)
        );
        if (this.previousNotificationIds.size === 0) {
          this.previousNotificationIds = currentIds;
        }

        this.notificationsSubject.next(response.data);
        this.updateUnreadCount(response.data);
      }
    });
  }

  /**
   * Get unread count from API
   */
  loadUnreadCount(): void {
    this.ensureSocket();
    this.http.get<{ success: boolean; count: number }>(
      `${this.apiUrl}/unread-count`
    ).pipe(
      catchError(error => {
        console.error('Error loading unread count:', error);
        return of({ success: false, count: 0 });
      })
    ).subscribe(response => {
      if (response && response.success) {
        this.unreadCountSubject.next(response.count || 0);
      }
    });
  }

  /**
   * Mark notification as read
   */
  markAsRead(notificationId: string): void {
    // Optimistic update: Update local state immediately
    const currentNotifications = this.notificationsSubject.value;
    const updatedNotifications = currentNotifications.map(notif => {
      if ((notif._id === notificationId || notif.id === notificationId) && !notif.read) {
        return { ...notif, read: true };
      }
      return notif;
    });
    this.notificationsSubject.next(updatedNotifications);
    this.updateUnreadCount(updatedNotifications);

    // Then update on server
    this.http.put<{ success: boolean }>(
      `${this.apiUrl}/${notificationId}/read`,
      {}
    ).pipe(
      catchError(error => {
        console.error('Error marking notification as read:', error);
        // Revert optimistic update on error
        this.loadNotifications();
        this.loadUnreadCount();
        return of({ success: false });
      })
    ).subscribe(response => {
      if (response && response.success) {
        // Reload to ensure sync with server
        this.loadNotifications();
        this.loadUnreadCount();
      } else {
        // Reload on failure to revert
        this.loadNotifications();
        this.loadUnreadCount();
      }
    });
  }

  /**
   * Update notification status (approve/reject)
   */
  updateNotificationStatus(notificationId: string, action: 'approve' | 'reject'): void {
    this.http.put<{ success: boolean }>(
      `${this.apiUrl}/${notificationId}/status`,
      { action }
    ).pipe(
      catchError(error => {
        console.error('Error updating notification status:', error);
        return of({ success: false });
      })
    ).subscribe(response => {
      if (response && response.success) {
        // Reload notifications
        this.loadNotifications();
      }
    });
  }

  /**
   * Update unread count from notifications
   */
  private updateUnreadCount(notifications: AdminNotification[]): void {
    const unreadCount = notifications.filter(n => !n.read).length;
    this.unreadCountSubject.next(unreadCount);
  }

  getUnreadCount(): number {
    return this.unreadCountSubject.value;
  }

  getNotifications(): Observable<AdminNotification[]> {
    return this.notifications$;
  }

  /**
   * Toast notification methods for admin UI feedback
   * These are simple console logs or could be integrated with a toast service
   */
  showSuccess(message: string, duration: number = 3000): void {
    console.log('✅ Success:', message);
    // TODO: Integrate with toast service if available
  }

  showError(message: string, duration: number = 4000): void {
    console.error('❌ Error:', message);
    // TODO: Integrate with toast service if available
  }

  showWarning(message: string, duration: number = 3000): void {
    console.warn('⚠️ Warning:', message);
    // TODO: Integrate with toast service if available
  }

  showInfo(message: string, duration: number = 3000): void {
    console.info('ℹ️ Info:', message);
    // TODO: Integrate with toast service if available
  }

  removeNotification(id: string): void {
    // For admin, we don't have a remove endpoint, so we just mark as read
    this.markAsRead(id);
  }
}
