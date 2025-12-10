import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CartItem } from '../../models/cart.model';
import { Media, Product } from '../../models/ecommerce.model';
import { CartService } from '../../services/cart.service';
import { MediaService } from '../../services/media.service';
import { ProductService } from '../../services/product.service';
import { ImageSliderComponent } from '../shared/image-slider/image-slider.component';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, RouterModule, ImageSliderComponent],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss',
})
export class ProductListComponent implements OnInit {
  products: Product[] = [];
  productMedia: Map<string, Media[]> = new Map();
  loading = true;
  error = '';

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private mediaService: MediaService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.productService.getAllProducts().subscribe({
      next: (products) => {
        this.products = products;
        this.loading = false;
        // Load media for each product
        products.forEach((product) => {
          if (product.id) {
            this.loadProductMedia(product.id);
          }
        });
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.error = 'Failed to load products. Please try again.';
        this.loading = false;
      },
    });
  }

  loadProductMedia(productId: string): void {
    this.mediaService.getMediaByProduct(productId).subscribe({
      next: (media) => {
        this.productMedia.set(productId, media);
      },
      error: (error) => {
        console.error('Error loading media for product:', productId, error);
      },
    });
  }

  getProductImageUrls(productId: string): string[] {
    const media = this.productMedia.get(productId);
    if (media && media.length > 0) {
      return media.map((m) => this.mediaService.getMediaFile(m.id!));
    }
    return [];
  }

  addToCart(product: Product): void {
    const cartItem: CartItem = {
      productId: product.id!,
      productName: product.name,
      price: product.price,
      quantity: 1,
    };

    this.cartService.addToCart(cartItem);
    console.log(`${product.name} added to cart!`);
  }
}
