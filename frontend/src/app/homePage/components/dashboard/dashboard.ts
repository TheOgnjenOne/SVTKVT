import { Component, OnInit } from '@angular/core';

import { Router } from '@angular/router';
import { map } from 'rxjs';
import {DatePipe, NgIf, NgFor, CurrencyPipe, DecimalPipe, SlicePipe} from '@angular/common';
import {EventResponse} from '../../../models/event.model';
import {LocationListDTO} from '../../../models/location.model';
import {ReviewResponseDTO} from '../../../models/review.model';
import {EventService} from '../../../services/event/event-service';
import {LocationService} from '../../../services/location/location-service';
import {ReviewService} from '../../../services/rewiev/review-service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, NgIf, NgFor, DecimalPipe, SlicePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  providers: [DatePipe]
})
export class Dashboard implements OnInit {
  eventsToday: EventResponse[] = [];
  topLocations: LocationListDTO[] = [];
  selectedLocationId: number | null = null;
  selectedLocationReviews: ReviewResponseDTO[] = [];
  loading = true;

  constructor(
    private eventService: EventService,
    private locationService: LocationService,
    private reviewService: ReviewService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.eventService.getAllEvents().subscribe(events => {
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      this.eventsToday = events.filter(event => {
        const eventDate = new Date(event.date);
        eventDate.setHours(0, 0, 0, 0);
        return eventDate.getTime() === today.getTime();
      });
    });

    this.locationService.getTopLocations(4).subscribe(locations => {
      this.topLocations = locations;
      this.loading = false;

      if (this.topLocations.length > 0 && this.topLocations[0].id) {
        this.selectLocation(this.topLocations[0].id!);
      }
    });
  }

  selectLocation(locationId: number): void {
    if (this.selectedLocationId === locationId) return;

    this.selectedLocationId = locationId;
    this.selectedLocationReviews = [];

    this.reviewService.getAllReviewsByLocation(locationId).pipe(
      map(reviews => reviews
        .sort((a, b) => new Date(b.submissionDate).getTime() - new Date(a.submissionDate).getTime())
        .slice(0, 3)
      )
    ).subscribe(reviews => {
      this.selectedLocationReviews = reviews;
    });
  }

  goToAllEvents() {
    this.router.navigate(['/events']);
  }

  getSelectedLocationName(): string {
    const selected = this.topLocations.find(loc => loc.id === this.selectedLocationId);
    return selected ? selected.name : '';
  }

  getSelectedLocationDescription(): string {
    const selected = this.topLocations.find(loc => loc.id === this.selectedLocationId);
    return selected ? (selected.description ?? '') : '';
  }

  getSelectedLocationRating(): number | null {
    const selected = this.topLocations.find(loc => loc.id === this.selectedLocationId);
    return selected ? (selected.totalRating ?? 0) : null;
  }
}
