import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Cart } from '../../models/cart.model';
import { Product, User } from '../../models/ecommerce.model';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { MediaService } from '../../services/media.service';
import { ProductService } from '../../services/product.service';
import { ImageSliderComponent } from '../shared/image-slider/image-slider.component';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ImageSliderComponent],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.scss'],
})
export class UserProfileComponent implements OnInit {
  currentUser: User | null = null;
  sellerProducts: Product[] = [];
  cart: Cart = { userId: '', items: [], total: 0 };
  isLoading = false;
  productImages: Map<string, string[]> = new Map();

  // Avatar management
  avatarUrl: string | null = null;
  isUploadingAvatar = false;
  avatarError = '';
  private allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
  private maxFileSize = 2 * 1024 * 1024; // 2MB

  constructor(
    private authService: AuthService,
    private productService: ProductService,
    private cartService: CartService,
    private mediaService: MediaService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();

    if (!this.currentUser) {
      this.router.navigate(['/login']);
      return;
    }

    this.cartService.cart$.subscribe((cart) => {
      this.cart = cart;
    });

    if (this.isSeller()) {
      this.loadSellerProducts();
      this.loadAvatar();
    }
  }

  loadAvatar(): void {
    if (this.currentUser?.avatar) {
      this.avatarUrl = this.mediaService.getAvatarFileUrl(this.currentUser.avatar);
    }
  }

  loadSellerProducts(): void {
    this.isLoading = true;
    this.productService.getAllProducts().subscribe({
      next: (products: Product[]) => {
        // Filter products by current seller
        this.sellerProducts = products.filter((p) => p.user === this.currentUser?.email);
        this.loadProductImages();
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Error loading products:', error);
        this.isLoading = false;
      },
    });
  }

  loadProductImages(): void {
    this.sellerProducts.forEach((product) => {
      if (product.id) {
        this.mediaService.getMediaByProduct(product.id).subscribe({
          next: (media: any) => {
            if (media && media.length > 0) {
              // Convert all media to URLs
              const imageUrls = media.map((m: any) => this.mediaService.getMediaFile(m.id));
              this.productImages.set(product.id!, imageUrls);
            }
          },
          error: () => {
            // Silently ignore - product may not have images
          },
        });
      }
    });
  }

  getProductImages(productId: string): string[] {
    return this.productImages.get(productId) || [];
  }

  isSeller(): boolean {
    return this.authService.isSeller();
  }

  isBuyer(): boolean {
    return this.authService.isClient();
  }

  getInitials(): string {
    if (!this.currentUser?.name) return 'U';
    return this.currentUser.name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase();
  }

  removeFromCart(productId: string): void {
    this.cartService.removeFromCart(productId);
  }

  updateQuantity(productId: string, event: Event): void {
    const target = event.target as HTMLInputElement;
    const quantity = parseInt(target.value, 10);
    if (quantity > 0) {
      this.cartService.updateQuantity(productId, quantity);
    }
  }

  // Avatar management methods
  onAvatarFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];
    this.avatarError = '';

    // Validate file type
    if (!this.allowedTypes.includes(file.type)) {
      this.avatarError = 'Invalid file type. Only JPG, PNG, GIF, and WebP are allowed.';
      input.value = '';
      return;
    }

    // Validate file size
    if (file.size > this.maxFileSize) {
      this.avatarError = 'File size exceeds 2MB limit.';
      input.value = '';
      return;
    }

    this.uploadAvatar(file);
    input.value = '';
  }

  uploadAvatar(file: File): void {
    this.isUploadingAvatar = true;
    this.avatarError = '';

    this.mediaService.uploadAvatar(file).subscribe({
      next: (avatar) => {
        this.avatarUrl = this.mediaService.getAvatarFileUrl(avatar.id!);
        // Update user data (updates BehaviorSubject so navbar sees it)
        this.authService.updateCurrentUser({ avatar: avatar.id });
        if (this.currentUser) {
          this.currentUser.avatar = avatar.id;
        }
        this.isUploadingAvatar = false;
      },
      error: (error) => {
        console.error('Avatar upload failed:', error);
        this.avatarError = error.error?.message || 'Failed to upload avatar';
        this.isUploadingAvatar = false;
      },
    });
  }

  deleteAvatar(): void {
    if (!confirm('Are you sure you want to delete your avatar?')) {
      return;
    }

    this.isUploadingAvatar = true;
    this.avatarError = '';

    this.mediaService.deleteAvatar().subscribe({
      next: () => {
        this.avatarUrl = null;
        // Update user data (updates BehaviorSubject so navbar sees it)
        this.authService.updateCurrentUser({ avatar: undefined });
        if (this.currentUser) {
          this.currentUser.avatar = undefined;
        }
        this.isUploadingAvatar = false;
      },
      error: (error) => {
        console.error('Avatar delete failed:', error);
        this.avatarError = error.error?.message || 'Failed to delete avatar';
        this.isUploadingAvatar = false;
      },
    });
  }
}
