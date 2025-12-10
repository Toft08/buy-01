import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Product, User } from '../../models/ecommerce.model';
import { AuthService } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';
import { ProductService } from '../../services/product.service';
import { ImageSliderComponent } from '../shared/image-slider/image-slider.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, ImageSliderComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  featuredProducts: Product[] = [];
  productImages: { [productId: string]: string[] } = {};
  isLoading = true;
  currentUser: User | null = null;

  constructor(
    private authService: AuthService,
    private productService: ProductService,
    private mediaService: MediaService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadFeaturedProducts();
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  isSeller(): boolean {
    return this.authService.isSeller();
  }

  private loadFeaturedProducts(): void {
    this.productService.getAllProducts().subscribe({
      next: (products) => {
        this.featuredProducts = products.slice(0, 6); // Show first 6 products
        this.loadProductImages();
      },
      error: (error) => {
        console.error('Failed to load products:', error);
        this.isLoading = false;
      },
    });
  }

  private loadProductImages(): void {
    const imagePromises = this.featuredProducts.map((product) => {
      if (!product.id) return Promise.resolve();

      return this.mediaService
        .getMediaByProduct(product.id)
        .toPromise()
        .then((media) => {
          if (media && media.length > 0) {
            this.productImages[product.id!] = media.map((m) =>
              this.mediaService.getMediaFile(m.id!)
            );
          }
        })
        .catch((error) => {
          console.error(`Failed to load media for product ${product.id}:`, error);
        });
    });

    Promise.all(imagePromises).finally(() => {
      this.isLoading = false;
    });
  }

  getProductImages(productId: string): string[] {
    return this.productImages[productId] || [];
  }
}
