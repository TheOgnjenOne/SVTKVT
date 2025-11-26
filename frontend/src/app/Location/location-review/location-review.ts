import {Component, OnInit, Input, numberAttribute, Output, EventEmitter} from '@angular/core';
import { NgIf, NgFor, NgClass, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LocationService } from '../../services/location/location-service';
import { ReviewService } from '../../services/rewiev/review-service'; // NOVI SERVIS ZA SLANJE
import { AuthService } from '../../services/auth/auth';
import { EventService } from '../../services/event/event-service';

import { Event, EventResponse } from '../../models/event.model';

interface EligibleEvent extends EventResponse {
  recurrencyCount: number;
  isRecurrent: boolean;
}

interface ReviewForm {
  eventId: number | null;
  commentText?: string | null;
  performanceRating: number | null;
  soundLightingRating: number | null;
  venueRating: number | null;
  overallRating: number | null;
}

@Component({
  selector: 'app-location-review',
  templateUrl: './location-review.html',
  styleUrls: ['./location-review.css'],
  standalone: true,
  imports: [NgIf, NgFor, FormsModule, DatePipe]
})
export class LocationReview implements OnInit {

  @Input({transform: numberAttribute}) locationId!: number;
  @Output() reviewSubmitted = new EventEmitter<void>();

  isAuthenticated = false;
  eligibleEvents: EligibleEvent[] = [];
  showReviewForm: boolean = false;
  newReview: ReviewForm = {
    eventId: null,
    commentText: null,
    performanceRating: null,
    soundLightingRating: null,
    venueRating: null,
    overallRating: null
  };

  currentEventRecurrency = 0;
  reviewSent = false;
  submitError = '';
  loadingEvents = false;

  constructor(
    private reviewService: ReviewService,
    private eventService: EventService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.isAuthenticated = !!user;
      if (this.isAuthenticated && this.locationId) {
        this.loadEligibleEvents();
      }
    });
  }

  loadEligibleEvents(): void {
    this.loadingEvents = true;

    this.eventService.getPastEventsByLocationId(this.locationId).subscribe({
      next: (responses: EventResponse[]) => {

        // LOGIKA MAPIRANJA U KOMPONENTI
        const counts = responses.reduce((acc, event) => {
          const key = `${event.name}_${event.locationId}`;
          acc.set(key, (acc.get(key) || 0) + 1);
          return acc;
        }, new Map<string, number>());

        const uniqueEvents: EligibleEvent[] = [];
        const processedKeys = new Set<string>();

        for (const event of responses) {
          const key = `${event.name}_${event.locationId}`;

          if (processedKeys.has(key)) {
            continue;
          }

          // KREIRANJE NOVOG OBJEKTA SA SVIM OBAVEZNIM POLJIMA
          const eligibleEvent: EligibleEvent = {
            ...event as EligibleEvent,
            recurrencyCount: counts.get(key) || 1,
            isRecurrent: event.recurrent // Mapiramo 'recurrent' iz API-ja u 'isRecurrent' za komponentu
          };

          uniqueEvents.push(eligibleEvent);
          processedKeys.add(key);
        }

        this.eligibleEvents = uniqueEvents;

        if (this.eligibleEvents.length > 0) {
          this.newReview.eventId = this.eligibleEvents[0].id;
          this.updateRecurrencyCount(this.eligibleEvents[0].id as number);
        }
        this.loadingEvents = false;
      },
      error: (err) => {
        console.error('Greška pri učitavanju događaja za recenziju:', err);
        this.loadingEvents = false;
      }
    });
  }

  updateRecurrencyCount(eventId: number): void {
    if (!eventId) {
      this.currentEventRecurrency = 0;
      return;
    }
    const selectedEvent = this.eligibleEvents.find(e => e.id === eventId);
    this.currentEventRecurrency = selectedEvent ? selectedEvent.recurrencyCount : 0;
  }

  get ratingOptions(): number[] {
    return Array.from({ length: 10 }, (_, i) => i + 1);
  }

  submitReview(): void {
    this.submitError = '';

    if (!this.newReview.eventId) {
      this.submitError = 'Morate izabrati događaj za koji ostavljate utisak.';
      console.log('DEBUG: Validacija neuspešna: Nije izabran eventId.');
      return;
    }

    const ratings = [
      this.newReview.performanceRating,
      this.newReview.soundLightingRating,
      this.newReview.venueRating,
      this.newReview.overallRating
    ];

    if (ratings.every(r => r === null || r === undefined)) {
      this.submitError = 'Morate dati bar jednu ocenu (Nastup, Zvuk/Svetlo, Prostor ili Ukupan utisak).';
      console.log('DEBUG: Validacija neuspešna: Nema ocena.');
      return;
    }

    const payload = {
      eventId: this.newReview.eventId!,
      locationId: this.locationId,
      commentText: this.newReview.commentText,
      performanceRating: this.newReview.performanceRating,
      soundLightingRating: this.newReview.soundLightingRating,
      venueRating: this.newReview.venueRating,
      overallRating: this.newReview.overallRating,
    };

    console.log('DEBUG: Slanje payload-a:', payload);
    console.log('DEBUG: Slanje na server...');

    this.reviewService.submitReview(payload).subscribe({
      next: () => {
        console.log('DEBUG: Recenzija uspešno poslata.');
        this.reviewSent = true;
        this.newReview = {
          eventId: null, commentText: null, performanceRating: null, soundLightingRating: null, venueRating: null, overallRating: null
        };
        this.reviewSubmitted.emit();
      },
      error: (err: any) => {
        console.error('ERROR: Greška pri slanju recenzije. Detalji odgovora:', err);

        let receivedMessage: string;

        if (typeof err.error === 'string' && err.error.length > 0) {
          receivedMessage = err.error;

        } else if (err.error && typeof err.error === 'object' && err.error.message) {
          receivedMessage = err.error.message;

        } else if (err.status) {
          receivedMessage = `Greška (HTTP ${err.status}). Proverite uslove (redovnost, datum, duplikat) ili vaša ovlašćenja.`;

        } else {
          receivedMessage = 'Nepoznata greška pri komunikaciji sa serverom.';
        }

        console.log('DEBUG: Poruka koja će biti prikazana korisniku:', receivedMessage);
        this.submitError = receivedMessage;
      }
    });
  }
}

