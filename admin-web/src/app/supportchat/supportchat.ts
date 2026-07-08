import {
  AfterViewChecked,
  Component,
  ElementRef,
  NgZone,
  OnDestroy,
  OnInit,
  ViewChild,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { io, Socket } from 'socket.io-client';

type SupportConversation = {
  _id: string;
  customerId: string;
  customerName?: string;
  customerAvatar?: string;
  customerPhone?: string;
  status: 'open' | 'closed' | string;
  statusLabel?: string;
  lastMessageAt?: string | null;
  lastMessageText?: string;
  unreadCountUser?: number;
  unreadCountAdmin?: number;
};

type SupportMessage = {
  _id?: string;
  id?: string;
  conversationId: string;
  senderType: 'user' | 'admin' | string;
  senderId?: string;
  text: string;
  createdAt?: string;
  clientMessageId?: string;
};

type ConversationUpdatedPayload = {
  conversationId: string;
  customerId?: string;
  lastMessageAt?: string;
  lastMessageText?: string;
  unreadCountAdmin?: number;
  unreadCountUser?: number;
  status?: string;
};

function getAccessToken(): string | null {
  const token = localStorage.getItem('adminAccessToken');
  return token && token.trim() ? token.trim() : null;
}

function socketBaseUrlFromApiUrl(apiUrl: string): string {
  const trimmed = (apiUrl || '').replace(/\/+$/, '');
  return trimmed.endsWith('/api') ? trimmed.slice(0, -4) : trimmed;
}

function normalizeMessage(raw: SupportMessage): SupportMessage {
  return {
    ...raw,
    id: String(raw.id || raw._id || ''),
    conversationId: String(raw.conversationId || ''),
    clientMessageId: raw.clientMessageId ? String(raw.clientMessageId) : undefined,
  };
}

function resolveAvatarUrl(url?: string | null): string {
  const value = String(url || '').trim();
  if (!value) return '';
  if (value.startsWith('http://') || value.startsWith('https://')) return value;
  const base = environment.apiUrl.replace(/\/api$/, '');
  return `${base}${value.startsWith('/') ? '' : '/'}${value}`;
}

function initialsFromName(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return '?';
  if (parts.length === 1) return parts[0].charAt(0).toUpperCase();
  return `${parts[0].charAt(0)}${parts[parts.length - 1].charAt(0)}`.toUpperCase();
}

@Component({
  selector: 'app-supportchat',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './supportchat.html',
  styleUrl: './supportchat.css',
})
export class SupportChat implements OnInit, OnDestroy, AfterViewChecked {
  conversations = signal<SupportConversation[]>([]);
  messages = signal<SupportMessage[]>([]);
  selectedConversationId = signal<string | null>(null);
  selectedConversation = signal<SupportConversation | null>(null);
  statusText = signal<string>('Chưa kết nối');
  inputText = signal<string>('');
  avatarFailedIds = signal<Record<string, boolean>>({});
  customerTyping = signal<boolean>(false);

  @ViewChild('messagesContainer') messagesContainer?: ElementRef<HTMLDivElement>;

  private socket: Socket | null = null;
  private shouldScrollToBottom = false;
  private isSending = false;
  private isLocalTyping = false;
  private typingStopTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private http: HttpClient,
    private zone: NgZone,
  ) {}

  ngOnInit(): void {
    this.ensureSocket();
    this.loadConversations();
  }

  ngAfterViewChecked(): void {
    if (!this.shouldScrollToBottom) return;
    this.shouldScrollToBottom = false;
    const el = this.messagesContainer?.nativeElement;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }

  ngOnDestroy(): void {
    this.emitTypingUpdate(false);
    if (this.typingStopTimer) {
      clearTimeout(this.typingStopTimer);
      this.typingStopTimer = null;
    }
    this.disconnectSocket();
  }

  loadConversations(): void {
    this.http
      .get<{ success: boolean; data: SupportConversation[] }>(`${environment.apiUrl}/support/conversations`)
      .subscribe({
        next: (res) => {
          if (!res?.success) return;
          const list = Array.isArray(res.data) ? res.data : [];
          this.conversations.set(list);
          this.syncSelectedConversationMeta();
        },
        error: () => {
          this.statusText.set('Không tải được danh sách chat');
        },
      });
  }

  selectConversation(conversation: SupportConversation): void {
    this.customerTyping.set(false);
    this.emitTypingUpdate(false);
    this.isLocalTyping = false;
    if (this.typingStopTimer) {
      clearTimeout(this.typingStopTimer);
      this.typingStopTimer = null;
    }
    this.selectedConversationId.set(conversation._id);
    this.selectedConversation.set(conversation);
    this.messages.set([]);
    this.loadMessages(conversation._id);
    this.markConversationRead(conversation._id);
    this.joinConversation(conversation._id);
  }

  loadMessages(conversationId: string): void {
    const url = `${environment.apiUrl}/support/conversations/${encodeURIComponent(conversationId)}/messages?limit=100`;
    this.http.get<{ success: boolean; data: SupportMessage[] }>(url).subscribe({
      next: (res) => {
        if (!res?.success) return;
        const list = (Array.isArray(res.data) ? res.data : []).map(normalizeMessage);
        this.messages.set(list);
        this.requestScrollToBottom();
      },
      error: () => {
        // ignore
      },
    });
  }

  markConversationRead(conversationId: string): void {
    const url = `${environment.apiUrl}/support/conversations/${encodeURIComponent(conversationId)}/mark-read`;
    this.http.post(url, {}).subscribe({
      next: () => {
        this.conversations.update((list) =>
          list.map((c) => (c._id === conversationId ? { ...c, unreadCountAdmin: 0 } : c)),
        );
      },
      error: () => {
        // ignore
      },
    });
  }

  ensureSocket(): void {
    if (this.socket) return;
    const token = getAccessToken();
    if (!token) {
      this.statusText.set('Thiếu token admin (hãy đăng nhập lại)');
      return;
    }

    const socketBaseUrl = socketBaseUrlFromApiUrl(environment.apiUrl);
    this.socket = io(socketBaseUrl, {
      auth: { token },
      transports: ['websocket', 'polling'],
      reconnection: true,
    });

    this.socket.on('connect', () => {
      this.zone.run(() => {
        this.statusText.set('Đã kết nối');
        const convo = this.selectedConversationId();
        if (convo) this.joinConversation(convo);
      });
    });

    this.socket.on('disconnect', () => {
      this.zone.run(() => this.statusText.set('Mất kết nối'));
    });

    this.socket.on('connect_error', () => {
      this.zone.run(() => this.statusText.set('Lỗi kết nối socket'));
    });

    this.socket.on('message:new', (msg: SupportMessage) => {
      this.zone.run(() => this.handleIncomingMessage(msg));
    });

    this.socket.on('conversation:updated', (payload: ConversationUpdatedPayload) => {
      this.zone.run(() => this.handleConversationUpdated(payload));
    });

    this.socket.on('typing:update', (payload: { conversationId?: string; senderType?: string; isTyping?: boolean }) => {
      this.zone.run(() => {
        const selected = this.selectedConversationId();
        if (!selected || String(payload?.conversationId || '') !== String(selected)) return;
        if (payload?.senderType === 'user') {
          this.customerTyping.set(Boolean(payload.isTyping));
          if (payload.isTyping) this.requestScrollToBottom();
        }
      });
    });
  }

  private handleIncomingMessage(msg: SupportMessage): void {
    if (!msg) return;
    const normalized = normalizeMessage(msg);
    const selected = this.selectedConversationId();
    const convoId = normalized.conversationId;

    if (selected && convoId === String(selected)) {
      this.appendMessage(normalized);
      if (normalized.senderType === 'user') {
        this.customerTyping.set(false);
        this.markConversationRead(selected);
      }
    } else if (normalized.senderType === 'user' && convoId) {
      this.conversations.update((list) =>
        list.map((c) =>
          c._id === convoId
            ? {
                ...c,
                unreadCountAdmin: (Number(c.unreadCountAdmin || 0) || 0) + 1,
                lastMessageText: normalized.text,
                lastMessageAt: normalized.createdAt || c.lastMessageAt,
              }
            : c,
        ),
      );
    }
  }

  private handleConversationUpdated(payload: ConversationUpdatedPayload): void {
    if (!payload?.conversationId) return;
    const selected = this.selectedConversationId();
    this.conversations.update((list) => {
      const next = list.map((c) => {
        if (c._id !== payload.conversationId) return c;
        return {
          ...c,
          lastMessageAt: payload.lastMessageAt ?? c.lastMessageAt,
          lastMessageText: payload.lastMessageText ?? c.lastMessageText,
          unreadCountAdmin:
            selected === c._id ? 0 : (payload.unreadCountAdmin ?? c.unreadCountAdmin ?? 0),
          unreadCountUser: payload.unreadCountUser ?? c.unreadCountUser ?? 0,
          status: payload.status ?? c.status,
          statusLabel: payload.status === 'closed' ? 'Đã đóng' : 'Đang mở',
        };
      });
      return [...next].sort((a, b) => {
        const aTime = new Date(a.lastMessageAt || 0).getTime();
        const bTime = new Date(b.lastMessageAt || 0).getTime();
        return bTime - aTime;
      });
    });
    this.syncSelectedConversationMeta();
  }

  private syncSelectedConversationMeta(): void {
    const selectedId = this.selectedConversationId();
    if (!selectedId) return;
    const found = this.conversations().find((c) => c._id === selectedId) || null;
    if (found) this.selectedConversation.set(found);
  }

  disconnectSocket(): void {
    if (!this.socket) return;
    try {
      this.socket.removeAllListeners();
      this.socket.disconnect();
    } catch {
      // ignore
    }
    this.socket = null;
  }

  joinConversation(conversationId: string): void {
    if (!this.socket) return;
    this.socket.emit('conversation:join', { conversationId }, (res: { ok?: boolean }) => {
      if (res && res.ok === false) {
        this.zone.run(() => this.statusText.set('Không thể tham gia phòng chat'));
      }
    });
  }

  send(): void {
    const convo = this.selectedConversationId();
    const text = (this.inputText() || '').trim();
    if (!convo || !text || !this.socket || this.isSending) return;

    this.isSending = true;
    this.inputText.set('');
    this.emitTypingUpdate(false);
    if (this.typingStopTimer) {
      clearTimeout(this.typingStopTimer);
      this.typingStopTimer = null;
    }
    this.socket.emit(
      'message:send',
      {
        conversationId: convo,
        text,
        clientMessageId: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      },
      () => {
        this.zone.run(() => {
          this.isSending = false;
        });
      },
    );
  }

  onInputChange(event: Event): void {
    const target = event.target as HTMLInputElement | null;
    const value = (target?.value || '') as string;
    this.inputText.set(value);
    this.handleInputTyping(value);
  }

  private handleInputTyping(value: string): void {
    const convo = this.selectedConversationId();
    if (!convo || !this.socket) return;

    if (this.typingStopTimer) {
      clearTimeout(this.typingStopTimer);
      this.typingStopTimer = null;
    }

    const hasText = value.trim().length > 0;
    if (!hasText) {
      this.emitTypingUpdate(false);
      return;
    }

    this.emitTypingUpdate(true);
    this.typingStopTimer = setTimeout(() => this.emitTypingUpdate(false), 2000);
  }

  private emitTypingUpdate(isTyping: boolean): void {
    const convo = this.selectedConversationId();
    if (!convo || !this.socket || this.isLocalTyping === isTyping) return;
    this.isLocalTyping = isTyping;
    this.socket.emit('typing:update', { conversationId: convo, isTyping });
  }

  onInputKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Enter' || event.shiftKey || event.isComposing) return;
    event.preventDefault();
    this.send();
  }

  showAvatar(conversation: SupportConversation): boolean {
    const url = this.avatarUrl(conversation);
    return !!url && !this.avatarFailedIds()[conversation._id];
  }

  onAvatarError(conversationId: string): void {
    this.avatarFailedIds.update((current) => ({ ...current, [conversationId]: true }));
  }

  avatarUrl(conversation: SupportConversation): string {
    return resolveAvatarUrl(conversation.customerAvatar);
  }

  avatarInitials(conversation: SupportConversation): string {
    return initialsFromName(conversation.customerName || conversation.customerId || '?');
  }

  previewText(conversation: SupportConversation): string {
    const text = String(conversation.lastMessageText || '').trim();
    if (text) return text;
    return 'Chưa có tin nhắn';
  }

  formatTime(value?: string | null): string {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  }

  shouldShowDateDivider(index: number): boolean {
    const list = this.messages();
    if (index === 0) return true;
    const current = list[index];
    const previous = list[index - 1];
    if (!current.createdAt || !previous.createdAt) return false;
    
    const currentDate = new Date(current.createdAt);
    const previousDate = new Date(previous.createdAt);
    
    return (
      currentDate.getDate() !== previousDate.getDate() ||
      currentDate.getMonth() !== previousDate.getMonth() ||
      currentDate.getFullYear() !== previousDate.getFullYear()
    );
  }

  formatDateDivider(value?: string | null): string {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    
    return `${hours}:${minutes} ${day}/${month}/${year}`;
  }

  isUnread(conversation: SupportConversation): boolean {
    return (Number(conversation.unreadCountAdmin || 0) || 0) > 0;
  }

  unreadCount(conversation: SupportConversation): number {
    return Number(conversation.unreadCountAdmin || 0) || 0;
  }

  private appendMessage(msg: SupportMessage): void {
    const normalized = normalizeMessage(msg);
    const existing = this.messages();
    const msgId = normalized.id;
    if (msgId && existing.some((m) => m.id === msgId)) return;
    const clientId = msg.clientMessageId;
    if (clientId && existing.some((m) => m.clientMessageId === clientId)) {
      return;
    }
    this.messages.set([...existing, normalized]);
    this.requestScrollToBottom();
  }

  private requestScrollToBottom(): void {
    this.shouldScrollToBottom = true;
  }
}
