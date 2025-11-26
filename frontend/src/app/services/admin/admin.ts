import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface AccountRequest {
  id: number;
  email: string;
  password: string;
  address: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  rejectionReason: string | null;
  createdAt: string;
}

export interface ManagerAssignment {
  userId: number;
  locationId: number;
  startDate: string;
  endDate?: string | null;
}

/**
 * Interface koji predstavlja aktivnog menadžera za datu lokaciju,
 * a koristi se za prikaz u modalnom prozoru na frontendu.
 */
export interface ManagerInfo {
  userId: number;
  email: string;
  startDate: string;
  endDate: string | null;
}
export interface AvailableUser {
  id: number;
  email: string;
}
@Injectable({
  providedIn: 'root'
})
export class AdminService {

  private readonly apiUrl = 'http://localhost:8080/api/admin';

  constructor(private http: HttpClient) { }

  getPendingRequests(): Observable<AccountRequest[]> {
    return this.http.get<AccountRequest[]>(`${this.apiUrl}/pending`)
      .pipe(
        map(requests => requests.map(r => ({
          ...r,
          createdAt: new Date(r.createdAt).toLocaleString()
        })))
      );
  }
  loadAvailableUsers(): Observable<AvailableUser[]> {
    return this.http.get<AvailableUser[]>(`${this.apiUrl}/users/available`);
  }

  approveRequest(id: number): Observable<string> {
    return this.http.post(`${this.apiUrl}/${id}/approve`, {}, { responseType: 'text' });
  }

  rejectRequest(id: number, reason: string): Observable<string> {
    return this.http.post(`${this.apiUrl}/${id}/reject`, { reason }, { responseType: 'text' });
  }

  assignManager(assignment: ManagerAssignment): Observable<string> {
    return this.http.post(`${this.apiUrl}/manager/assign`, assignment, { responseType: 'text' });
  }

  unassignManager(unassignmentData: { userId: number, locationId: number }): Observable<string> {
    return this.http.post(`${this.apiUrl}/manager/unassign`, unassignmentData, { responseType: 'text' });
  }

  getActiveManagers(locationId: number): Observable<ManagerInfo[]> {
    return this.http.get<ManagerInfo[]>(`${this.apiUrl}/location/${locationId}/managers`);
  }

}
