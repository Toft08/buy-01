import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { RegisterRequest } from '../../models/ecommerce.model';
import { AuthService } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent {
  userData: RegisterRequest = {
    name: '',
    email: '',
    password: '',
    role: 'client',
  };

  isLoading = false;
  errorMessage = '';

  // Avatar handling
  selectedAvatarFile: File | null = null;
  avatarPreview: string | null = null;
  avatarError = '';

  private allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
  private maxFileSize = 2 * 1024 * 1024; // 2MB

  constructor(
    private authService: AuthService,
    private mediaService: MediaService,
    private router: Router
  ) {}

  onAvatarSelected(event: Event): void {
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

    this.selectedAvatarFile = file;

    // Create preview
    const reader = new FileReader();
    reader.onload = () => {
      this.avatarPreview = reader.result as string;
    };
    reader.readAsDataURL(file);
  }

  removeAvatar(): void {
    this.selectedAvatarFile = null;
    this.avatarPreview = null;
    this.avatarError = '';
  }

  onSubmit(): void {
    if (this.isLoading) return;

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.register(this.userData).subscribe({
      next: () => {
        // If seller and has avatar, upload it
        if (this.userData.role === 'seller' && this.selectedAvatarFile) {
          this.uploadAvatarAfterRegistration();
        } else {
          this.router.navigate(['/']);
        }
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Registration failed. Please try again.';
        this.isLoading = false;
      },
    });
  }

  private uploadAvatarAfterRegistration(): void {
    if (!this.selectedAvatarFile) {
      this.router.navigate(['/']);
      return;
    }

    this.mediaService.uploadAvatar(this.selectedAvatarFile).subscribe({
      next: (avatar) => {
        // Update user data with avatar (updates BehaviorSubject so navbar sees it)
        this.authService.updateCurrentUser({ avatar: avatar.id });
        this.isLoading = false;
        this.router.navigate(['/']);
      },
      error: () => {
        // Registration succeeded, just avatar failed - still navigate
        this.isLoading = false;
        this.router.navigate(['/']);
      },
    });
  }
}
