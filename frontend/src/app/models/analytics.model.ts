
import { ReviewResponseDTO } from './review.model';

export interface LocationEventStatsDTO {
  locationId: number;
  locationName: string;
  totalEvents: number;
  regularEvents: number;
  irregularEvents: number;
  paidEvents: number;
  freeEvents: number;
}

export interface LocationRatingDTO {
  locationId: number;
  locationName: string;
  averageRating: number;
}

export interface ManagerAnalyticsResponse {
  eventStatistics: LocationEventStatsDTO[];
  topRatedLocations: LocationRatingDTO[];
  lowestRatedLocations: LocationRatingDTO[];
  topRatedEvents: EventRatingDTO[];
  lowestRatedEvents: EventRatingDTO[];
  latestReviewsForTopLocation: ReviewResponseDTO[];
}
export interface EventRatingDTO {
  eventId: number;
  eventName: string;
  averageRating: number;
}
