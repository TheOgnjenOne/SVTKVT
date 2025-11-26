import { User } from './user.model';
import { Location } from './location.model';
import { Event } from './event.model';
import {Comment, CommentDTO} from './comment.model';

export interface Review {
  id?: number;
  createdAt?: string;
  commentText?: string;
  isHidden?: boolean;
  eventCount?: number;
  performanceRating?: number;
  soundLightingRating?: number;
  venueRating?: number;
  overallRating?: number;
  user?: User;
  location?: Location;
  event?: Event;
  comments?: Comment[];
}

export interface ReviewResponseDTO {
  id: number;
  userName: string;
  submissionDate: string;
  commentText: string;
  overallRating: number;
  reviewedLocationId: number;
  reviewedEventId: number;
  eventCount: number;
  isHidden: boolean;
  comments: CommentDTO[];
}
export interface ReviewDisplay extends ReviewResponseDTO {
  showReplyForm?: boolean;
}
