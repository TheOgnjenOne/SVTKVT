// src/app/pages/event-page/event-page.ts

import { Component, OnInit, OnDestroy } from '@angular/core';
import {CurrencyPipe, DatePipe, NgClass, NgForOf, NgIf} from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AuthService, CurrentUser } from '../../services/auth/auth';
import { EventService } from '../../services/event/event-service';
import { LocationService } from '../../services/location/location-service';
import { EventResponse, EventRequest } from '../../models/event.model';
import { Location } from '../../models/location.model';

interface EventUI extends EventResponse {
  showDetails: boolean;
}

@Component({
  selector: 'app-event-page',
  templateUrl: './event-page.html',
  styleUrl: './event-page.css',
  standalone: true,
  imports: [NgIf, FormsModule, NgForOf, NgClass, DatePipe, CurrencyPipe]
})
export class EventPage implements OnInit, OnDestroy {

  events: EventUI[] = [];
  locations: Location[] = [];
  loading = true;
  errorMessage = '';
  currentUser: CurrentUser | null = null;
  private userSubscription: Subscription | undefined;

  showAddForm = false;
  showEditForm = false;
  showDeleteConfirmModal = false;

  managedLocationIds: number[] = [];

  deletingEventId: number | undefined;

  editingEvent: EventUI | null = null;
  selectedFile: File | null = null;

  addError: string = '';
  editError: string = '';

  newEvent: EventRequest = {
    name: '',
    address: '',
    type: '',
    date: new Date().toISOString().substring(0, 16),
    price: null,
    recurrent: false,
    locationId: ''
  };

  searchType: string = '';
  searchLocationId: number | null = null;
  searchAddress: string = '';
  searchMaxPrice: number | null = null;
  searchDate: string | null = null;
  dateFilterMode: 'all' | 'past' | 'future' | 'today' = 'today';
  filterFreeEvents: boolean = false;

  constructor(
    private eventService: EventService,
    private authService: AuthService,
    private locationService: LocationService
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
    this.loadEvents();
    this.loadAllLocations();
  }

  ngOnDestroy(): void {
    if (this.userSubscription) {
      this.userSubscription.unsubscribe();
    }
  }

  resetFilters(): void {
    this.searchType = '';
    this.searchLocationId = null;
    this.searchAddress = '';
    this.searchMaxPrice = null;
    this.searchDate = null;
    this.filterFreeEvents = false;
    this.dateFilterMode = 'today';
  }


  get filteredEvents(): EventUI[] {
    let filtered = this.events;
    const now = new Date();
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0);
    const tomorrowStart = new Date(todayStart);
    tomorrowStart.setDate(todayStart.getDate() + 1);


    //  Filtriranje po tipu
    if (this.searchType) {
      const term = this.searchType.toLowerCase();
      filtered = filtered.filter(e => e.type.toLowerCase().includes(term));
    }

    //  Filtriranje po mestu/lokaciji
    if (this.searchLocationId !== null) {
      filtered = filtered.filter(e => e.locationId === this.searchLocationId);
    }

    // Filtriranje po adresi
    if (this.searchAddress) {
      const term = this.searchAddress.toLowerCase();
      filtered = filtered.filter(e => e.address.toLowerCase().includes(term));
    }

    //  Filtriranje po ceni
    if (this.filterFreeEvents) {
      filtered = filtered.filter(e => (e.price || 0) === 0);
    } else if (this.searchMaxPrice !== null) {
      const maxPrice = this.searchMaxPrice;
      filtered = filtered.filter(e => (e.price || 0) <= maxPrice);
    }

    //  Filtriranje po datumu
    if (this.searchDate) {
      filtered = filtered.filter(e => {
        const eventDateString = e.date.substring(0, 10); // YYYY-MM-DD
        return eventDateString === this.searchDate;
      });
    } else {
      filtered = filtered.filter(e => {
        const eventDate = new Date(e.date);

        if (this.dateFilterMode === 'future') {
          return eventDate >= tomorrowStart;
        } else if (this.dateFilterMode === 'past') {
          return eventDate < todayStart;
        } else if (this.dateFilterMode === 'today') {
          return eventDate >= todayStart && eventDate < tomorrowStart;
        }
        return true;
      });
    }

    return filtered;
  }

  private loadCurrentUser(): void {
    this.userSubscription = this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      if (user && user.role === 'MANAGER') {
        this.loadManagedLocations();
      } else {
        this.managedLocationIds = [];
      }
    });
  }

  private loadManagedLocations(): void {
    this.locationService.getMyManagedLocations().subscribe({
      next: (ids: number[]) => {
        this.managedLocationIds = ids;
      },
      error: (err) => {
        console.error('Greška pri dohvatanju menadžerskih ID-eva:', err);
        this.managedLocationIds = [];
      }
    });
  }

  private loadAllLocations(): void {
    this.locationService.getAll().subscribe({
      next: (data: Location[]) => {
        this.locations = data.map(l => ({
          id: l.id, name: l.name, address: l.address, type: l.type,
          description: l.description, totalRating: l.totalRating,
           image: l.image, showDetails: (l as any).showDetails
        }));
      },
      error: (err) => {
        console.error('Greška pri učitavanju lokacija za padajući meni:', err);
      }
    });
  }

  private loadEvents(): void {
    this.loading = true;
    this.eventService.getAllEvents().subscribe({
      next: (data: EventResponse[]) => {
        this.events = data.map(event => ({...event, showDetails: false}));
        this.loading = false;
      },
      error: (err) => {
        console.error('Greška pri učitavanju događaja:', err);
        this.errorMessage = 'Greška pri učitavanju događaja';
        this.loading = false;
      }
    });
  }

  canEditDelete(event: EventUI): boolean {
    if (!this.currentUser) return false;

    if (this.currentUser.role === 'ADMIN') {
      return true;
    }
    if (this.currentUser.role === 'MANAGER') {
      return this.managedLocationIds.includes(event.locationId);
    }

    return false;
  }

  handleRecurrentChange(event: Event, model: 'new' | 'edit'): void {
    const target = event.target as HTMLInputElement;

    if (model === 'new') {
      this.newEvent.recurrent = target.checked;
    } else if (model === 'edit' && this.editingEvent) {
      this.editingEvent.recurrent = target.checked;
    }
  }

  onFileSelected(event: Event) {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      this.selectedFile = target.files[0];
    }
  }

  addEvent(): void {
    this.addError = '';

    if (!this.newEvent.name || !this.newEvent.locationId || !this.newEvent.address || !this.newEvent.type || !this.newEvent.date || !this.selectedFile) {
      this.addError = 'Popunite sva obavezna polja i dodajte sliku.';
      return;
    }

    const selectedLocationId = Number(this.newEvent.locationId);
    const isAuthorized = this.currentUser?.role === 'ADMIN' ||
      (this.currentUser?.role === 'MANAGER' && this.managedLocationIds.includes(selectedLocationId));

    if (!isAuthorized) {
      this.addError = 'Nije moguće dodati događaj. Menadžer može kreirati događaje samo za lokacije koje su mu dodeljene.';
      console.warn(`Korisnik ${this.currentUser?.email} (Uloga: ${this.currentUser?.role}) nema ovlašćenje za Lokaciju ID: ${selectedLocationId}.`);
      return;
    }

    if (this.newEvent.price === undefined || this.newEvent.price === null) {
      this.newEvent.price = 0;
    }

    const eventDataToSend: EventRequest = {
      ...this.newEvent,
      locationId: String(this.newEvent.locationId)
    };

    const eventJson = JSON.stringify(eventDataToSend);
    const formData = new FormData();

    formData.append('event', eventJson);

    if (this.selectedFile) {
      formData.append('image', this.selectedFile, this.selectedFile.name);
    }

    this.eventService.createEvent(formData).subscribe({
      next: (savedEvent) => {
        this.events.push({...savedEvent, showDetails: false});
        this.showAddForm = false;
        this.newEvent = { name: '', address: '', type: '', date: new Date().toISOString().substring(0, 16), price: null, recurrent: false, locationId: '' };
        this.selectedFile = null;
        this.addError = '';
      },
      error: (err) => {
        console.error('Potpuni objekat greške (za debug):', err);

        const status = err.status || 'Nepoznat';
        let detailedMessage = err.error?.message || (status === 403 ? 'NEMATE DOZVOLU za ovu operaciju.' : 'Došlo je do greške na serveru.');

        if (status === 400) {
          detailedMessage = 'Neispravan format podataka. Proverite da li su svi brojevi ispravni.';
        } else if (status === 403) {
          detailedMessage = 'Zabranjeno. Niste ovlašćeni menadžer za ovu lokaciju, ili je token istekao.';
        } else if (status === 404) {
          detailedMessage = 'Lokacija ili korisnik nisu pronađeni.';
        }

        this.addError = `Greška (HTTP ${status}): ${detailedMessage}`;
      }
    });
  }

  openEditModal(event: EventUI): void {
    this.editingEvent = {...event};
    this.selectedFile = null;
    this.showEditForm = true;
    this.editError = '';
  }

  saveEventEdit(): void {
    this.editError = '';
    if (!this.editingEvent || !this.editingEvent.id) return;
    if (!this.editingEvent.name || !this.editingEvent.locationId || !this.editingEvent.address || !this.editingEvent.type || !this.editingEvent.date) {
      this.editError = 'Popunite sva obavezna polja!';
      return;
    }

    const requestData: EventRequest = {
      name: this.editingEvent.name,
      address: this.editingEvent.address,
      type: this.editingEvent.type,
      date: this.editingEvent.date,
      price: this.editingEvent.price || 0,
      recurrent: this.editingEvent.recurrent,
      locationId: String(this.editingEvent.locationId)
    };

    this.eventService.updateEvent(this.editingEvent.id, requestData, this.selectedFile).subscribe({
      next: (updatedEvent) => {
        const index = this.events.findIndex(e => e.id === updatedEvent.id);
        if (index !== -1) {
          this.events[index] = {...updatedEvent, showDetails: this.events[index].showDetails};
        }
        this.showEditForm = false;
        this.editingEvent = null;
        this.selectedFile = null;
        this.editError = '';
      },
      error: (err) => {
        console.error('Greška pri izmeni događaja:', err);

        const status = err.status || 'Nepoznat';
        let detailedMessage = err.error?.message || (status === 403 ? 'NEMATE DOZVOLU za ovu operaciju.' : 'Došlo je do greške pri izmeni događaja.');

        if (status === 403) {
          detailedMessage = 'Zabranjeno. Niste ovlašćeni menadžer za lokaciju ovog događaja, ili je token istekao.';
        } else if (status === 404) {
          detailedMessage = 'Događaj nije pronađen.';
        }

        this.editError = `Greška (HTTP ${status}): ${detailedMessage}`;
      }
    });
  }

  confirmDelete(id: number | undefined): void {
    if (id) {
      this.deletingEventId = id;
      this.showDeleteConfirmModal = true;
      this.errorMessage = '';
    }
  }

  deleteEvent(): void {
    if (!this.deletingEventId) return;

    this.eventService.deleteEvent(this.deletingEventId).subscribe({
      next: () => {
        this.events = this.events.filter(event => event.id !== this.deletingEventId);
        this.showDeleteConfirmModal = false;
        this.deletingEventId = undefined;
        this.errorMessage = '';
      },
      error: (err) => {
        console.error('Greška pri brisanju događaja:', err);

        const status = err.status || 'Nepoznat';
        let detailedMessage = err.error?.message || (status === 403 ? 'NEMATE DOZVOLU za ovu operaciju.' : 'Došlo je do greške pri brisanju događaja.');

        if (status === 403) {
          detailedMessage = 'Zabranjeno. Niste ovlašćeni menadžer za lokaciju ovog događaja, ili je token istekao.';
        } else if (status === 404) {
          detailedMessage = 'Događaj nije pronađen.';
        }

        this.errorMessage = `Greška pri brisanju (HTTP ${status}): ${detailedMessage}`;

        this.showDeleteConfirmModal = false;
        this.deletingEventId = undefined;
      }
    });
  }

  cancelDelete(): void {
    this.showDeleteConfirmModal = false;
    this.deletingEventId = undefined;
  }
}
