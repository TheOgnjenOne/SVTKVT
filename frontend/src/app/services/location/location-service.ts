// src/app/services/location/location-service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {Location, LocationListDTO} from '../../models/location.model'


@Injectable({
  providedIn: 'root'
})
export class LocationService {

  private apiUrl = 'http://localhost:8080/api/locations';

  constructor(private http: HttpClient) {}

  getById(id: number): Observable<LocationListDTO> {
    return this.http.get<LocationListDTO>(`${this.apiUrl}/${id}`);
  }

  getAll(): Observable<LocationListDTO[]> {
    return this.http.get<LocationListDTO[]>(this.apiUrl);
  }

  create(formData: FormData): Observable<Location> {
    return this.http.post<Location>(`${this.apiUrl}/create`, formData);
  }

  update(id: number | string, formData: FormData): Observable<Location> {
    return this.http.put<Location>(`${this.apiUrl}/${id}`, formData);
  }

  delete(id: number | string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getMyManagedLocations(): Observable<number[]> {
    return this.http.get<number[]>(`${this.apiUrl}/my-managed-ids`);
  }

  getTopLocations(limit: number): Observable<LocationListDTO[]> {
    return this.http.get<LocationListDTO[]>(`${this.apiUrl}/top?limit=${limit}`);
  }

}
