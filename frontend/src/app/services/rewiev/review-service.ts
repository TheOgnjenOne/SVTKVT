import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {Review, ReviewResponseDTO} from '../../models/review.model';

interface ReviewPayload {
  eventId: number;
  locationId: number;
  commentText?: string | null;
  performanceRating: number | null;
  soundLightingRating: number | null;
  venueRating: number | null;
  overallRating: number | null;
}
interface CommentPayload {
  reviewId: number;
  parentCommentId?: number;
  text: string;
}
@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  private API_URL = 'http://localhost:8080/api/reviews';

  constructor(private http: HttpClient) {}

  submitReview(reviewPayload: ReviewPayload): Observable<any> {
    return this.http.post(`${this.API_URL}/submit`, reviewPayload);
  }

  getAllReviewsByLocation(locationId: number | undefined): Observable<ReviewResponseDTO[]> {
    return this.http.get<ReviewResponseDTO[]>(`${this.API_URL}/location/${locationId}`);
  }

  deleteReview(reviewId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${reviewId}`);
  }

  toggleReviewVisibility(reviewId: number, isHidden: boolean): Observable<ReviewResponseDTO> {
    return this.http.put<ReviewResponseDTO>(`${this.API_URL}/${reviewId}/visibility`, { isHidden });
  }

  addComment(payload: CommentPayload): Observable<any> {
    return this.http.post(`${this.API_URL}/comment`, payload);
  }
}
