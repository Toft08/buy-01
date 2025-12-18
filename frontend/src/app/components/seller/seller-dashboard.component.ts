import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Media, Product } from '../../models/ecommerce.model';
import { AuthService } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './seller-dashboard.component.html',
  styleUrl: './seller-dashboard.component.scss',
})
export class SellerDashboardComponent implements OnInit {
  myProducts: Product[] = [];
  productMedia: Map<string, Media[]> = new Map();
  loading = true;
  error = '';
  showAddForm = false;
  showEditForm = false;
  submitting = false;
  formError = '';
  successMessage = '';
  editingProductId: string | null = null;

  // Two-step product creation
  creationStep: 1 | 2 = 1;
  newlyCreatedProductId: string | null = null;

  // Image upload
  selectedFiles: Map<string, File[]> = new Map();
  uploadingImages: Map<string, boolean> = new Map();
  imageError: Map<string, string> = new Map();
  newProductImages: File[] = []; // For new product creation
  maxImagesPerProduct = 5;
  allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
  maxFileSize = 2 * 1024 * 1024; // 2MB

  newProduct: Product = {
    name: '',
    description: '',
    price: 0,
    quality: 0,
  };

  editProduct: Product = {
    name: '',
    description: '',
    price: 0,
    quality: 0,
  };

  constructor(
    private productService: ProductService,
    private authService: AuthService,
    private mediaService: MediaService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.checkSellerRole();
    this.loadMyProducts();
  }

  checkSellerRole(): void {
    this.authService.currentUser$.subscribe((user) => {
      if (!user || user.role !== 'seller') {
        this.router.navigate(['/']);
      }
    });
  }

  loadMyProducts(): void {
    this.productService.getMyProducts().subscribe({
      next: (products) => {
        this.myProducts = products;
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
        this.error = 'Failed to load your products.';
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

  getProductImages(productId: string): Media[] {
    return this.productMedia.get(productId) || [];
  }

  getImageUrl(media: Media): string {
    return this.mediaService.getMediaFile(media.id!);
  }

  canAddMoreImages(productId: string): boolean {
    const currentImages = this.getProductImages(productId).length;
    return currentImages < this.maxImagesPerProduct;
  }

  getRemainingImageSlots(productId: string): number {
    const currentImages = this.getProductImages(productId).length;
    return this.maxImagesPerProduct - currentImages;
  }

  onFileSelected(event: Event, productId: string): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const files = Array.from(input.files);
    const remainingSlots = this.getRemainingImageSlots(productId);

    // Clear previous errors
    this.imageError.delete(productId);

    // Validate files
    const validFiles: File[] = [];
    for (const file of files) {
      if (!this.allowedTypes.includes(file.type)) {
        this.imageError.set(
          productId,
          `Invalid file type: ${file.name}. Only PNG, JPG, GIF, and WebP are allowed.`
        );
        input.value = '';
        return;
      }
      if (file.size > this.maxFileSize) {
        this.imageError.set(productId, `File too large: ${file.name}. Maximum size is 2MB.`);
        input.value = '';
        return;
      }
      validFiles.push(file);
    }

    if (validFiles.length > remainingSlots) {
      this.imageError.set(
        productId,
        `Can only add ${remainingSlots} more image(s). Maximum is ${this.maxImagesPerProduct} per product.`
      );
      input.value = '';
      return;
    }

    this.selectedFiles.set(productId, validFiles);
    input.value = '';
  }

  uploadImages(productId: string): void {
    const files = this.selectedFiles.get(productId);
    if (!files || files.length === 0) return;

    this.uploadingImages.set(productId, true);
    this.imageError.delete(productId);

    let uploaded = 0;
    const total = files.length;

    files.forEach((file) => {
      this.mediaService.uploadMedia(file, productId).subscribe({
        next: (media) => {
          uploaded++;
          // Add to local media list
          const currentMedia = this.productMedia.get(productId) || [];
          currentMedia.push(media);
          this.productMedia.set(productId, currentMedia);

          if (uploaded === total) {
            this.uploadingImages.set(productId, false);
            this.selectedFiles.delete(productId);
            this.successMessage = `${total} image(s) uploaded successfully!`;
            setTimeout(() => (this.successMessage = ''), 3000);
          }
        },
        error: (error) => {
          console.error('Error uploading image:', error);
          this.imageError.set(productId, error.error?.message || 'Failed to upload image');
          this.uploadingImages.set(productId, false);
        },
      });
    });
  }

  deleteImage(productId: string, mediaId: string): void {
    if (!confirm('Are you sure you want to delete this image?')) return;

    this.mediaService.deleteMedia(mediaId).subscribe({
      next: () => {
        const currentMedia = this.productMedia.get(productId) || [];
        const updatedMedia = currentMedia.filter((m) => m.id !== mediaId);
        this.productMedia.set(productId, updatedMedia);
        this.successMessage = 'Image deleted successfully!';
        setTimeout(() => (this.successMessage = ''), 3000);
      },
      error: (error) => {
        console.error('Error deleting image:', error);
        this.imageError.set(productId, error.error?.message || 'Failed to delete image');
      },
    });
  }

  isUploading(productId: string): boolean {
    return this.uploadingImages.get(productId) || false;
  }

  getSelectedFiles(productId: string): File[] {
    return this.selectedFiles.get(productId) || [];
  }

  getImageError(productId: string): string {
    return this.imageError.get(productId) || '';
  }

  clearSelectedFiles(productId: string): void {
    this.selectedFiles.delete(productId);
    this.imageError.delete(productId);
  }

  createProduct(): void {
    this.formError = '';
    this.successMessage = '';

    if (
      !this.newProduct.name ||
      !this.newProduct.description ||
      this.newProduct.price <= 0 ||
      this.newProduct.quality < 0 ||
      this.newProduct.quality > 100
    ) {
      this.formError = 'Please fill in all fields correctly.';
      return;
    }

    this.submitting = true;

    this.productService.createProduct(this.newProduct).subscribe({
      next: (product) => {
        this.myProducts.unshift(product);
        this.submitting = false;

        // Move to step 2: Image upload
        if (product.id) {
          this.newlyCreatedProductId = product.id;
          this.productMedia.set(product.id, []);
          this.creationStep = 2;
          this.successMessage = 'Product created! Now add images (optional).';
        } else {
          this.finishProductCreation();
        }
      },
      error: (error) => {
        console.error('Error creating product:', error);
        this.formError = error.error?.message || 'Failed to create product. Please try again.';
        this.submitting = false;
      },
    });
  }

  finishProductCreation(): void {
    this.successMessage = 'Product created successfully!';
    this.resetForm();
    this.showAddForm = false;
    this.creationStep = 1;
    this.newlyCreatedProductId = null;
    this.newProductImages = [];
    setTimeout(() => (this.successMessage = ''), 3000);
  }

  skipImageUpload(): void {
    this.finishProductCreation();
  }

  onNewProductFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const files = Array.from(input.files);
    this.formError = '';

    // Validate files
    const validFiles: File[] = [];
    for (const file of files) {
      if (!this.allowedTypes.includes(file.type)) {
        this.formError = `Invalid file type: ${file.name}. Only PNG, JPG, GIF, and WebP are allowed.`;
        input.value = '';
        return;
      }
      if (file.size > this.maxFileSize) {
        this.formError = `File too large: ${file.name}. Maximum size is 2MB.`;
        input.value = '';
        return;
      }
      validFiles.push(file);
    }

    const currentCount = this.newProductImages.length;
    if (currentCount + validFiles.length > this.maxImagesPerProduct) {
      this.formError = `Can only add ${
        this.maxImagesPerProduct - currentCount
      } more image(s). Maximum is ${this.maxImagesPerProduct} per product.`;
      input.value = '';
      return;
    }

    this.newProductImages = [...this.newProductImages, ...validFiles];
    input.value = '';
  }

  removeNewProductImage(index: number): void {
    this.newProductImages.splice(index, 1);
  }

  uploadNewProductImages(): void {
    if (!this.newlyCreatedProductId || this.newProductImages.length === 0) {
      this.finishProductCreation();
      return;
    }

    this.submitting = true;
    this.formError = '';

    let uploaded = 0;
    const total = this.newProductImages.length;
    const productId = this.newlyCreatedProductId;

    this.newProductImages.forEach((file) => {
      this.mediaService.uploadMedia(file, productId).subscribe({
        next: (media) => {
          uploaded++;
          const currentMedia = this.productMedia.get(productId) || [];
          currentMedia.push(media);
          this.productMedia.set(productId, currentMedia);

          if (uploaded === total) {
            this.submitting = false;
            this.successMessage = `Product created with ${total} image(s)!`;
            this.resetForm();
            this.showAddForm = false;
            this.creationStep = 1;
            this.newlyCreatedProductId = null;
            this.newProductImages = [];
            setTimeout(() => (this.successMessage = ''), 3000);
          }
        },
        error: (error) => {
          console.error('Error uploading image:', error);
          this.formError = error.error?.message || 'Failed to upload some images.';
          this.submitting = false;
        },
      });
    });
  }

  openEditForm(product: Product): void {
    this.editingProductId = product.id || null;
    this.editProduct = { ...product };
    this.showEditForm = true;
    this.formError = '';
    this.successMessage = '';
  }

  updateProduct(): void {
    this.formError = '';
    this.successMessage = '';

    if (
      !this.editProduct.name ||
      !this.editProduct.description ||
      this.editProduct.price <= 0 ||
      this.editProduct.quality < 0 ||
      this.editProduct.quality > 100
    ) {
      this.formError = 'Please fill in all fields correctly.';
      return;
    }

    if (!this.editingProductId) {
      this.formError = 'Product ID is missing.';
      return;
    }

    this.submitting = true;

    this.productService.updateProduct(this.editingProductId, this.editProduct).subscribe({
      next: (updatedProduct) => {
        const index = this.myProducts.findIndex((p) => p.id === this.editingProductId);
        if (index !== -1) {
          this.myProducts[index] = updatedProduct;
        }
        this.successMessage = 'Product updated successfully!';
        this.resetEditForm();
        this.showEditForm = false;
        this.submitting = false;
      },
      error: (error) => {
        console.error('Error updating product:', error);
        this.formError = error.error?.message || 'Failed to update product. Please try again.';
        this.submitting = false;
      },
    });
  }

  cancelEdit(): void {
    this.showEditForm = false;
    this.resetEditForm();
  }

  deleteProduct(id: string): void {
    if (!confirm('Are you sure you want to delete this product?')) {
      return;
    }

    this.productService.deleteProduct(id).subscribe({
      next: () => {
        this.myProducts = this.myProducts.filter((p) => p.id !== id);
        this.successMessage = 'Product deleted successfully!';
      },
      error: (error) => {
        console.error('Error deleting product:', error);
        this.error = 'Failed to delete product. Please try again.';
      },
    });
  }

  resetForm(): void {
    this.newProduct = {
      name: '',
      description: '',
      price: 0,
      quality: 0,
    };
    this.formError = '';
    this.creationStep = 1;
    this.newlyCreatedProductId = null;
    this.newProductImages = [];
  }

  resetEditForm(): void {
    this.editProduct = {
      name: '',
      description: '',
      price: 0,
      quality: 0,
    };
    this.editingProductId = null;
    this.formError = '';
  }
}
