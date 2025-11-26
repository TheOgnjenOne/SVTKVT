import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';


export interface ChangePassword {
  currentPassword: string;
  newPassword: string;
}

export interface UserData {
  id: number;
  email: string;
  name: string;
  phoneNumber: string;
  birthday: string;
  address: string;
  city: string;
  role: 'USER' | 'MANAGER' | 'ADMIN';
  profileImageId: number | null;
}

export interface UserUpdateData {
  name: string;
  phoneNumber: string;
  birthday: string;
  address: string;
  city: string;
}

export interface UserReview {
  id: number;
  locationName: string;
  rating: number;
  text: string;
  reviewDate: string;
}

export interface ManagedLocation {
  id: number;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly apiUrl = 'http://localhost:8080/api/user';

  constructor(private http: HttpClient) {}

  getUserProfile(): Observable<UserData> {
    return this.http.get<UserData>(`${this.apiUrl}/profile`);
  }

  updateProfile(data: UserUpdateData): Observable<any> {
    return this.http.put(`${this.apiUrl}/profile`, data);
  }

  getUserReviews(): Observable<UserReview[]> {
    return this.http.get<UserReview[]>(`${this.apiUrl}/reviews`);
  }

  getManagedLocations(): Observable<ManagedLocation[]> {
    return this.http.get<ManagedLocation[]>(`${this.apiUrl}/managed-locations`);
  }

  changePassword(passwordCredentials: ChangePassword): Observable<any> {
    return this.http.post(`${this.apiUrl}/change-password`, passwordCredentials);
  }

  uploadProfileImage(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file, file.name); // 'file' mora odgovarati imenu polja na backend-u

    return this.http.put(`${this.apiUrl}/profile/image`, formData);
  }
}
