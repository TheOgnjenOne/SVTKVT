import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { EventService } from '../../services/event/event-service'; // Proverite tačan path!
import { EventResponse } from '../../models/event.model';
import { CurrencyPipe, DatePipe, NgClass, NgForOf, NgIf } from '@angular/common';

interface EventUI extends EventResponse {
  showDetails: boolean;
}

@Component({
  selector: 'app-location-event-page',
  templateUrl: './location-event-page.html',
  styleUrl: './location-event-page.css',
  standalone: true,
  imports: [NgForOf, NgIf, DatePipe, CurrencyPipe, NgClass]
})
export class LocationEventPage implements OnInit {

  locationId: number | null = null;
  locationName: string = '';
  futureEvents: EventUI[] = [];
  loading = true;
  errorMessage: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private eventService: EventService
  ) { }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const idString = params.get('locationId');

      if (idString) {
        this.locationId = +idString;
        this.loadFutureEvents(this.locationId);
      } else {
        this.errorMessage = 'ID локације није пронађен у рути.';
        this.loading = false;
      }
    });
  }

  loadFutureEvents(locationId: number): void {
    this.loading = true;
    this.errorMessage = null;

    this.eventService.getFutureEventsByLocationId(locationId).subscribe({
      next: (data: EventResponse[]) => {
        this.futureEvents = data.map(event => ({ ...event, showDetails: false }));
        this.loading = false;

        if (data.length > 0) {
          this.locationName = data[0].locationName || `Lokacija ID: ${locationId}`;
        } else {
          this.locationName = `Lokacija ID: ${locationId}`;
        }
      },
      error: (err) => {
        console.error('Greška pri dohvatanju budućih događaja:', err);
        this.errorMessage = 'Дошло је до грешке при учитавању догађаја или локација није пронађена.';
        this.loading = false;
      }
    });
  }
}
