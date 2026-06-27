import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// UES: parametri pretrage (poklapaju se sa LocationSearchRequestDTO na backendu)
export interface LocationSearchRequest {
  naziv?: string;
  opis?: string;
  pdfOpis?: string;
  tipMesta?: string;
  operator?: 'AND' | 'OR';

  reviewCountMin?: number | null;
  reviewCountMax?: number | null;

  avgNastupMin?: number | null;
  avgNastupMax?: number | null;
  avgZvukSvetloMin?: number | null;
  avgZvukSvetloMax?: number | null;
  avgProstorMin?: number | null;
  avgProstorMax?: number | null;
  avgUkupnoMin?: number | null;
  avgUkupnoMax?: number | null;

  sortBy?: string | null;
  sortDir?: string | null;

  page?: number;
  size?: number;
}

export interface LocationSearchResult {
  id: number;
  naziv: string;
  opis: string;
  adresa: string;
  tipMesta: string;
  reviewCount: number;
  prosecnaOcena: number;
  avgNastup: number;
  avgZvukSvetlo: number;
  avgProstor: number;
  avgUkupno: number;
  imageId: number | null;
  hasPdf: boolean;
  score: number;
  highlights: string[];
}

export interface LocationSearchResponse {
  total: number;
  results: LocationSearchResult[];
}

@Injectable({
  providedIn: 'root'
})
export class SearchService {

  private readonly SEARCH_URL = 'http://localhost:8080/api/search';
  private readonly LOCATIONS_URL = 'http://localhost:8080/api/locations';

  constructor(private http: HttpClient) {}

  search(request: LocationSearchRequest): Observable<LocationSearchResponse> {
    return this.http.post<LocationSearchResponse>(`${this.SEARCH_URL}/locations`, request);
  }

  moreLikeThis(locationId: number, size: number = 10): Observable<LocationSearchResponse> {
    return this.http.get<LocationSearchResponse>(
      `${this.SEARCH_URL}/locations/${locationId}/more-like-this?size=${size}`
    );
  }

  uploadPdf(locationId: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('pdf', file);
    return this.http.post(`${this.LOCATIONS_URL}/${locationId}/pdf`, formData);
  }

  // Download ide kroz HttpClient (auth interceptor dodaje JWT), pa vraćamo Blob.
  downloadPdf(locationId: number): Observable<Blob> {
    return this.http.get(`${this.LOCATIONS_URL}/${locationId}/pdf`, { responseType: 'blob' });
  }

  reindexAll(): Observable<any> {
    return this.http.post(`${this.SEARCH_URL}/reindex`, {});
  }
}
