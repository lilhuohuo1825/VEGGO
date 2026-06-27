import { Component, OnInit, inject, ChangeDetectorRef, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Router } from '@angular/router';
import { ApiService } from '../services/api.service';
import { NotificationService } from '../services/notification.service';

interface BlogJSON {
  id: string;
  _id?: string;
  img: string;
  imageUrl?: string;
  title: string;
  excerpt: string;
  pubDate?: string;
  publishedAt?: string;
  updatedAt?: string;
  UpdatedAt?: string;
  createdAt?: string;
  author: string;
  categoryTag: string;
  content: string;
  views?: number;
}

interface BlogPost {
  _id: string;
  blog_id: string;
  id?: string; // MongoDB 'id' field (NS002, NS016, etc.) - same as blog_id
  title: string;
  author: string;
  email?: string; // Email của tác giả
  category: string;
  content: string;
  created_date: string;
  updated_date?: string; // Ngày cập nhật mới nhất
  views: number;
  selected?: boolean;
}

@Component({
  selector: 'app-blog',
  imports: [CommonModule, FormsModule],
  templateUrl: './blog.html',
  styleUrl: './blog.css',
  standalone: true
})
export class Blog implements OnInit, AfterViewInit {
  @ViewChild('contentEditor') contentEditor!: ElementRef<HTMLDivElement>;
  @ViewChild('formatBlockSelect') formatBlockSelect?: ElementRef<HTMLSelectElement>;
  @ViewChild('fontSizeSelect') fontSizeSelect?: ElementRef<HTMLSelectElement>;
  
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiService = inject(ApiService);
  private cdr = inject(ChangeDetectorRef);
  private notificationService = inject(NotificationService);

  blogs: BlogPost[] = [];
  allBlogs: BlogPost[] = [];
  isLoading = false;
  loadError = '';

  selectedCount = 0;
  selectAll = false;
  searchQuery = '';
  selectedSort: string = '';
  
  // View mode: 'list' or 'detail'
  viewMode: 'list' | 'detail' = 'list';
  currentBlog: BlogPost | null = null;
  isNewBlog = false; // Track if we're creating a new blog
  
  // Edit popup state
  showEditPopup = false;

  // Confirmation dialog state
  showConfirmDialog = false;
  confirmMessage = '';
  confirmCallback: (() => void) | null = null;
  
  // Popup notification state
  showPopup: boolean = false;
  popupMessage: string = '';
  popupType: 'success' | 'error' | 'info' = 'success';
  private popupAutoCloseTimeout?: any;
  private pendingReloadAfterPopup?: () => void; // Callback to execute when popup is closed
  
  // Sort dropdown state
  showSortDropdown: boolean = false;
  currentSortBy: 'date' | 'views' = 'date';
  currentSortOrder: 'asc' | 'desc' = 'desc';
  
  // Product categories and subcategories for hashtag generation
  // Map: category -> Set of subcategories and product keywords
  categoryToSubcategories: Map<string, Set<string>> = new Map();
  
  // Blog categories (separate from product categories)
  blogCategories: string[] = [
    'Sức khỏe',
    'Dinh dưỡng',
    'Mẹo dinh dưỡng',
    'Công thức nấu ăn',
    'Lối sống',
    'Yoga & Thể thao',
    'Mẹ và bé',
    'Làm đẹp',
    'Thông tin sản phẩm',
    'Hướng dẫn sử dụng',
    'Câu chuyện khách hàng',
    'Kiến thức nông nghiệp'
  ];
  
  // Date picker state
  showDatePicker: boolean = false;
  selectedDate: Date = new Date();
  
  // Editor state for undo/redo
  private editorHistory: string[] = [];
  private historyIndex: number = -1;
  private maxHistorySize: number = 50;
  private editorSetup: boolean = false;
  private inputHandler?: () => void;
  private keydownHandler?: (e: KeyboardEvent) => void;
  private historySaveTimeout?: any;
  private contentUpdateTimeout?: any;
  private isUpdatingContent: boolean = false;

  ngOnInit(): void {
    this.loadProductCategories();
    this.loadBlogs();
    
    // Global click listener to close dropdowns
    document.addEventListener('click', (e) => {
      this.showSortDropdown = false;
      // Close date picker if clicking outside
      const target = e.target as HTMLElement;
      if (!target.closest('.date-picker-container') && !target.closest('.input-icon')) {
        this.showDatePicker = false;
      }
      this.cdr.detectChanges();
    });
  }

  ngAfterViewInit(): void {
    // Setup editor after view is initialized
    // Use setTimeout to ensure view is fully rendered
    setTimeout(() => {
      if (this.contentEditor?.nativeElement && this.viewMode === 'detail') {
        this.setupEditor();
      }
    }, 100);
  }

  /**
   * Setup editor with history tracking and keyboard shortcuts
   */
  setupEditor(): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const editor = this.contentEditor.nativeElement;
    
    // Remove old listeners if they exist
    if (this.editorSetup && this.inputHandler && this.keydownHandler) {
      editor.removeEventListener('input', this.inputHandler);
      editor.removeEventListener('keydown', this.keydownHandler);
    }
    
    // Clear any pending timeouts
    if (this.historySaveTimeout) {
      clearTimeout(this.historySaveTimeout);
    }
    if (this.contentUpdateTimeout) {
      clearTimeout(this.contentUpdateTimeout);
    }
    
    // Only set content if editor is empty or content is different
    const currentEditorContent = editor.innerHTML.trim();
    const blogContent = this.currentBlog?.content || '';
    
    if (currentEditorContent !== blogContent && !this.isUpdatingContent) {
      this.isUpdatingContent = true;
      
      // If editor is empty, just set content and move cursor to end
      if (currentEditorContent === '' && blogContent !== '') {
        editor.innerHTML = blogContent;
        requestAnimationFrame(() => {
          const selection = window.getSelection();
          if (selection) {
            const range = document.createRange();
            range.selectNodeContents(editor);
            range.collapse(false);
            selection.removeAllRanges();
            selection.addRange(range);
          }
          this.isUpdatingContent = false;
        });
      } else if (blogContent === '' && currentEditorContent !== '') {
        // If clearing content
        editor.innerHTML = '';
        requestAnimationFrame(() => {
          editor.focus();
          this.isUpdatingContent = false;
        });
      } else {
        // If content changed, preserve cursor position if possible
        const selection = window.getSelection();
        let savedOffset = 0;
        let savedNode: Node | null = null;
        
        if (selection && selection.rangeCount > 0) {
          try {
            const range = selection.getRangeAt(0);
            savedNode = range.startContainer;
            savedOffset = range.startOffset;
            
            // Calculate text offset
            const preCaretRange = range.cloneRange();
            preCaretRange.selectNodeContents(editor);
            preCaretRange.setEnd(range.startContainer, range.startOffset);
            savedOffset = preCaretRange.toString().length;
          } catch (e) {
            // Ignore
          }
        }
        
        editor.innerHTML = blogContent;
        
        // Restore cursor position
        requestAnimationFrame(() => {
          if (savedOffset > 0 && blogContent.length > 0) {
            this.restoreCursorPosition(savedOffset);
          } else {
            // Move cursor to end
            const selection = window.getSelection();
            if (selection) {
              const range = document.createRange();
              range.selectNodeContents(editor);
              range.collapse(false);
              selection.removeAllRanges();
              selection.addRange(range);
            }
          }
          this.isUpdatingContent = false;
        });
      }
    }
    
    // Initialize history with current content
    this.saveToHistory();
    
    // Update toolbar dropdowns initially
    setTimeout(() => {
      this.updateToolbarDropdowns();
    }, 50);
    
    // Track changes for undo/redo with debounce
    this.inputHandler = () => {
      // Don't do anything if we're updating content programmatically
      if (this.isUpdatingContent) return;
      
      // Update toolbar dropdowns based on current selection
      this.updateToolbarDropdowns();
      
      // Debounce history saving to avoid lag
      if (this.historySaveTimeout) {
        clearTimeout(this.historySaveTimeout);
      }
      this.historySaveTimeout = setTimeout(() => {
        this.saveToHistory();
      }, 300); // Save history after 300ms of no typing
      
      // Debounce content update to avoid re-render during typing
      if (this.contentUpdateTimeout) {
        clearTimeout(this.contentUpdateTimeout);
      }
      this.contentUpdateTimeout = setTimeout(() => {
        if (!this.isUpdatingContent && this.currentBlog) {
          this.currentBlog.content = editor.innerHTML;
        }
      }, 500); // Update content after 500ms of no typing
    };
    editor.addEventListener('input', this.inputHandler, { passive: true });
    
    // Update toolbar when selection changes
    editor.addEventListener('selectionchange', () => {
      if (!this.isUpdatingContent) {
        this.updateToolbarDropdowns();
      }
    });
    
    // Update toolbar on click
    editor.addEventListener('click', () => {
      setTimeout(() => this.updateToolbarDropdowns(), 10);
    });
    
    // Update toolbar on keyup (arrow keys, etc.)
    editor.addEventListener('keyup', () => {
      if (!this.isUpdatingContent) {
        setTimeout(() => this.updateToolbarDropdowns(), 10);
      }
    });
    
    // Keyboard shortcuts
    this.keydownHandler = (e: KeyboardEvent) => {
      // Only handle shortcuts, don't interfere with normal typing
      // Ctrl+Z for undo
      if (e.ctrlKey && e.key === 'z' && !e.shiftKey) {
        e.preventDefault();
        this.undo();
        return;
      }
      // Ctrl+Y or Ctrl+Shift+Z for redo
      if ((e.ctrlKey && e.key === 'y') || (e.ctrlKey && e.shiftKey && e.key === 'z')) {
        e.preventDefault();
        this.redo();
        return;
      }
      // Ctrl+B for bold
      if (e.ctrlKey && e.key === 'b') {
        e.preventDefault();
        this.execCommand('bold');
        return;
      }
      // Ctrl+I for italic
      if (e.ctrlKey && e.key === 'i') {
        e.preventDefault();
        this.execCommand('italic');
        return;
      }
      // Ctrl+U for underline
      if (e.ctrlKey && e.key === 'u') {
        e.preventDefault();
        this.execCommand('underline');
        return;
      }
    };
    editor.addEventListener('keydown', this.keydownHandler);
    
    this.editorSetup = true;
  }

  /**
   * Save current editor state to history
   */
  private saveToHistory(): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const content = this.contentEditor.nativeElement.innerHTML;
    
    // Don't save if content hasn't changed
    if (this.editorHistory.length > 0 && this.editorHistory[this.historyIndex] === content) {
      return;
    }
    
    // Remove old future history if we're not at the end
    if (this.historyIndex < this.editorHistory.length - 1) {
      this.editorHistory = this.editorHistory.slice(0, this.historyIndex + 1);
    }
    
    // Add new state
    this.editorHistory.push(content);
    this.historyIndex = this.editorHistory.length - 1;
    
    // Limit history size
    if (this.editorHistory.length > this.maxHistorySize) {
      this.editorHistory.shift();
      this.historyIndex--;
    }
  }
  
  /**
   * Restore cursor position after content update
   */
  private restoreCursorPosition(position: number): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const editor = this.contentEditor.nativeElement;
    const selection = window.getSelection();
    if (!selection) return;
    
    try {
      const walker = document.createTreeWalker(
        editor,
        NodeFilter.SHOW_TEXT,
        null
      );
      
      let charCount = 0;
      let textNode: Node | null = null;
      let offset = 0;
      
      while (textNode = walker.nextNode()) {
        const nodeLength = textNode.textContent?.length || 0;
        const nextCharCount = charCount + nodeLength;
        
        if (position <= nextCharCount) {
          offset = position - charCount;
          textNode = textNode;
          break;
        }
        charCount = nextCharCount;
      }
      
      if (textNode && textNode.nodeType === Node.TEXT_NODE) {
        const range = document.createRange();
        const maxOffset = Math.min(offset, textNode.textContent?.length || 0);
        range.setStart(textNode, maxOffset);
        range.setEnd(textNode, maxOffset);
        selection.removeAllRanges();
        selection.addRange(range);
      } else {
        // Fallback: move to end
        const range = document.createRange();
        range.selectNodeContents(editor);
        range.collapse(false);
        selection.removeAllRanges();
        selection.addRange(range);
      }
    } catch (e) {
      // If restoration fails, just move cursor to end
      try {
        const range = document.createRange();
        range.selectNodeContents(editor);
        range.collapse(false);
        selection.removeAllRanges();
        selection.addRange(range);
      } catch (e2) {
        // Ignore errors
      }
    }
  }

  /**
   * Load blogs from MongoDB API only
   */
  loadBlogs(): void {
    this.isLoading = true;
    this.loadError = '';
    
    console.log('🔄 Loading blogs from MongoDB...');
    
    // Load from MongoDB API only - no JSON fallback
    this.apiService.getBlogs().subscribe({
      next: (data) => {
        console.log('✅ SUCCESS: Loaded blogs from MongoDB!');
        console.log(`📊 Total blogs: ${data.length}`);
        
        // Map MongoDB data to BlogPost interface
        this.allBlogs = data.map((blog: any, index: number) => this.mapBlogJSONtoBlogPost(blog, index));
        this.blogs = [...this.allBlogs];
        this.isLoading = false;
        
        // Debug: Log first few blogs to verify IDs
        console.log('📝 Processed blogs (first 5):');
        this.allBlogs.slice(0, 5).forEach((blog, idx) => {
          console.log(`  Blog ${idx + 1}: blog_id="${blog.blog_id}", id="${blog.id || blog.blog_id}", title="${blog.title?.substring(0, 40)}"`);
        });
        
        // Verify IDs are from MongoDB (should be NS002, NS016, etc., NOT B0001, B0002)
        const invalidIds = this.allBlogs.filter(b => b.blog_id && (b.blog_id.startsWith('B000') || b.blog_id.match(/^B\d+$/)));
        if (invalidIds.length > 0) {
          console.warn(`⚠️ WARNING: Found ${invalidIds.length} blogs with invalid IDs (B000X format):`, invalidIds.slice(0, 3).map(b => b.blog_id));
          console.warn('   These should be from MongoDB (NS002, NS016, etc.), not sample data!');
        }
        
        console.log('📝 Total processed blogs:', this.allBlogs.length);
      },
      error: (error: any) => {
        console.error('❌ ERROR loading blogs from MongoDB:', error);
        this.loadError = 'Không thể tải danh sách bài viết từ MongoDB';
        this.isLoading = false;
        // Don't fallback to JSON - only use MongoDB data
        this.allBlogs = [];
        this.blogs = [];
      }
    });
  }

  /**
   * Map BlogJSON to BlogPost interface
   */
  private mapBlogJSONtoBlogPost(blogJSON: any, index: number): BlogPost {
    // CRITICAL: Use the actual 'id' field from MongoDB (NS002, NS016, etc.)
    // MongoDB stores blog ID in the 'id' field, NOT in 'blog_id'
    // Priority: blogJSON.id > blogJSON.blog_id (fallback only if id doesn't exist)
    // IMPORTANT: blogJSON.id from MongoDB API should be like: NS002, NS016, NS015, etc.
    const mongoBlogId = blogJSON.id || blogJSON.blog_id || '';
    
    // Debug logging for first few blogs
    if (index < 5) {
      console.log(`📝 [Map Blog ${index + 1}]`, {
        'MongoDB id': blogJSON.id,
        'MongoDB blog_id': blogJSON.blog_id,
        'MongoDB _id': blogJSON._id,
        'Mapped blog_id': mongoBlogId,
        'Title': blogJSON.title?.substring(0, 40)
      });
      
      // Validate: MongoDB IDs should NOT be in B000X format
      if (mongoBlogId && (mongoBlogId.startsWith('B000') || mongoBlogId.match(/^B\d+$/))) {
        console.error(`❌ [Map Blog ${index + 1}] ERROR: Invalid ID format detected!`, {
          'Received ID': mongoBlogId,
          'Expected format': 'NS002, NS016, etc.',
          'Full blogJSON': blogJSON
        });
      }
    }
    
    // Use category from data or default to 'Sức khỏe'
    const category = blogJSON.categoryTag || 'Sức khỏe';
    
    // Get views from data or generate random
    const views = blogJSON.views || Math.floor(Math.random() * 5000) + 500;
    
    const createdDateValue = this.getBlogDateValue(blogJSON, ['pubDate', 'publishedAt', 'PublishedAt', 'createdAt', 'CreatedAt']);
    const updatedDateValue = this.getBlogDateValue(blogJSON, ['updatedAt', 'UpdatedAt', 'updated_at', 'ModifiedAt', 'modifiedAt'])
      || createdDateValue;
    const created_date = this.formatDate(createdDateValue);
    const updated_date = this.formatDate(updatedDateValue);
    
    // IMPORTANT: Use MongoDB 'id' field directly - this is the source of truth
    // Do NOT generate fallback IDs - if MongoDB doesn't have 'id', log warning
    if (!mongoBlogId) {
      console.warn(`⚠️ [Map Blog ${index + 1}] Blog missing ID field!`, blogJSON);
    }
    
    // CRITICAL: Ensure blog_id is set to MongoDB 'id' field - this is what displays in the table
    // Do NOT use any fallback or generated ID - only use what MongoDB provides
    // blogJSON.id from MongoDB should be like: NS002, NS016, NS015, etc.
    const finalBlogId = mongoBlogId || '';
    
    // Validate: If blog_id is empty, this is an error - MongoDB should always have 'id' field
    if (!finalBlogId) {
      console.error(`❌ [Map Blog ${index + 1}] CRITICAL: Blog missing ID!`, {
        blogJSON_id: blogJSON.id,
        blogJSON_blog_id: blogJSON.blog_id,
        blogJSON_title: blogJSON.title
      });
    }
    
    const result: BlogPost = {
      _id: blogJSON._id ? String(blogJSON._id) : '', // MongoDB _id (ObjectId) - must be from MongoDB
      blog_id: finalBlogId, // MongoDB 'id' field (NS002, NS016, etc.) - THIS IS WHAT DISPLAYS IN THE TABLE {{ blog.blog_id }}
      id: finalBlogId, // MongoDB 'id' field - use actual ID from MongoDB (same as blog_id)
      title: blogJSON.title || '',
      author: blogJSON.author || '',
      email: blogJSON.email || '',
      category: category,
      content: blogJSON.content || '',
      created_date: created_date,
      updated_date: updated_date,
      views: views,
      selected: false
    };
    
    return result;
  }

  /**
   * Load sample blogs as fallback
   */
  private loadSampleBlogs(): void {
    this.allBlogs = [
      {
        _id: '1',
        blog_id: 'B0001',
        title: 'Công thức nấu canh chua chuẩn vị Bắc',
        author: 'Thanh Thinh Tran',
        category: 'Công thức nấu ăn',
        content: 'Nội dung bài viết...',
        created_date: '20/10/2025',
        views: 1234,
        selected: false
      },
      {
        _id: '2',
        blog_id: 'B0002',
        title: 'Rau má có tác dụng gì với sức khỏe?',
        author: 'Thanh Thinh Tran',
        category: 'Sức khỏe',
        content: 'Nội dung bài viết...',
        created_date: '20/01/2025',
        views: 5689,
        selected: false
      }
    ];
    this.blogs = [...this.allBlogs];
  }

  /**
   * Format date to Vietnamese format
   */
  formatDate(dateStr: any): string {
    if (!dateStr) {
      return '---';
    }
    if (dateStr instanceof Date && !isNaN(dateStr.getTime())) {
      const day = String(dateStr.getDate()).padStart(2, '0');
      const month = String(dateStr.getMonth() + 1).padStart(2, '0');
      const year = dateStr.getFullYear();
      return `${day}/${month}/${year}`;
    }
    if (typeof dateStr === 'object' && dateStr.$date) {
      return this.formatDate(dateStr.$date);
    }
    const rawDate = String(dateStr).trim();
    if (!rawDate) {
      return '---';
    }
    if (rawDate.includes('/') && !rawDate.includes('NaN')) {
      return dateStr; // Already formatted
    }
    const date = new Date(rawDate);
    if (isNaN(date.getTime())) {
      return '---';
    }
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
  }

  private getBlogDateValue(blogJSON: any, keys: string[]): any {
    for (const key of keys) {
      const value = blogJSON?.[key];
      if (value !== undefined && value !== null && value !== '') {
        return value;
      }
    }
    return '';
  }

  /**
   * Toggle select all blogs
   */
  toggleSelectAll(): void {
    this.selectAll = !this.selectAll;
    this.blogs.forEach(blog => blog.selected = this.selectAll);
    this.updateSelectedCount();
  }

  /**
   * Toggle individual blog selection
   */
  toggleBlog(blog: BlogPost): void {
    blog.selected = !blog.selected;
    this.updateSelectedCount();
    this.selectAll = this.blogs.every(b => b.selected);
  }

  /**
   * Update selected count
   */
  updateSelectedCount(): void {
    this.selectedCount = this.blogs.filter(b => b.selected).length;
  }

  /**
   * Search blogs
   */
  searchBlogs(event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    
    if (!query || query.trim() === '') {
      this.blogs = [...this.allBlogs];
    } else {
      const searchTerm = query.toLowerCase().trim();
      this.blogs = this.allBlogs.filter(blog => {
        return (
          blog.title.toLowerCase().includes(searchTerm) ||
          blog.blog_id.toLowerCase().includes(searchTerm) ||
          blog.author.toLowerCase().includes(searchTerm) ||
          blog.category.toLowerCase().includes(searchTerm)
        );
      });
    }
    
    // Reset selection
    this.selectedCount = 0;
    this.selectAll = false;
  }

  /**
   * Edit selected blogs
   */
  editBlogs(): void {
    const selected = this.blogs.filter(b => b.selected);
    
    if (selected.length === 0) {
      this.notificationService.showWarning('Vui lòng chọn ít nhất 1 bài viết để chỉnh sửa');
      return;
    }

    if (selected.length === 1) {
      // Edit single blog - you can navigate to detail page
      console.log('Edit blog:', selected[0].blog_id);
      this.viewBlogDetail(selected[0]);
    } else {
      // Batch edit multiple blogs
      this.notificationService.showInfo(`Chỉnh sửa hàng loạt ${selected.length} bài viết`);
    }
  }

  /**
   * Delete selected blogs
   */
  deleteBlogs(): void {
    const selected = this.blogs.filter(b => b.selected);
    
    if (selected.length === 0) {
      this.notificationService.showWarning('Vui lòng chọn ít nhất 1 bài viết để xóa');
      return;
    }

    this.showConfirmation(
      `Bạn có chắc chắn muốn xóa ${selected.length} bài viết?`,
      () => {
        // Get blog IDs to delete (use id field from MongoDB, not _id)
        const deletePromises = selected.map(blog => {
          // Use blog.blog_id which is mapped from MongoDB's 'id' field (NS002, NS016, etc.)
          // Fallback to blog._id if blog_id is not available
          const blogId = blog.blog_id || blog.id || blog._id;
          console.log(`🗑️ Deleting blog:`, { blogId, blog_id: blog.blog_id, id: blog.id, _id: blog._id, title: blog.title });
          return this.apiService.deleteBlog(blogId).toPromise().then(response => {
            console.log(`✅ Delete API response for ${blogId}:`, response);
            return response;
          }).catch(error => {
            console.error(`❌ Delete API error for ${blogId}:`, error);
            throw error;
          });
        });

        Promise.all(deletePromises).then(results => {
          console.log('📊 Delete results:', results);
          
          // Filter out null results and check for errors
          const validResults = results.filter(r => r !== null && r !== undefined);
          const successResults = validResults.filter(r => {
            if (r && typeof r === 'object') {
              const result = r as any;
              return result.success !== false;
            }
            return true;
          });
          
          const successCount = successResults.length;
          const failedCount = validResults.length - successCount;
          
          console.log(`✅ Deleted ${successCount} blogs successfully`);
          if (failedCount > 0) {
            console.warn(`⚠️ Failed to delete ${failedCount} blogs`);
          }
          
          // Reload blogs from MongoDB to get updated list
          this.loadBlogs();
          
          this.selectedCount = 0;
          this.selectAll = false;
          
          if (failedCount > 0) {
            this.notificationService.showError(`Đã xóa ${successCount} bài viết, ${failedCount} bài viết lỗi`);
          } else {
            this.notificationService.showSuccess(`Đã xóa ${successCount} bài viết thành công!`);
          }
          this.closeConfirmModal();
        }).catch(error => {
          console.error('❌ Error deleting blogs:', error);
          console.error('❌ Error details:', {
            status: error.status,
            statusText: error.statusText,
            message: error.message,
            error: error.error,
            url: error.url
          });
          
          const errorMessage = error.error?.message || error.error?.error || error.message || 'Lỗi không xác định';
          this.notificationService.showError('Lỗi khi xóa bài viết: ' + errorMessage);
          
          // Still reload to sync with server
          this.loadBlogs();
          this.closeConfirmModal();
        });
      }
    );
  }

  /**
   * Add new blog
   */
  addNewBlog(): void {
    // Create a new empty blog object
    const today = new Date();
    const day = String(today.getDate()).padStart(2, '0');
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const year = today.getFullYear();
    const formattedDate = `${day}/${month}/${year}`;
    
    // Generate next blog_id based on existing IDs in MongoDB
    // Check existing IDs to determine format (NSXXX or BXXXX)
    const existingIds = this.allBlogs
      .map(b => b.blog_id || b.id)
      .filter((id): id is string => !!id); // Type guard to ensure id is string, not undefined
    let blogId = '';
    
    if (existingIds.length > 0) {
      // Check if any existing ID starts with 'NS'
      const hasNSFormat = existingIds.some(id => id.startsWith('NS'));
      
      if (hasNSFormat) {
        // Use NS format - find max NS number
        const nsNumbers = existingIds
          .filter(id => id.startsWith('NS'))
          .map(id => {
            const numStr = id.replace('NS', '');
            return parseInt(numStr);
          })
          .filter(num => !isNaN(num));
        const nextNSNumber = nsNumbers.length > 0 ? Math.max(...nsNumbers) + 1 : 1;
        blogId = `NS${String(nextNSNumber).padStart(3, '0')}`;
      } else {
        // Use B format
        const bNumbers = existingIds
          .filter(id => id.startsWith('B'))
          .map(id => {
            const numStr = id.replace('B', '');
            return parseInt(numStr);
          })
          .filter(num => !isNaN(num));
        const nextBNumber = bNumbers.length > 0 ? Math.max(...bNumbers) + 1 : 1;
        blogId = `B${String(nextBNumber).padStart(4, '0')}`;
      }
    } else {
      // No existing blogs, default to NS format (matching MongoDB format)
      blogId = 'NS001';
    }
    
    this.currentBlog = {
      _id: '', // Will be generated by backend
      blog_id: blogId,
      title: '',
      author: '',
      email: '',
      category: 'Sức khỏe',
      content: '',
      created_date: formattedDate,
      views: 0,
      selected: false
    };
    
    this.isNewBlog = true;
    this.viewMode = 'detail';
    // Don't open popup, show form directly
    this.showEditPopup = false;
    
    // Reset editor history
    this.editorHistory = [];
    this.historyIndex = -1;
    
    // Setup editor after view updates
    setTimeout(() => {
      if (this.contentEditor?.nativeElement) {
        const editor = this.contentEditor.nativeElement;
        // Clear content if needed
        if (editor.innerHTML.trim() !== '') {
          this.isUpdatingContent = true;
          editor.innerHTML = '';
          this.isUpdatingContent = false;
        }
        this.setupEditor();
        editor.focus();
      }
    }, 100);
  }

  /**
   * View blog detail
   */
  viewBlogDetail(blog: BlogPost): void {
    console.log('View blog:', blog.blog_id);
    this.currentBlog = { ...blog }; // Create a copy to avoid reference issues
    // Ensure email is set
    if (!this.currentBlog.email) {
      this.currentBlog.email = '';
    }
    this.isNewBlog = false;
    this.viewMode = 'detail';
    
    // Reset editor history
    this.editorHistory = [];
    this.historyIndex = -1;
    
    // Wait a bit for view to update, then setup editor
    setTimeout(() => {
      // Setup editor - it will set content if needed
      if (this.contentEditor?.nativeElement) {
        this.setupEditor();
      }
      
      // Trigger change detection
      this.cdr.detectChanges();
    }, 100);
  }

  /**
   * Reload current blog from MongoDB after update
   */
  reloadCurrentBlog(blogId: string): void {
    if (!blogId) {
      console.warn('⚠️ Cannot reload blog: blogId is missing');
      return;
    }
    
    console.log(`🔄 Reloading blog ${blogId} from MongoDB...`);
    
    // Load blog from API
    this.http.get<any>(`${environment.apiUrl}/blogs/${blogId}`).subscribe({
      next: (response: any) => {
        const updatedBlog = response?.success && response?.data ? response.data : response;
        if (updatedBlog) {
          console.log('✅ Reloaded blog from MongoDB:', updatedBlog);
          
          // Update currentBlog with fresh data from MongoDB
          if (this.currentBlog) {
            // Update all fields from MongoDB
            // CRITICAL: Update _id, id, and blog_id correctly
            // MongoDB _id is the ObjectId
            this.currentBlog._id = updatedBlog._id ? String(updatedBlog._id) : this.currentBlog._id;
            // MongoDB 'id' field is the source of truth (NS002, NS016, etc.)
            const mongoId = updatedBlog.id || updatedBlog._id?.toString() || this.currentBlog.blog_id;
            this.currentBlog.blog_id = mongoId; // blog_id should be MongoDB 'id' field
            this.currentBlog.id = mongoId; // id field should also be MongoDB 'id' field
            
            this.currentBlog.title = updatedBlog.title || this.currentBlog.title;
            this.currentBlog.author = updatedBlog.author || this.currentBlog.author;
            this.currentBlog.email = updatedBlog.email || this.currentBlog.email || '';
            this.currentBlog.category = updatedBlog.categoryTag || this.currentBlog.category;
            this.currentBlog.content = updatedBlog.content || this.currentBlog.content;
            const createdDateValue = this.getBlogDateValue(updatedBlog, ['pubDate', 'publishedAt', 'PublishedAt', 'createdAt', 'CreatedAt']);
            const updatedDateValue = this.getBlogDateValue(updatedBlog, ['updatedAt', 'UpdatedAt', 'updated_at', 'ModifiedAt', 'modifiedAt']) || createdDateValue;
            this.currentBlog.created_date = createdDateValue ? this.formatDate(createdDateValue) : this.currentBlog.created_date;
            this.currentBlog.updated_date = updatedDateValue ? this.formatDate(updatedDateValue) : this.currentBlog.updated_date;
            this.currentBlog.views = updatedBlog.views || this.currentBlog.views || 0;
            
            
            // Update editor content if editor exists
            if (this.contentEditor?.nativeElement) {
              this.isUpdatingContent = true; // Prevent triggering input handler
              this.contentEditor.nativeElement.innerHTML = this.currentBlog.content || '';
              setTimeout(() => {
                this.isUpdatingContent = false;
              }, 100);
            }
            
            console.log('✅ Current blog updated with MongoDB data');
            this.cdr.detectChanges();
          }
        } else {
          console.warn('⚠️ Blog reload response missing data:', response);
        }
      },
      error: (error: any) => {
        console.error('❌ Error reloading blog from MongoDB:', error);
        // Don't show error to user - just log it
      }
    });
  }

  /**
   * Format date from MongoDB to display format
   */
  private formatDateFromMongoDB(dateInput: any): string {
    if (!dateInput) return '';
    
    let date: Date;
    if (dateInput instanceof Date) {
      date = dateInput;
    } else if (typeof dateInput === 'string') {
      date = new Date(dateInput);
    } else if (dateInput.$date) {
      date = new Date(dateInput.$date);
    } else {
      return '';
    }
    
    if (isNaN(date.getTime())) {
      return '';
    }
    
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}-${month}-${year}`;
  }

  /**
   * Display popup notification
   * @param message - Message to display
   * @param type - Type of popup (success, error, info)
   * @param onCloseCallback - Optional callback to execute when user clicks OK
   */
  displayPopup(message: string, type: 'success' | 'error' | 'info' = 'success', onCloseCallback?: () => void): void {
    // Clear any existing auto-close timeout
    if (this.popupAutoCloseTimeout) {
      clearTimeout(this.popupAutoCloseTimeout);
      this.popupAutoCloseTimeout = undefined;
    }
    
    this.popupMessage = message;
    this.popupType = type;
    this.showPopup = true;
    this.pendingReloadAfterPopup = onCloseCallback; // Store callback for when user clicks OK
    
    // Auto-close popup after 8 seconds if user doesn't click OK
    // This gives user plenty of time to see and click the popup
    // Note: Auto-close does NOT trigger reload, only manual OK click does
    this.popupAutoCloseTimeout = setTimeout(() => {
      if (this.showPopup) {
        // Clear callback on auto-close (don't reload)
        this.pendingReloadAfterPopup = undefined;
        this.closePopup();
      }
    }, 8000);
  }

  /**
   * Close popup notification
   * If there's a pending reload callback, execute it (user clicked OK)
   */
  closePopup(): void {
    // Clear auto-close timeout if exists
    if (this.popupAutoCloseTimeout) {
      clearTimeout(this.popupAutoCloseTimeout);
      this.popupAutoCloseTimeout = undefined;
    }
    
    // Execute pending reload callback if exists (user clicked OK)
    if (this.pendingReloadAfterPopup) {
      const callback = this.pendingReloadAfterPopup;
      this.pendingReloadAfterPopup = undefined; // Clear before executing
      this.showPopup = false;
      this.popupMessage = '';
          
      // Execute callback after closing popup
          setTimeout(() => {
        callback();
      }, 100);
      return;
    }
    
    // No callback, just close popup (auto-close)
    this.showPopup = false;
    this.popupMessage = '';
      }


  /**
   * Navigate to blog list
   */
  navigateToBlogList(): void {
    this.viewMode = 'list';
    this.currentBlog = null;
    this.isNewBlog = false;
  }

  /**
   * Back to blog list
   */
  backToList(): void {
    this.viewMode = 'list';
    this.currentBlog = null;
    this.isNewBlog = false;
    
    // Reset editor state
    this.editorSetup = false;
    this.editorHistory = [];
    this.historyIndex = -1;
    
    // Remove event listeners if editor exists
    if (this.contentEditor?.nativeElement && this.inputHandler && this.keydownHandler) {
      this.contentEditor.nativeElement.removeEventListener('input', this.inputHandler);
      this.contentEditor.nativeElement.removeEventListener('keydown', this.keydownHandler);
    }
  }

  /**
   * Delete current blog
   */
  deleteCurrentBlog(): void {
    if (!this.currentBlog) return;
    
    this.showConfirmation(
      `Bạn có chắc chắn muốn xóa bài viết "${this.currentBlog.title}"?`,
      () => {
        if (!this.currentBlog) return;
        
        // Use blog.blog_id which is mapped from MongoDB's 'id' field (NS002, NS016, etc.)
        // Fallback to blog._id if blog_id is not available
        const blogId = this.currentBlog.blog_id || this.currentBlog.id || this.currentBlog._id;
        console.log(`🗑️ Deleting blog:`, { blogId, blog_id: this.currentBlog.blog_id, id: this.currentBlog.id, _id: this.currentBlog._id, title: this.currentBlog.title });
        
        this.apiService.deleteBlog(blogId).subscribe({
          next: (response: any) => {
            console.log(`✅ Delete API response for ${blogId}:`, response);
            
            if (response.success !== false) {
              this.notificationService.showSuccess('Đã xóa bài viết thành công!');
              
              // Reload blogs from MongoDB to get updated list
              this.loadBlogs();
              
              this.backToList();
            } else {
              this.notificationService.showError(response.message || 'Lỗi khi xóa bài viết');
            }
            this.closeConfirmModal();
          },
          error: (error: any) => {
            console.error(`❌ Delete API error for ${blogId}:`, error);
            console.error('❌ Error details:', {
              status: error.status,
              statusText: error.statusText,
              message: error.message,
              error: error.error,
              url: error.url
            });
            
            const errorMessage = error.error?.message || error.error?.error || error.message || 'Lỗi không xác định';
            this.notificationService.showError('Lỗi khi xóa bài viết: ' + errorMessage);
            
            // Still reload to sync with server
            this.loadBlogs();
            this.closeConfirmModal();
          }
        });
      }
    );
  }

  /**
   * Show confirmation dialog
   */
  showConfirmation(message: string, callback: () => void): void {
    this.confirmMessage = message;
    this.confirmCallback = callback;
    this.showConfirmDialog = true;
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
   * Close confirm modal
   */
  closeConfirmModal(): void {
    this.showConfirmDialog = false;
    this.confirmCallback = null;
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
   * Save current blog changes
   */
  saveCurrentBlog(): void {
    if (!this.currentBlog) return;
    
    // Validate required fields
    if (!this.currentBlog.title || this.currentBlog.title.trim() === '') {
      this.notificationService.showWarning('Vui lòng nhập tiêu đề bài viết!');
      return;
    }
    
    if (!this.currentBlog.author || this.currentBlog.author.trim() === '') {
      this.notificationService.showWarning('Vui lòng nhập tên tác giả!');
      return;
    }
    
    // Get content from editor if available
    let blogContent = this.currentBlog.content || '';
    if (this.contentEditor?.nativeElement) {
      blogContent = this.contentEditor.nativeElement.innerHTML || this.currentBlog.content || '';
      // Update currentBlog.content with editor content
      this.currentBlog.content = blogContent;
    }
    
    if (!blogContent || blogContent.trim() === '' || blogContent.trim() === '<br>') {
      this.notificationService.showWarning('Vui lòng nhập nội dung bài viết!');
      return;
    }
    
    // Map BlogPost interface to backend format
    // CRITICAL: Use blog_id or id field (NS002, NS016, etc.), NOT _id (MongoDB ObjectId)
    // Backend API expects 'id' field, not MongoDB ObjectId
    const blogId = this.currentBlog.id || this.currentBlog.blog_id || this.currentBlog._id;
    const blogData: any = {
      id: blogId,
      title: this.currentBlog.title,
      author: this.currentBlog.author,
      email: this.currentBlog.email || '',
      categoryTag: this.currentBlog.category,
      content: blogContent, // Use content from editor
      // Only send if manually set (array with values)
      img: '', // Có thể cần thêm logic để lấy ảnh từ content
      excerpt: blogContent.replace(/<[^>]*>/g, '').substring(0, 200) || '', // Tạo excerpt từ content (remove HTML tags)
      pubDate: this.currentBlog.created_date ? this.parseDateToISO(this.currentBlog.created_date) : new Date(),
      status: 'Active',
      views: this.currentBlog.views || 0
    };
    
    console.log('💾 Saving blog to MongoDB:', {
      id: blogData.id,
      title: blogData.title,
      contentLength: blogData.content?.length || 0,
      blog_id: this.currentBlog.blog_id,
      id_field: this.currentBlog.id,
      _id: this.currentBlog._id
    });
    
    if (this.isNewBlog) {
      // Tạo blog mới qua API
      this.apiService.createBlog(blogData).subscribe({
        next: (response: any) => {
          console.log('✅ Blog created successfully:', response);
          
          
          // Display popup with callback to reload when user clicks OK
          this.displayPopup('Thao tác thành công', 'success', () => {
            // This callback executes when user clicks OK
            console.log('🔄 User clicked OK, reloading data...');
          
          // Reload blogs để lấy blog mới với ngày cập nhật
          this.loadBlogs();
          
            // Navigate back to list
            setTimeout(() => {
          this.backToList();
            }, 300);
          });
        },
        error: (error: any) => {
          console.error('❌ Error creating blog:', error);
          this.displayPopup('Lỗi khi tạo bài viết: ' + (error.error?.message || error.message), 'error');
        }
      });
    } else {
      // Cập nhật blog hiện có qua API
      // CRITICAL: Use blog_id or id field (NS002, NS016, etc.), NOT _id (MongoDB ObjectId)
      // Backend API expects 'id' field, not MongoDB ObjectId
      const updateBlogId = this.currentBlog.id || this.currentBlog.blog_id || this.currentBlog._id;
      console.log('🔄 Updating blog with ID:', updateBlogId);
      console.log('   - blog_id:', this.currentBlog.blog_id);
      console.log('   - id:', this.currentBlog.id);
      console.log('   - _id:', this.currentBlog._id);
      this.apiService.updateBlog(updateBlogId, blogData).subscribe({
        next: (response: any) => {
          console.log('✅ Blog updated successfully:', response);
          console.log('📋 Response data:', response.data);
          
          // Display popup with callback to reload when user clicks OK
          this.displayPopup('Thao tác thành công', 'success', () => {
            // This callback executes when user clicks OK
            console.log('🔄 User clicked OK, reloading data...');
            
            // Reload blogs to get updated data
          this.loadBlogs();
    
            // Reload current blog from MongoDB to get latest data
            // This ensures the detail view shows the updated content
            if (this.currentBlog && this.viewMode === 'detail') {
              // Use blog_id or id field (NS002, NS016, etc.), NOT _id (MongoDB ObjectId)
              const reloadBlogId = this.currentBlog.id || this.currentBlog.blog_id || this.currentBlog._id;
              console.log('🔄 Reloading current blog from MongoDB after update...');
              setTimeout(() => {
                this.reloadCurrentBlog(reloadBlogId);
              }, 300);
            } else {
              // Navigate back to list
    this.backToList();
            }
          });
        },
        error: (error: any) => {
          console.error('❌ Error updating blog:', error);
          this.displayPopup('Lỗi khi cập nhật bài viết: ' + (error.error?.message || error.message), 'error');
        }
      });
    }
  }

  /**
   * Open edit popup
   */
  openEditPopup(): void {
    this.showEditPopup = true;
    this.cdr.detectChanges();
  }

  /**
   * Close edit popup
   */
  closeEditPopup(): void {
    this.showEditPopup = false;
    this.cdr.detectChanges();
  }

  /**
   * Save from popup and close
   */
  saveFromPopup(): void {
    if (!this.currentBlog) return;
    
    // Validate required fields
    if (!this.currentBlog.title || this.currentBlog.title.trim() === '') {
      this.notificationService.showWarning('Vui lòng nhập tiêu đề bài viết!');
      return;
    }
    
    if (!this.currentBlog.author || this.currentBlog.author.trim() === '') {
      this.notificationService.showWarning('Vui lòng nhập tên tác giả!');
      return;
    }
    
    // Get content from editor if available
    let blogContent = this.currentBlog.content || '';
    if (this.contentEditor?.nativeElement) {
      blogContent = this.contentEditor.nativeElement.innerHTML || this.currentBlog.content || '';
      // Update currentBlog.content with editor content
      this.currentBlog.content = blogContent;
    }
    
    if (!blogContent || blogContent.trim() === '' || blogContent.trim() === '<br>') {
      this.notificationService.showWarning('Vui lòng nhập nội dung bài viết!');
      return;
    }
    
    // Map BlogPost interface to backend format
    // CRITICAL: Use blog_id or id field (NS002, NS016, etc.), NOT _id (MongoDB ObjectId)
    // Backend API expects 'id' field, not MongoDB ObjectId
    const blogId = this.currentBlog.id || this.currentBlog.blog_id || this.currentBlog._id;
    const blogData: any = {
      id: blogId,
      title: this.currentBlog.title,
      author: this.currentBlog.author,
      email: this.currentBlog.email || '',
      categoryTag: this.currentBlog.category,
      content: blogContent, // Use content from editor
      // Only send if manually set (array with values)
      img: '', // Có thể cần thêm logic để lấy ảnh từ content
      excerpt: blogContent.replace(/<[^>]*>/g, '').substring(0, 200) || '', // Tạo excerpt từ content (remove HTML tags)
      pubDate: this.currentBlog.created_date ? this.parseDateToISO(this.currentBlog.created_date) : new Date(),
      status: 'Active',
      views: this.currentBlog.views || 0
    };
    
    console.log('💾 Saving blog from popup to MongoDB:', {
      id: blogData.id,
      title: blogData.title,
      contentLength: blogData.content?.length || 0,
      blog_id: this.currentBlog.blog_id,
      id_field: this.currentBlog.id,
      _id: this.currentBlog._id
    });
    
    if (this.isNewBlog) {
      // Tạo blog mới qua API
      this.apiService.createBlog(blogData).subscribe({
        next: (response: any) => {
          console.log('✅ Blog created successfully:', response);
          
          
          // Close edit popup immediately
      this.showEditPopup = false;
      // Don't go back to list, stay on detail view
      this.isNewBlog = false;
          
          // Display popup with callback to reload when user clicks OK
          this.displayPopup('Thao tác thành công', 'success', () => {
            // This callback executes when user clicks OK
            console.log('🔄 User clicked OK, reloading data...');
            
            // Reload blogs để lấy blog mới với ngày cập nhật
            this.loadBlogs();
            
      this.cdr.detectChanges();
          });
        },
        error: (error: any) => {
          console.error('❌ Error creating blog:', error);
          this.displayPopup('Lỗi khi tạo bài viết: ' + (error.error?.message || error.message), 'error');
        }
      });
    } else {
      // Cập nhật blog hiện có qua API
      // CRITICAL: Use blog_id or id field (NS002, NS016, etc.), NOT _id (MongoDB ObjectId)
      // Backend API expects 'id' field, not MongoDB ObjectId
      const updateBlogId = this.currentBlog.id || this.currentBlog.blog_id || this.currentBlog._id;
      console.log('🔄 Updating blog from popup with ID:', updateBlogId);
      console.log('   - blog_id:', this.currentBlog.blog_id);
      console.log('   - id:', this.currentBlog.id);
      console.log('   - _id:', this.currentBlog._id);
      this.apiService.updateBlog(updateBlogId, blogData).subscribe({
        next: (response: any) => {
          // console.log('✅ Blog updated successfully:', response);
          
          // Update currentBlog with response data
          if (response.data && this.currentBlog) {
            // console.log('📋 Response data:', response.data);
          }
          
          // Close edit popup immediately
          this.showEditPopup = false;
          
          // Display popup with callback to reload when user clicks OK
          this.displayPopup('Thao tác thành công', 'success', () => {
            // This callback executes when user clicks OK
            // console.log('🔄 User clicked OK, reloading data...');
            
            // Reload blogs to get updated data
          this.loadBlogs();
          
            // Reload current blog from MongoDB to get latest data
            // This ensures the detail view shows the updated content
            if (this.currentBlog && this.viewMode === 'detail') {
              // Use blog_id or id field (NS002, NS016, etc.), NOT _id (MongoDB ObjectId)
              const reloadBlogId = this.currentBlog.id || this.currentBlog.blog_id || this.currentBlog._id;
              console.log('🔄 Reloading current blog from MongoDB after update...');
              setTimeout(() => {
                this.reloadCurrentBlog(reloadBlogId);
              }, 300);
            }
          });
          
      this.cdr.detectChanges();
        },
        error: (error: any) => {
          console.error('❌ Error updating blog:', error);
          this.displayPopup('Lỗi khi cập nhật bài viết: ' + (error.error?.message || error.message), 'error');
        }
      });
    }
  }

  /**
   * Filter blogs by category
   */
  filterByCategory(category: string): void {
    if (category === 'all') {
      this.blogs = [...this.allBlogs];
    } else {
      this.blogs = this.allBlogs.filter(b => b.category === category);
    }
  }

  /**
   * Sort blogs based on selected option
   */
  /**
   * Sort by date
   */
  sortByDate(order: 'asc' | 'desc' = 'desc'): void {
    this.currentSortBy = 'date';
    this.currentSortOrder = order;
    
    this.blogs.sort((a, b) => {
      const dateA = a.updated_date ? this.parseDate(a.updated_date) : 0;
      const dateB = b.updated_date ? this.parseDate(b.updated_date) : 0;
      
      return order === 'asc' ? dateA - dateB : dateB - dateA;
    });
    
    console.log(`📊 Sorted blogs by date: ${order}`);
    this.closeSortDropdown();
  }

  /**
   * Sort by views
   */
  sortByViews(order: 'asc' | 'desc' = 'desc'): void {
    this.currentSortBy = 'views';
    this.currentSortOrder = order;
    
    this.blogs.sort((a, b) => {
      const viewsA = a.views || 0;
      const viewsB = b.views || 0;
      
      return order === 'asc' ? viewsA - viewsB : viewsB - viewsA;
    });
    
    console.log(`📊 Sorted blogs by views: ${order}`);
    this.closeSortDropdown();
  }

  /**
   * Toggle sort order (asc/desc)
   */
  toggleSortOrder(): void {
    const newOrder = this.currentSortOrder === 'asc' ? 'desc' : 'asc';
    
    if (this.currentSortBy === 'date') {
      this.sortByDate(newOrder);
    } else if (this.currentSortBy === 'views') {
      this.sortByViews(newOrder);
    }
  }

  /**
   * Toggle sort dropdown
   */
  toggleSortDropdown(event: Event): void {
    event.stopPropagation();
    this.showSortDropdown = !this.showSortDropdown;
    console.log('🔄 Toggle dropdown:', this.showSortDropdown);
    this.cdr.detectChanges();
  }

  /**
   * Close sort dropdown
   */
  closeSortDropdown(): void {
    this.showSortDropdown = false;
    this.cdr.detectChanges();
  }

  /**
   * Parse date string DD/MM/YYYY to timestamp
   */
  private parseDate(dateString: string): number {
    if (!dateString) return 0;
    
    const parts = dateString.split('/');
    if (parts.length === 3) {
      const day = parseInt(parts[0], 10);
      const month = parseInt(parts[1], 10) - 1; // Month is 0-indexed
      const year = parseInt(parts[2], 10);
      return new Date(year, month, day).getTime();
    }
    
    return 0;
  }

  /**
   * Parse date string DD/MM/YYYY to ISO Date object
   */
  private parseDateToISO(dateString: string): Date {
    if (!dateString) return new Date();
    
    const parts = dateString.split('/');
    if (parts.length === 3) {
      const day = parseInt(parts[0], 10);
      const month = parseInt(parts[1], 10) - 1; // Month is 0-indexed
      const year = parseInt(parts[2], 10);
      return new Date(year, month, day);
    }
    
    return new Date();
  }

  /**
   * Close dropdowns when clicking outside
   */
  closeDropdowns(event: Event): void {
    // Can be used for dropdown functionality if needed
  }
  
  /**
   * Open date picker when clicking calendar icon
   */
  openDatePicker(): void {
    if (this.currentBlog && this.currentBlog.created_date) {
      // Parse existing date if available
      const dateParts = this.currentBlog.created_date.split('/');
      if (dateParts.length === 3) {
        this.selectedDate = new Date(parseInt(dateParts[2]), parseInt(dateParts[1]) - 1, parseInt(dateParts[0]));
      }
    }
    this.showDatePicker = !this.showDatePicker;
    this.cdr.detectChanges();
  }
  
  /**
   * Select date from date picker
   */
  selectDate(date: Date): void {
    this.selectedDate = date;
    if (this.currentBlog) {
      const day = String(date.getDate()).padStart(2, '0');
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const year = date.getFullYear();
      this.currentBlog.created_date = `${day}/${month}/${year}`;
    }
    this.showDatePicker = false;
    this.cdr.detectChanges();
  }
  
  /**
   * Get current month dates for date picker
   */
  getDatePickerDates(): Date[] {
    const year = this.selectedDate.getFullYear();
    const month = this.selectedDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const dates: Date[] = [];
    
    // Add dates from previous month to fill first week
    const startDay = firstDay.getDay();
    for (let i = startDay - 1; i >= 0; i--) {
      const date = new Date(year, month, -i);
      dates.push(date);
    }
    
    // Add current month dates
    for (let day = 1; day <= lastDay.getDate(); day++) {
      dates.push(new Date(year, month, day));
    }
    
    // Add dates from next month to fill last week
    const remainingDays = 42 - dates.length; // 6 weeks * 7 days
    for (let day = 1; day <= remainingDays; day++) {
      dates.push(new Date(year, month + 1, day));
    }
    
    return dates;
  }
  
  /**
   * Check if date is today
   */
  isToday(date: Date): boolean {
    const today = new Date();
    return date.getDate() === today.getDate() &&
           date.getMonth() === today.getMonth() &&
           date.getFullYear() === today.getFullYear();
  }
  
  /**
   * Check if date is selected
   */
  isSelectedDate(date: Date): boolean {
    return date.getDate() === this.selectedDate.getDate() &&
           date.getMonth() === this.selectedDate.getMonth() &&
           date.getFullYear() === this.selectedDate.getFullYear();
  }
  
  /**
   * Check if date is current month
   */
  isCurrentMonth(date: Date): boolean {
    return date.getMonth() === this.selectedDate.getMonth();
  }
  
  /**
   * Navigate to previous month
   */
  previousMonth(): void {
    const newDate = new Date(this.selectedDate);
    newDate.setMonth(newDate.getMonth() - 1);
    this.selectedDate = newDate;
    this.cdr.detectChanges();
  }
  
  /**
   * Navigate to next month
   */
  nextMonth(): void {
    const newDate = new Date(this.selectedDate);
    newDate.setMonth(newDate.getMonth() + 1);
    this.selectedDate = newDate;
    this.cdr.detectChanges();
  }
  
  /**
   * Get month name in Vietnamese
   */
  getMonthName(month: number): string {
    const months = ['Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6',
                   'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'];
    return months[month];
  }
  
  /**
   * Get day names in Vietnamese
   */
  getDayNames(): string[] {
    return ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
  }

  /**
   * Load product categories and subcategories for hashtag generation
   */
  loadProductCategories(): void {
    console.log('🔄 Loading product categories...');
    
    // Try API first, then fallback to JSON
    this.apiService.getProducts().subscribe({
      next: (products) => {
        console.log(`✅ Loaded ${products.length} products from API`);
        this.buildCategoryMap(products);
      },
      error: (error: any) => {
        console.warn('⚠️ API failed, trying JSON file...', error);
        // Fallback to JSON
        this.http.get<any[]>('data/products.json').subscribe({
          next: (products) => {
            console.log(`✅ Loaded ${products.length} products from JSON`);
            this.buildCategoryMap(products);
          },
          error: (jsonError) => {
            console.error('❌ Error loading products from JSON:', jsonError);
            // Try alternative path
            this.http.get<any[]>('data/product.json').subscribe({
              next: (products) => {
                console.log(`✅ Loaded ${products.length} products from product.json`);
                this.buildCategoryMap(products);
              },
              error: (finalError) => {
                console.error('❌ Error loading products:', finalError);
              }
            });
          }
        });
      }
    });
  }

  /**
   * Build category to keywords map from products
   */
  private buildCategoryMap(products: any[]): void {
    // Clear existing map
    this.categoryToSubcategories.clear();
    
    products.forEach(product => {
      if (product.category) {
        // Split by comma in case of multiple categories
        const categories = product.category.split(',').map((c: string) => c.trim().toLowerCase());
        
        categories.forEach((cat: string) => {
          if (!this.categoryToSubcategories.has(cat)) {
            this.categoryToSubcategories.set(cat, new Set<string>());
          }
          
          const keywordsSet = this.categoryToSubcategories.get(cat)!;
          
          // Add subcategories
          if (product.subcategory) {
            const subcategories = product.subcategory.split(',').map((s: string) => s.trim().toLowerCase());
            subcategories.forEach((subcat: string) => {
              if (subcat && subcat.length > 1) {
                keywordsSet.add(subcat);
              }
            });
          }
          
          // Extract keywords from product_name
          if (product.product_name) {
            const productName = product.product_name.toLowerCase();
            
            // Common fruit/vegetable/food names to extract
            const keywords = [
              // Trái cây (Fruits)
              'cam', 'quýt', 'bưởi', 'dâu', 'dâu tây', 'kiwi', 'ổi', 'xoài', 'thanh long', 
              'dưa', 'táo', 'lê', 'chuối', 'nho', 'măng cụt', 'chôm chôm', 'nhãn', 'vải',
              'đu đủ', 'dưa hấu', 'dưa lưới', 'dưa gang', 'mận', 'mơ', 'anh đào', 'cherry',
              'việt quất', 'quả mọng', 'dâu đen', 'mâm xôi', 'hồng', 'bơ',
              
              // Rau củ (Vegetables)
              'cà chua', 'cà rót', 'cà pháo', 'rau', 'súp lơ', 'bông cải', 'cải', 'bắp',
              'khoai', 'khoai lang', 'khoai tây', 'củ', 'hành', 'tỏi', 'gừng', 'sả', 'ớt',
              'rau bina', 'cải xoăn', 'cải bó xôi', 'cải thảo', 'rau muống', 'rau đay',
              'cần tây', 'dưa chuột', 'dưa leo', 'bí đỏ', 'bí đao', 'mướp đắng', 'mướp',
              'củ dền', 'su hào', 'cà rốt', 'củ cải', 'rau má', 'rau húng', 'ngò',
              'cải xanh', 'bắp cải', 'súp lơ xanh', 'súp lơ trắng', 'cà tím',
              
              // Các loại hạt (Nuts & Seeds)
              'hạnh nhân', 'óc chó', 'hạt lanh', 'hạt chia', 'đậu', 'đậu đen', 'đậu lăng',
              'đậu hũ', 'đậu nành', 'đậu xanh', 'đậu đỏ', 'hạt điều', 'hạt hướng dương',
              
              // Ngũ cốc (Grains)
              'yến mạch', 'ngô', 'bắp', 'gạo', 'lúa mạch',
              
              // Đồ uống (Beverages)
              'cacao', 'ca cao', 'cà phê', 'trà', 'chanh',
              
              // Nấm (Mushrooms)
              'nấm', 'nấm hương', 'nấm rơm', 'nấm mỡ',
              
              // Chất dinh dưỡng và hợp chất (Nutrients)
              'vitamin', 'protein', 'chất xơ', 'omega', 'omega-3', 'omega-6',
              'papain', 'lycopene', 'lutein', 'zeaxanthin', 'glucosinolates',
              'flavonoids', 'betalains', 'pectin', 'curcumin', 'nitrat',
              
              // Các từ khóa khác (Other keywords)
              'salad', 'nghệ', 'húng quế', 'rau thơm'
            ];
            
            // Check for 2-word combinations first (to match "dâu tây" before "dâu")
            const twoWordKeywords = keywords.filter(k => k.includes(' '));
            twoWordKeywords.forEach(keyword => {
              if (productName.includes(keyword)) {
                keywordsSet.add(keyword);
              }
            });
            
            // Then check single words
            const singleWordKeywords = keywords.filter(k => !k.includes(' '));
            singleWordKeywords.forEach(keyword => {
              if (productName.includes(keyword)) {
                keywordsSet.add(keyword);
              }
            });
            
            // Also extract first 1-2 words from product name as potential keywords
            // Remove common brand/measurement words
            const cleanProductName = productName
              .replace(/co\.op select|kg|gói|hộp|chai|lon|túi|thùng|organic|hữu cơ|\d+g|\d+kg|–|-|\(.*?\)/gi, ' ')
              .trim();
            
            const firstWords = cleanProductName.split(/\s+/).slice(0, 2).join(' ').trim();
            if (firstWords && firstWords.length > 2) {
              keywordsSet.add(firstWords);
            }
          }
        });
      }
    });
    
    console.log('✅ Loaded category mappings:');
    this.categoryToSubcategories.forEach((subcats, cat) => {
      console.log(`  ${cat}: ${Array.from(subcats).slice(0, 10).join(', ')}... (${subcats.size} total)`);
    });
    
    // Trigger change detection after categories are loaded
    this.cdr.detectChanges();
    
  }


  /**
   * Update blog content from contenteditable div (with change detection)
   */
  updateBlogContent(event: Event): void {
    if (!this.currentBlog || !this.contentEditor?.nativeElement) return;
    
    const editor = this.contentEditor.nativeElement;
    this.currentBlog.content = editor.innerHTML;
    
    // Use requestAnimationFrame to avoid blocking
    requestAnimationFrame(() => {
      this.cdr.detectChanges();
    });
  }

  /**
   * Execute formatting command with focus management
   */
  execCommand(command: string, value: string = ''): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const editor = this.contentEditor.nativeElement;
    
    // Save cursor position before command
    const selection = window.getSelection();
    let savedRange: Range | null = null;
    if (selection && selection.rangeCount > 0) {
      savedRange = selection.getRangeAt(0).cloneRange();
    }
    
    // Ensure editor has focus
    editor.focus();
    
    // Save state before command
    const beforeContent = editor.innerHTML;
    
    // Execute command
    document.execCommand(command, false, value);
    
    // Restore cursor position
    if (savedRange && selection) {
      try {
        selection.removeAllRanges();
        selection.addRange(savedRange);
      } catch (e) {
        // If range is invalid, just focus the editor
        editor.focus();
      }
    }
    
    // Save to history if content changed
    if (editor.innerHTML !== beforeContent) {
      // Clear debounce timeout and save immediately
      if (this.historySaveTimeout) {
        clearTimeout(this.historySaveTimeout);
      }
      this.saveToHistory();
      this.updateBlogContent({ target: editor } as any);
    }
  }
  
  /**
   * Undo last action
   */
  undo(): void {
    if (!this.contentEditor?.nativeElement) return;
    if (this.historyIndex <= 0) return;
    
    this.isUpdatingContent = true;
    this.historyIndex--;
    this.contentEditor.nativeElement.innerHTML = this.editorHistory[this.historyIndex];
    
    // Restore focus and cursor
    requestAnimationFrame(() => {
      this.contentEditor.nativeElement.focus();
      // Move cursor to end
      const selection = window.getSelection();
      if (selection) {
        const range = document.createRange();
        range.selectNodeContents(this.contentEditor.nativeElement);
        range.collapse(false);
        selection.removeAllRanges();
        selection.addRange(range);
      }
      this.isUpdatingContent = false;
      this.updateBlogContent({ target: this.contentEditor.nativeElement } as any);
    });
  }
  
  /**
   * Redo last undone action
   */
  redo(): void {
    if (!this.contentEditor?.nativeElement) return;
    if (this.historyIndex >= this.editorHistory.length - 1) return;
    
    this.isUpdatingContent = true;
    this.historyIndex++;
    this.contentEditor.nativeElement.innerHTML = this.editorHistory[this.historyIndex];
    
    // Restore focus and cursor
    requestAnimationFrame(() => {
      this.contentEditor.nativeElement.focus();
      // Move cursor to end
      const selection = window.getSelection();
      if (selection) {
        const range = document.createRange();
        range.selectNodeContents(this.contentEditor.nativeElement);
        range.collapse(false);
        selection.removeAllRanges();
        selection.addRange(range);
      }
      this.isUpdatingContent = false;
      this.updateBlogContent({ target: this.contentEditor.nativeElement } as any);
    });
  }

  /**
   * Get current block element (h1, h2, h3, p, etc.)
   */
  private getCurrentBlockElement(): HTMLElement | null {
    if (!this.contentEditor?.nativeElement) return null;
    
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0) return null;
    
    const range = selection.getRangeAt(0);
    let element: HTMLElement | null = null;
    
    if (range.commonAncestorContainer.nodeType === Node.TEXT_NODE) {
      element = range.commonAncestorContainer.parentElement as HTMLElement;
    } else {
      element = range.commonAncestorContainer as HTMLElement;
    }
    
    // Find the block-level element
    while (element && !['P', 'DIV', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'LI', 'BLOCKQUOTE'].includes(element.tagName)) {
      if (element === this.contentEditor.nativeElement) break;
      element = element.parentElement;
    }
    
    return element;
  }
  
  /**
   * Get current font size in pixels
   */
  private getCurrentFontSize(): number {
    const element = this.getCurrentBlockElement();
    if (!element) return 14; // Default
    
    const computedStyle = window.getComputedStyle(element);
    const fontSize = computedStyle.fontSize;
    
    // Convert to pixels
    if (fontSize.endsWith('px')) {
      return parseFloat(fontSize);
    } else if (fontSize.endsWith('pt')) {
      return parseFloat(fontSize) * 1.33; // 1pt ≈ 1.33px
    } else if (fontSize.endsWith('em')) {
      const baseSize = parseFloat(window.getComputedStyle(this.contentEditor.nativeElement).fontSize);
      return parseFloat(fontSize) * baseSize;
    }
    
    return 14; // Default
  }
  
  /**
   * Update toolbar dropdowns based on current selection
   */
  updateToolbarDropdowns(): void {
    if (!this.contentEditor?.nativeElement) return;
    
    // Update format block dropdown
    const blockElement = this.getCurrentBlockElement();
    if (blockElement && this.formatBlockSelect?.nativeElement) {
      const tagName = blockElement.tagName.toLowerCase();
      const select = this.formatBlockSelect.nativeElement;
      
      // Set selected value
      if (['p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'].includes(tagName)) {
        select.value = tagName;
      } else {
        select.value = 'p'; // Default to paragraph
      }
    }
    
    // Update font size dropdown based on current element
    const fontSize = this.getCurrentFontSize();
    if (this.fontSizeSelect?.nativeElement) {
      const select = this.fontSizeSelect.nativeElement;
      
      // Map font size to dropdown value
      // Default sizes for headings
      let selectedValue = '3'; // Default 12pt
      
      if (blockElement) {
        const tagName = blockElement.tagName.toLowerCase();
        
        // Set default sizes for headings
        if (tagName === 'h1') {
          selectedValue = '7'; // 36pt
        } else if (tagName === 'h2') {
          selectedValue = '6'; // 24pt
        } else if (tagName === 'h3') {
          selectedValue = '5'; // 18pt
        } else if (tagName === 'h4') {
          selectedValue = '4'; // 14pt
        } else if (tagName === 'h5') {
          selectedValue = '3'; // 12pt
        } else if (tagName === 'h6') {
          selectedValue = '2'; // 10pt
        } else {
          // For paragraph or other elements, use actual font size
          // Map px to pt values
          if (fontSize >= 30) {
            selectedValue = '7'; // 36pt
          } else if (fontSize >= 22) {
            selectedValue = '6'; // 24pt
          } else if (fontSize >= 16) {
            selectedValue = '5'; // 18pt
          } else if (fontSize >= 13) {
            selectedValue = '4'; // 14pt
          } else if (fontSize >= 11) {
            selectedValue = '3'; // 12pt
          } else if (fontSize >= 9) {
            selectedValue = '2'; // 10pt
          } else {
            selectedValue = '1'; // 8pt
          }
        }
      }
      
      select.value = selectedValue;
    }
  }
  
  /**
   * Format block (heading, paragraph)
   */
  formatBlock(event: Event): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const target = event.target as HTMLSelectElement;
    const value = target.value;
    
    this.contentEditor.nativeElement.focus();
    document.execCommand('formatBlock', false, value);
    
    // Update font size to match the new format block
    setTimeout(() => {
      const blockElement = this.getCurrentBlockElement();
      if (blockElement && this.fontSizeSelect?.nativeElement) {
        const tagName = blockElement.tagName.toLowerCase();
        let fontSizeValue = '3'; // Default 12pt for paragraph
        
        // Set default font sizes for headings
        if (tagName === 'h1') {
          fontSizeValue = '7'; // 36pt
          blockElement.style.fontSize = '36pt';
        } else if (tagName === 'h2') {
          fontSizeValue = '6'; // 24pt
          blockElement.style.fontSize = '24pt';
        } else if (tagName === 'h3') {
          fontSizeValue = '5'; // 18pt
          blockElement.style.fontSize = '18pt';
        } else if (tagName === 'h4') {
          fontSizeValue = '4'; // 14pt
          blockElement.style.fontSize = '14pt';
        } else if (tagName === 'h5') {
          fontSizeValue = '3'; // 12pt
          blockElement.style.fontSize = '12pt';
        } else if (tagName === 'h6') {
          fontSizeValue = '2'; // 10pt
          blockElement.style.fontSize = '10pt';
        } else {
          // For paragraph, use default or keep current size
          fontSizeValue = this.fontSizeSelect.nativeElement.value || '3';
        }
        
        this.fontSizeSelect.nativeElement.value = fontSizeValue;
      }
      
      // Update all dropdowns
      this.updateToolbarDropdowns();
    }, 10);
    
    this.saveToHistory();
    this.updateBlogContent({ target: this.contentEditor.nativeElement } as any);
  }

  /**
   * Insert link
   */
  insertLink(): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const editor = this.contentEditor.nativeElement;
    editor.focus();
    
    const selection = window.getSelection();
    if (!selection || selection.rangeCount === 0 || selection.toString().trim() === '') {
      this.notificationService.showWarning('Vui lòng chọn văn bản để thêm liên kết');
      return;
    }

    const selectedText = selection.toString();
    const url = prompt('Nhập URL (ví dụ: https://example.com):', 'https://');
    
    if (url && url.trim()) {
      let finalUrl = url.trim();
      
      // Validate URL
      if (!finalUrl.startsWith('http://') && !finalUrl.startsWith('https://')) {
        const confirm = window.confirm('URL không có http:// hoặc https://. Thêm https:// tự động?');
        if (confirm) {
          finalUrl = 'https://' + finalUrl;
        }
      }
      
      document.execCommand('createLink', false, finalUrl);
      this.saveToHistory();
      this.updateBlogContent({ target: editor } as any);
    }
  }

  /**
   * Insert image
   */
  insertImage(): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const editor = this.contentEditor.nativeElement;
    editor.focus();
    
    const url = prompt('Nhập URL của ảnh (ví dụ: https://example.com/image.jpg):', 'https://');
    
    if (url && url.trim()) {
      const imageUrl = url.trim();
      
      // Validate image URL (more flexible - allow data URLs and any http/https)
      if (!imageUrl.startsWith('http://') && 
          !imageUrl.startsWith('https://') && 
          !imageUrl.startsWith('data:')) {
        this.notificationService.showError('URL không hợp lệ. Vui lòng nhập URL hình ảnh hợp lệ (http://, https:// hoặc data:)');
        return;
      }

      // Insert image with Word-like styling
      const img = `<img src="${imageUrl}" alt="Ảnh" style="max-width: 100%; height: auto; border-radius: 8px; margin: 16px 0; display: block;">`;
      document.execCommand('insertHTML', false, img);
      this.saveToHistory();
      this.updateBlogContent({ target: editor } as any);
    }
  }

  /**
   * Change font family
   */
  changeFontFamily(event: Event): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const target = event.target as HTMLSelectElement;
    const fontFamily = target.value;
    
    if (fontFamily) {
      this.contentEditor.nativeElement.focus();
      document.execCommand('fontName', false, fontFamily);
      this.saveToHistory();
      this.updateBlogContent({ target: this.contentEditor.nativeElement } as any);
    }
  }

  /**
   * Change font size with proper pixel/pt values
   */
  changeFontSize(event: Event): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const target = event.target as HTMLSelectElement;
    const sizeValue = target.value;
    
    // Map fontSize values to actual sizes (Word-like)
    const sizeMap: { [key: string]: string } = {
      '1': '8pt',
      '2': '10pt',
      '3': '12pt',
      '4': '14pt',
      '5': '18pt',
      '6': '24pt',
      '7': '36pt'
    };
    
    const fontSize = sizeMap[sizeValue] || '12pt';
    
    this.contentEditor.nativeElement.focus();
    
    // Get current block element
    const blockElement = this.getCurrentBlockElement();
    
    // Use styleWithCSS for better control
    const selection = window.getSelection();
    if (selection && selection.rangeCount > 0) {
      const range = selection.getRangeAt(0);
      
      if (blockElement && !range.collapsed) {
        // Apply to the entire block element
        blockElement.style.fontSize = fontSize;
      } else if (!range.collapsed) {
        // Apply to selected text
        document.execCommand('fontSize', false, sizeValue);
        // Then override with actual size
        const selectedElements = range.commonAncestorContainer.parentElement?.querySelectorAll('font[size]');
        if (selectedElements) {
          selectedElements.forEach((el: any) => {
            if (el.tagName === 'FONT' && el.hasAttribute('size')) {
              el.style.fontSize = fontSize;
              el.removeAttribute('size');
            }
          });
        }
      } else {
        // Apply to current block or position
        if (blockElement) {
          blockElement.style.fontSize = fontSize;
        } else {
          // Apply to current position (will apply to next typed text)
          document.execCommand('fontSize', false, sizeValue);
        }
      }
    } else {
      if (blockElement) {
        blockElement.style.fontSize = fontSize;
      } else {
        document.execCommand('fontSize', false, sizeValue);
      }
    }
    
    this.saveToHistory();
    this.updateBlogContent({ target: this.contentEditor.nativeElement } as any);
  }

  /**
   * Change text color
   */
  changeTextColor(event: Event): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const target = event.target as HTMLInputElement;
    const color = target.value;
    
    this.contentEditor.nativeElement.focus();
    document.execCommand('foreColor', false, color);
    this.saveToHistory();
    this.updateBlogContent({ target: this.contentEditor.nativeElement } as any);
  }

  /**
   * Change highlight/background color
   */
  changeHighlightColor(event: Event): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const target = event.target as HTMLInputElement;
    const color = target.value;
    
    this.contentEditor.nativeElement.focus();
    document.execCommand('backColor', false, color);
    this.saveToHistory();
    this.updateBlogContent({ target: this.contentEditor.nativeElement } as any);
  }
  
  /**
   * Change line spacing (Word-like)
   */
  changeLineSpacing(spacing: string): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const editor = this.contentEditor.nativeElement;
    editor.focus();
    
    const selection = window.getSelection();
    if (selection && selection.rangeCount > 0) {
      const range = selection.getRangeAt(0);
      let element: HTMLElement | null = null;
      
      // Get the block element (p, div, h1-h6, etc.)
      if (range.commonAncestorContainer.nodeType === Node.TEXT_NODE) {
        element = range.commonAncestorContainer.parentElement as HTMLElement;
      } else {
        element = range.commonAncestorContainer as HTMLElement;
      }
      
      // Find the block-level element
      while (element && !['P', 'DIV', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'LI'].includes(element.tagName)) {
        element = element.parentElement;
      }
      
      if (element) {
        // Apply line height
        const lineHeightMap: { [key: string]: string } = {
          '1.0': '1',
          '1.15': '1.15',
          '1.5': '1.5',
          '2.0': '2',
          '2.5': '2.5',
          '3.0': '3'
        };
        
        const lineHeight = lineHeightMap[spacing] || spacing;
        element.style.lineHeight = lineHeight;
        
        // If selection spans multiple blocks, apply to all
        const walker = document.createTreeWalker(
          range.commonAncestorContainer as Node,
          NodeFilter.SHOW_ELEMENT,
          {
            acceptNode: (node: Node) => {
              const el = node as HTMLElement;
              if (['P', 'DIV', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'LI'].includes(el.tagName)) {
                return NodeFilter.FILTER_ACCEPT;
              }
              return NodeFilter.FILTER_SKIP;
            }
          }
        );
        
        let node;
        while (node = walker.nextNode()) {
          (node as HTMLElement).style.lineHeight = lineHeight;
        }
      } else {
        // Apply to current paragraph
        document.execCommand('formatBlock', false, '<p>');
        const p = editor.querySelector('p:last-child') as HTMLElement;
        if (p) {
          p.style.lineHeight = spacing;
        }
      }
    }
    
    this.saveToHistory();
    this.updateBlogContent({ target: editor } as any);
  }

  /**
   * Clear formatting (Word-like - removes all formatting)
   */
  clearFormatting(): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const editor = this.contentEditor.nativeElement;
    editor.focus();
    
    // Remove formatting
    document.execCommand('removeFormat', false);
    document.execCommand('unlink', false); // Remove links
    
    // Also remove inline styles from selected elements
    const selection = window.getSelection();
    if (selection && selection.rangeCount > 0) {
      const range = selection.getRangeAt(0);
      const container = range.commonAncestorContainer;
      
      if (container.nodeType === Node.TEXT_NODE) {
        const parent = container.parentElement;
        if (parent) {
          // Remove style attributes
          parent.removeAttribute('style');
          // Remove font tags
          if (parent.tagName === 'FONT') {
            const text = parent.textContent || '';
            const textNode = document.createTextNode(text);
            parent.parentNode?.replaceChild(textNode, parent);
          }
        }
      } else if (container.nodeType === Node.ELEMENT_NODE) {
        const element = container as HTMLElement;
        element.removeAttribute('style');
        if (element.tagName === 'FONT') {
          const text = element.textContent || '';
          const textNode = document.createTextNode(text);
          element.parentNode?.replaceChild(textNode, element);
        }
      }
    }
    
    this.saveToHistory();
    this.updateBlogContent({ target: editor } as any);
  }
  
  /**
   * Increase indent (Word-like)
   */
  increaseIndent(): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const editor = this.contentEditor.nativeElement;
    editor.focus();
    
    const selection = window.getSelection();
    if (selection && selection.rangeCount > 0) {
      const range = selection.getRangeAt(0);
      let element: HTMLElement | null = null;
      
      if (range.commonAncestorContainer.nodeType === Node.TEXT_NODE) {
        element = range.commonAncestorContainer.parentElement as HTMLElement;
      } else {
        element = range.commonAncestorContainer as HTMLElement;
      }
      
      // Find block element
      while (element && !['P', 'DIV', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'LI', 'BLOCKQUOTE'].includes(element.tagName)) {
        element = element.parentElement;
      }
      
      if (element) {
        const currentMargin = parseInt(window.getComputedStyle(element).marginLeft) || 0;
        element.style.marginLeft = `${currentMargin + 40}px`;
      } else {
        document.execCommand('indent', false);
      }
    } else {
      document.execCommand('indent', false);
    }
    
    this.saveToHistory();
    this.updateBlogContent({ target: editor } as any);
  }
  
  /**
   * Decrease indent (Word-like)
   */
  decreaseIndent(): void {
    if (!this.contentEditor?.nativeElement) return;
    
    const editor = this.contentEditor.nativeElement;
    editor.focus();
    
    const selection = window.getSelection();
    if (selection && selection.rangeCount > 0) {
      const range = selection.getRangeAt(0);
      let element: HTMLElement | null = null;
      
      if (range.commonAncestorContainer.nodeType === Node.TEXT_NODE) {
        element = range.commonAncestorContainer.parentElement as HTMLElement;
      } else {
        element = range.commonAncestorContainer as HTMLElement;
      }
      
      // Find block element
      while (element && !['P', 'DIV', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'LI', 'BLOCKQUOTE'].includes(element.tagName)) {
        element = element.parentElement;
      }
      
      if (element) {
        const currentMargin = parseInt(window.getComputedStyle(element).marginLeft) || 0;
        const newMargin = Math.max(0, currentMargin - 40);
        element.style.marginLeft = `${newMargin}px`;
      } else {
        document.execCommand('outdent', false);
      }
    } else {
      document.execCommand('outdent', false);
    }
    
    this.saveToHistory();
    this.updateBlogContent({ target: editor } as any);
  }
}

