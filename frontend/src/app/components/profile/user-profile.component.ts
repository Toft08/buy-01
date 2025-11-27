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

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.scss'],
})
export class UserProfileComponent implements OnInit {
  currentUser: User | null = null;
  sellerProducts: Product[] = [];
  cart: Cart = { userId: '', items: [], total: 0 };
  isLoading = false;
  private productImages: Map<string, string> = new Map();

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
              this.productImages.set(product.id!, media[0].imagePath);
            }
          },
          error: (error: any) => console.error('Error loading media:', error),
        });
      }
    });
  }

  getProductImage(productId: string): string | undefined {
    return this.productImages.get(productId);
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
}
