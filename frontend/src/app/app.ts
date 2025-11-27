import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { Router, RouterModule, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterModule, CommonModule],
  template: `
    <nav class="navbar">
      <div class="nav-container">
        <a routerLink="/" class="nav-brand">Buy-01</a>

        <div class="nav-links">
          <a routerLink="/" class="nav-link">Home</a>
          <a routerLink="/products" class="nav-link">Products</a>

          <div *ngIf="currentUser$ | async as user; else authLinks" class="user-menu">
            <div class="user-dropdown">
              <button (click)="toggleDropdown()" class="user-toggle">
                <span class="welcome">Hello, {{ user.name }}</span>
                <span class="chevron" [class.open]="dropdownOpen">▼</span>
              </button>

              <div class="dropdown-content" *ngIf="dropdownOpen">
                <a routerLink="/profile" class="dropdown-item" (click)="closeDropdown()">
                  👤 My Profile
                </a>
                <a routerLink="/profile" class="dropdown-item" (click)="closeDropdown()">
                  🛍️ My Cart
                </a>
                <a
                  *ngIf="user.role === 'seller'"
                  routerLink="/seller/dashboard"
                  class="dropdown-item"
                  (click)="closeDropdown()"
                >
                  📊 Manage Products
                </a>
                <div class="dropdown-divider"></div>
                <button (click)="logout()" class="dropdown-item logout-btn">🚪 Logout</button>
              </div>
            </div>
          </div>

          <ng-template #authLinks>
            <a routerLink="/login" class="nav-link">Sign In</a>
            <a routerLink="/register" class="btn btn-primary">Sign Up</a>
          </ng-template>
        </div>
      </div>
    </nav>

    <main class="main-content">
      <router-outlet></router-outlet>
    </main>

    <footer class="footer">
      <div class="footer-content">
        <p>&copy; 2024 Buy-01 Marketplace. All rights reserved.</p>
      </div>
    </footer>
  `,
  styles: [
    `
      .navbar {
        background-color: #fff;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        position: sticky;
        top: 0;
        z-index: 1000;
      }

      .nav-container {
        max-width: 1200px;
        margin: 0 auto;
        padding: 0 20px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        height: 70px;
      }

      .nav-brand {
        font-size: 1.8rem;
        font-weight: 700;
        color: #4caf50;
        text-decoration: none;
      }

      .nav-links {
        display: flex;
        align-items: center;
        gap: 20px;
      }

      .nav-link {
        color: #333;
        text-decoration: none;
        font-weight: 500;
        transition: color 0.3s;
      }

      .nav-link:hover {
        color: #4caf50;
      }

      .user-menu {
        display: flex;
        align-items: center;
        gap: 15px;
        position: relative;
      }

      .user-dropdown {
        position: relative;
      }

      .user-toggle {
        background: none;
        border: none;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 6px 12px;
        border-radius: 4px;
        transition: background-color 0.3s;
        font-size: 14px;
      }

      .user-toggle:hover {
        background-color: #f0f0f0;
      }

      .welcome {
        color: #666;
        font-weight: 500;
      }

      .chevron {
        font-size: 10px;
        transition: transform 0.3s;
        display: inline-block;
      }

      .chevron.open {
        transform: rotate(180deg);
      }

      .dropdown-content {
        position: absolute;
        top: 100%;
        right: 0;
        background-color: white;
        border: 1px solid #e0e0e0;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        margin-top: 8px;
        min-width: 200px;
        z-index: 1001;
      }

      .dropdown-item {
        display: block;
        width: 100%;
        padding: 12px 16px;
        text-align: left;
        color: #333;
        background: none;
        border: none;
        cursor: pointer;
        text-decoration: none;
        transition: background-color 0.2s;
        font-size: 14px;
      }

      .dropdown-item:hover {
        background-color: #f5f5f5;
      }

      .dropdown-item:first-child {
        border-radius: 8px 8px 0 0;
      }

      .dropdown-divider {
        height: 1px;
        background-color: #e0e0e0;
        margin: 0;
      }

      .logout-btn {
        color: #f44336;
        border-radius: 0 0 8px 8px;
      }

      .logout-btn:hover {
        background-color: #ffebee;
      }

      .btn {
        padding: 8px 16px;
        border-radius: 20px;
        text-decoration: none;
        font-weight: 600;
        transition: all 0.3s;
        border: none;
        cursor: pointer;
      }

      .btn-primary {
        background-color: #4caf50;
        color: white;
      }

      .btn-primary:hover {
        background-color: #45a049;
      }

      .main-content {
        min-height: calc(100vh - 140px);
      }

      .footer {
        background-color: #333;
        color: white;
        padding: 20px 0;
        text-align: center;
      }

      .footer-content {
        max-width: 1200px;
        margin: 0 auto;
        padding: 0 20px;
      }

      @media (max-width: 768px) {
        .nav-container {
          flex-direction: column;
          height: auto;
          padding: 15px 20px;
        }

        .nav-links {
          margin-top: 15px;
          flex-wrap: wrap;
          justify-content: center;
        }

        .user-menu {
          flex-direction: column;
          gap: 10px;
        }
      }
    `,
  ],
})
export class App {
  protected readonly title = signal('buy-01');
  currentUser$;
  dropdownOpen = false;

  constructor(private authService: AuthService, private router: Router) {
    this.currentUser$ = this.authService.currentUser$;
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  logout(): void {
    console.log('Logout clicked');
    this.authService.logout().subscribe({
      next: () => {
        console.log('✓ Logout successful - navigating to home');
        this.closeDropdown();
        this.router.navigate(['/']);
      },
      error: (error: any) => {
        console.error('✗ Logout failed:', error);
        // Still navigate even if logout fails
        this.router.navigate(['/']);
      },
    });
  }
}
