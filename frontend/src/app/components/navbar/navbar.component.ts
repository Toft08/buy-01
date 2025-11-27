import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/ecommerce.model';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <nav class="navbar">
      <div class="navbar-container">
        <a routerLink="/" class="navbar-logo">
          <span class="logo-icon">🛒</span>
          Buy-01
        </a>

        <div class="navbar-menu">
          <a routerLink="/products" class="nav-link">Browse Products</a>

          <div *ngIf="!isLoggedIn()" class="nav-auth">
            <a routerLink="/login" class="nav-link">Sign In</a>
            <a routerLink="/register" class="nav-link nav-register">Join as Seller</a>
          </div>

          <div *ngIf="isLoggedIn() && currentUser" class="nav-user">
            <a routerLink="/seller/dashboard" class="nav-link" *ngIf="isSeller()">
              Manage Products
            </a>

            <div class="user-dropdown">
              <button class="user-menu-toggle" (click)="toggleDropdown()">
                <span class="user-greeting">Hello, {{ currentUser.name }}</span>
                <span class="dropdown-icon" [class.open]="dropdownOpen">▼</span>
              </button>

              <div class="dropdown-menu" *ngIf="dropdownOpen" [@dropdownAnimation]>
                <a routerLink="/profile" class="dropdown-item" (click)="closeDropdown()">
                  👤 My Profile
                </a>
                <a routerLink="/profile" class="dropdown-item" (click)="closeDropdown()">
                  🛍️ My Cart
                </a>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item logout" (click)="logout()">🚪 Logout</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </nav>
  `,
  styles: [
    `
      .navbar {
        background-color: #ffffff;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        position: sticky;
        top: 0;
        z-index: 1000;
      }

      .navbar-container {
        max-width: 1200px;
        margin: 0 auto;
        padding: 0 20px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        height: 60px;
      }

      .navbar-logo {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 24px;
        font-weight: 700;
        color: #667eea;
        text-decoration: none;
        transition: color 0.3s;
      }

      .navbar-logo:hover {
        color: #764ba2;
      }

      .logo-icon {
        font-size: 28px;
      }

      .navbar-menu {
        display: flex;
        align-items: center;
        gap: 30px;
      }

      .nav-link {
        color: #333;
        text-decoration: none;
        font-weight: 500;
        transition: color 0.3s;
        cursor: pointer;
      }

      .nav-link:hover {
        color: #667eea;
      }

      .nav-auth {
        display: flex;
        gap: 15px;
        align-items: center;
      }

      .nav-register {
        background-color: #667eea;
        color: white;
        padding: 8px 16px;
        border-radius: 20px;
      }

      .nav-register:hover {
        background-color: #764ba2;
        color: white;
      }

      .nav-user {
        display: flex;
        align-items: center;
        gap: 20px;
      }

      .user-dropdown {
        position: relative;
      }

      .user-menu-toggle {
        background: none;
        border: none;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        color: #333;
        padding: 8px 12px;
        border-radius: 4px;
        transition: background-color 0.3s;
      }

      .user-menu-toggle:hover {
        background-color: #f0f0f0;
      }

      .user-greeting {
        font-weight: 600;
        color: #667eea;
      }

      .dropdown-icon {
        font-size: 10px;
        transition: transform 0.3s;
      }

      .dropdown-icon.open {
        transform: rotate(180deg);
      }

      .dropdown-menu {
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

      .dropdown-item.logout {
        color: #f44336;
        border-radius: 0 0 8px 8px;
      }

      .dropdown-item.logout:hover {
        background-color: #ffebee;
      }

      @media (max-width: 768px) {
        .navbar-container {
          flex-wrap: wrap;
        }

        .navbar-menu {
          gap: 15px;
          flex-basis: 100%;
          margin-top: 10px;
          flex-wrap: wrap;
        }

        .user-greeting {
          display: none;
        }

        .user-menu-toggle {
          padding: 8px 8px;
        }
      }
    `,
  ],
})
export class NavbarComponent implements OnInit {
  currentUser: User | null = null;
  dropdownOpen = false;

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user) => {
      this.currentUser = user;
    });
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  isSeller(): boolean {
    return this.authService.isSeller();
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.closeDropdown();
        this.router.navigate(['/']);
      },
      error: (error: any) => {
        console.error('Logout error:', error);
      },
    });
  }
}
