import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Avatar, Media } from '../models/ecommerce.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class MediaService {
  private apiUrl = 'http://localhost:8080/media';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  uploadMedia(file: File, productId: string): Observable<Media> {
    const formData = new FormData();
    formData.append('file', file);

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    });

    return this.http.post<Media>(`${this.apiUrl}/upload/${productId}`, formData, {
      headers: headers,
      withCredentials: true
    });
  }

  getMediaByProduct(productId: string): Observable<Media[]> {
    return this.http.get<Media[]>(`${this.apiUrl}/product/${productId}`);
  }

  getMediaFile(mediaId: string): string {
    return `${this.apiUrl}/file/${mediaId}`;
  }

  deleteMedia(mediaId: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${mediaId}`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true
    });
  }

  deleteMediaByProduct(productId: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/product/${productId}`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }

  // Avatar methods
  uploadAvatar(file: File): Observable<Avatar> {
    const formData = new FormData();
    formData.append('file', file);

    const headers = new HttpHeaders({
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    });

    return this.http.post<Avatar>(`${this.apiUrl}/avatar/upload`, formData, {
      headers: headers,
      withCredentials: true,
    });
  }

  getAvatarByUserId(userId: string): Observable<Avatar> {
    return this.http.get<Avatar>(`${this.apiUrl}/avatar/user/${userId}`);
  }

  getAvatarFileUrl(avatarId: string): string {
    return `${this.apiUrl}/avatar/file/${avatarId}`;
  }

  deleteAvatar(): Observable<any> {
    return this.http.delete(`${this.apiUrl}/avatar`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }
}