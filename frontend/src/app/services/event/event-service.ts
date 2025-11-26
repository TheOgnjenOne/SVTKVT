import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {map, Observable} from 'rxjs';
import { EventResponse, EventRequest } from '../../models/event.model';
interface EligibleEvent extends EventResponse {
  recurrencyCount: number;
}
@Injectable({
  providedIn: 'root'
})
export class EventService {

  private readonly API_URL = 'http://localhost:8080/api/events';

  constructor(private http: HttpClient) { }

  getAllEvents(): Observable<EventResponse[]> {
    console.log()
    return this.http.get<EventResponse[]>(this.API_URL);
  }

  getPastEventsByLocationId(locationId: number): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.API_URL}/location/past/${locationId}`);
  }
  getEventsByLocationId(locationId: number): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.API_URL}/location/${locationId}`);
  }
  getFutureEventsByLocationId(locationId: number): Observable<EventResponse[]> {
    return this.http.get<EventResponse[]>(`${this.API_URL}/location/future/${locationId}`);
  }

  createEvent(formData: FormData): Observable<EventResponse> {
    return this.http.post<EventResponse>(`${this.API_URL}/create`, formData);
  }

  updateEvent(eventId: number, eventData: EventRequest, imageFile: File | null): Observable<EventResponse> {
    const formData = new FormData();

    formData.append('event', JSON.stringify(eventData));

    if (imageFile) {
      formData.append('image', imageFile, imageFile.name);
    }

    return this.http.put<EventResponse>(`${this.API_URL}/${eventId}`, formData);
  }

  deleteEvent(eventId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${eventId}`);
  }


}
