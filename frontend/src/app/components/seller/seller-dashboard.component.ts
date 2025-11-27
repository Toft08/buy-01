import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../services/product.service';
import { AuthService } from '../../services/auth.service';
import { Product } from '../../models/ecommerce.model';

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './seller-dashboard.component.html',
  styleUrl: './seller-dashboard.component.scss',
})
export class SellerDashboardComponent implements OnInit {
  myProducts: Product[] = [];
  loading = true;
  error = '';
  showAddForm = false;
  showEditForm = false;
  submitting = false;
  formError = '';
  successMessage = '';
  editingProductId: string | null = null;

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
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.error = 'Failed to load your products.';
        this.loading = false;
      },
    });
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
        this.successMessage = 'Product created successfully!';
        this.resetForm();
        this.showAddForm = false;
        this.submitting = false;
      },
      error: (error) => {
        console.error('Error creating product:', error);
        this.formError = error.error?.message || 'Failed to create product. Please try again.';
        this.submitting = false;
      },
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

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }

  resetForm(): void {
    this.newProduct = {
      name: '',
      description: '',
      price: 0,
      quality: 0,
    };
    this.formError = '';
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
