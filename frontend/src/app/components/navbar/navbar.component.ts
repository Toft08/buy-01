import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { User } from '../../models/ecommerce.model';
import { AuthService } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
})
export class NavbarComponent implements OnInit {
  currentUser: User | null = null;
  dropdownOpen = false;
  avatarUrl: string | null = null;

  constructor(
    private readonly authService: AuthService,
    private readonly mediaService: MediaService,
    private readonly router: Router,
    private readonly elementRef: ElementRef
  ) {}

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    // Close dropdown if click is outside the navbar component
    if (this.dropdownOpen && !this.elementRef.nativeElement.contains(event.target)) {
      this.dropdownOpen = false;
    }
  }

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user) => {
      this.currentUser = user;
      this.loadAvatar();
    });
  }

  private loadAvatar(): void {
    if (this.currentUser?.avatar && this.currentUser?.role === 'seller') {
      this.avatarUrl = this.mediaService.getAvatarFileUrl(this.currentUser.avatar);
    } else {
      this.avatarUrl = null;
    }
  }

  getInitials(): string {
    if (!this.currentUser?.name) return 'U';
    return this.currentUser.name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
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
      error: () => {
        // Even on error, still navigate home (auth service already cleared localStorage)
        this.closeDropdown();
        this.router.navigate(['/']);
      },
    });
  }
}
