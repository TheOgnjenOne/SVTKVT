import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ManagerAnalyticsResponse } from '../../models/analytics.model';

@Injectable({
  providedIn: 'root'
})
export class LocationAnalyticsService {

  private apiUrl = 'http://localhost:8080/api/analytics/manager-data';

  constructor(private http: HttpClient) {}

  getManagerAnalytics(startDate: string, endDate: string): Observable<ManagerAnalyticsResponse> {
    let params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<ManagerAnalyticsResponse>(this.apiUrl, { params });
  }
}
