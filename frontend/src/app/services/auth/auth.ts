import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import {map, Observable, BehaviorSubject} from 'rxjs'; // Dodao BehaviorSubject

import { jwtDecode } from 'jwt-decode';


interface LoginCredentials {
  email: string;
  password: string;
}
interface RegistrationCredentials {
  email: string;
  password: string;
  address?: string;
}
interface JwtResponse { jwt: string; }
export interface RegistrationResponse {
  status: string;
}
export interface CurrentUser {
  email: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly apiUrl = 'http://localhost:8080/api/auth';
  private readonly TOKEN_KEY = 'auth_token';
  private readonly PENDING_EMAIL_KEY = 'pending_registration_email';

  private currentUserSubject: BehaviorSubject<CurrentUser | null>;
  public currentUser$: Observable<CurrentUser | null>;

  constructor(private http: HttpClient, private router: Router) {
    const initialUser = this.decodeUserFromToken();
    this.currentUserSubject = new BehaviorSubject<CurrentUser | null>(initialUser);
    this.currentUser$ = this.currentUserSubject.asObservable();
  }

  private decodeUserFromToken(): CurrentUser | null {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (!token) return null;

    try {
      const decoded = jwtDecode<any>(token);
      return {
        email: decoded.sub || decoded.email,
        role: decoded.role || decoded.roles[0]
      };
    } catch (err) {
      console.error('JWT decode error', err);
      localStorage.removeItem(this.TOKEN_KEY);
      return null;
    }
  }

  getCurrentUser(): CurrentUser | null {
    return this.currentUserSubject.getValue();
  }

  login(credentials: LoginCredentials): Observable<any> { // Menjam povratni tip u Observable<any> privremeno
    return this.http.post<any>(`${this.apiUrl}/login`, credentials) // Menjam povratni tip u postu privremeno
      .pipe(
        map(res => {
          console.log('DEBUG (FE-AuthService): Mapiranje odgovora:', res);

          if (res?.status === 'REQUEST_STATUS_CHECK') {
            return res;
          }

          localStorage.setItem(this.TOKEN_KEY, res.token);

          const user = this.decodeUserFromToken();
          this.currentUserSubject.next(user);

          return { jwt: res.token };
        })
      );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.clear();

    this.currentUserSubject.next(null);

    this.router.navigate(['/login']);
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }

  saveTokenAndRedirect(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);

    const user = this.decodeUserFromToken();
    this.currentUserSubject.next(user);

    this.router.navigate(['/dashboard']);
  }


  register(registerModel: RegistrationCredentials): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(`${this.apiUrl}/register`, registerModel);
  }

  resubmit(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/resubmit`, { email });
  }

  getCurrentUserEmail(): Observable<string> {
    return this.http.get<{ email: string }>(`${this.apiUrl}/current-user`)
      .pipe(
        map(res => res.email)
      );
  }

  setPendingRegistrationEmail(email: string) {
    localStorage.setItem(this.PENDING_EMAIL_KEY, email);
  }

  getPendingRegistrationEmail(): string | null {
    return localStorage.getItem(this.PENDING_EMAIL_KEY);
  }

  clearPendingRegistrationEmail() {
    localStorage.removeItem(this.PENDING_EMAIL_KEY);
  }
  getRegistrationStatus(email: string): Observable<{ status: string, reason?: string }> {
    return this.http.get<{ status: string, reason?: string }>(`${this.apiUrl}/registration-status?email=${email}`);
  }

}
