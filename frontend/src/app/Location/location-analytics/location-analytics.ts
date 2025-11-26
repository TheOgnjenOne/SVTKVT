// src/app/components/location-analytics/location-analytics.component.ts

import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import { AsyncPipe, NgIf, NgFor, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';

import { LocationAnalyticsService } from '../../services/manager/analytics-service';
import { LocationEventStatsDTO, ManagerAnalyticsResponse, EventRatingDTO} from '../../models/analytics.model';
import { Observable, catchError, of, tap, BehaviorSubject, switchMap, finalize } from 'rxjs';
import { ChartConfiguration, ChartOptions } from 'chart.js';

import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
  DoughnutController
} from 'chart.js';

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
  DoughnutController
);

const formatToISO = (date: Date): string => new Date(date.getTime() - (date.getTimezoneOffset() * 60000)).toISOString();
const formatForInput = (date: Date): string => {
  const local = new Date(date.getTime() - (date.getTimezoneOffset() * 60000));
  return local.toISOString().slice(0, 16);
};
const getPeriodStart = (months: number): string => {
  const date = new Date();
  date.setMonth(date.getMonth() - months);
  return formatForInput(date);
};


@Component({
  selector: 'app-location-analytics',
  templateUrl: './location-analytics.html',
  styleUrls: ['./location-analytics.css'],
  standalone: true,
  imports: [
    AsyncPipe, NgIf, NgFor, FormsModule, BaseChartDirective, DecimalPipe
  ]
})
export class LocationAnalyticsComponent implements OnInit {

  analyticsData$!: Observable<ManagerAnalyticsResponse>;
  loadingFull: boolean = true;
  loadingCharts: boolean = false;
  error: string | null = null;

  private fetchTrigger$ = new BehaviorSubject<void>(undefined);

  startDate: string = getPeriodStart(1);
  endDate: string = formatForInput(new Date());

  selectedLocationId: number = -1;
  currentLocationIndex: number = 0;

  topRatedEvents: EventRatingDTO[] = [];
  lowestRatedEvents: EventRatingDTO[] = [];

  constructor(private analyticsService: LocationAnalyticsService,private cdr: ChangeDetectorRef) { }

  ngOnInit(): void {
    this.analyticsData$ = this.fetchTrigger$.pipe(
      tap(() => {
        this.loadingFull = true;
        this.error = null;
      }),
      switchMap(() => {
        this.loadingCharts = true;

        const startISO = formatToISO(new Date(this.startDate));
        const endISO = formatToISO(new Date(this.endDate));

        return this.analyticsService.getManagerAnalytics(startISO, endISO).pipe(
          tap(data => {
            this.loadingFull = false;

            this.topRatedEvents = data.topRatedEvents || [];
            this.lowestRatedEvents = data.lowestRatedEvents || [];

            if (data.eventStatistics.length > 0) {

              let targetId = this.selectedLocationId;

              if (this.selectedLocationId === -1) {
                targetId = data.eventStatistics[0].locationId;
              }

              const index = data.eventStatistics.findIndex(s => s.locationId === targetId);

              this.currentLocationIndex = index !== -1 ? index : 0;
              this.selectedLocationId = data.eventStatistics[this.currentLocationIndex].locationId;

            } else {
              this.currentLocationIndex = 0;
              this.selectedLocationId = -1;
            }
          }),
          catchError(err => {
            this.error = 'Greška pri dohvatu analitike.';
            this.loadingFull = false;
            return of({
              eventStatistics: [],
              topRatedLocations: [],
              lowestRatedLocations: [],
              latestReviewsForTopLocation: [],
              topRatedEvents: [],
              lowestRatedEvents: []
            } as ManagerAnalyticsResponse);
          }),
          finalize(() => {
            this.loadingCharts = false;
          })
        );
      })
    );
  }

  handlePeriodChange(period?: 'week' | 'month' | 'year'): void {
    if (period) {
      let newDate = new Date();
      if (period === 'week') newDate.setDate(newDate.getDate() - 7);
      if (period === 'month') newDate.setMonth(newDate.getMonth() - 1);
      if (period === 'year') newDate.setFullYear(newDate.getFullYear() - 1);

      this.startDate = formatForInput(newDate);
      this.endDate = formatForInput(new Date());
    }
    this.fetchTrigger$.next();
  }

  onLocationSelectChange(selectedId: number, data: ManagerAnalyticsResponse): void {
    this.selectedLocationId = selectedId;

    if (data.eventStatistics.length > 0) {
      const targetId = selectedId === -1 ? data.eventStatistics[0].locationId : selectedId;
      const index = data.eventStatistics.findIndex(s => s.locationId === targetId);
      this.currentLocationIndex = index !== -1 ? index : 0;

      this.loadingCharts = true;
      this.cdr.detectChanges();

      setTimeout(() => this.loadingCharts = false, 100);
    }
  }


  navigateLocation(direction: 'prev' | 'next', data: ManagerAnalyticsResponse): void {
    const count = data.eventStatistics.length;
    if (count <= 1) return;

    let newIndex = this.currentLocationIndex;

    if (direction === 'next') {
      newIndex = (newIndex + 1) % count;
    } else if (direction === 'prev') {
      newIndex = (newIndex - 1 + count) % count;
    }

    this.currentLocationIndex = newIndex;
    this.selectedLocationId = data.eventStatistics[newIndex].locationId;
  }

  getCurrentStats(data: ManagerAnalyticsResponse): LocationEventStatsDTO | undefined {

    if (data.eventStatistics.length > 0 && this.currentLocationIndex >= 0 && this.currentLocationIndex < data.eventStatistics.length) {
      return data.eventStatistics[this.currentLocationIndex];
    }
    return undefined;
  }


  // --- CHART LOGIKA  ---
  public doughnutChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' } }
  };
  public doughnutChartType = 'doughnut' as const;

  getPaidFreeChartData(stats: LocationEventStatsDTO): ChartConfiguration<'doughnut'>['data'] {
    return {
      labels: [`Plaćeni (${stats.paidEvents})`, `Besplatni (${stats.freeEvents})`],
      datasets: [
        {
          data: [stats.paidEvents, stats.freeEvents],
          backgroundColor: ['#28a745', '#ffc107'],
        }
      ]
    };
  }

  getRecurrenceChartData(stats: LocationEventStatsDTO): ChartConfiguration<'doughnut'>['data'] {
    return {
      labels: [`Redovni (${stats.regularEvents})`, `Neredovni (${stats.irregularEvents})`],
      datasets: [
        {
          data: [stats.regularEvents, stats.irregularEvents],
          backgroundColor: ['#007bff', '#dc3545'],
        }
      ]
    };
  }
}
