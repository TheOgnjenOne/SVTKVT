import {Image, ImageResponse} from './image.model';
import { Event } from './event.model';
import { Review } from './review.model';
import { LocationManager } from './location-manager.model';

export interface Location {
  id?: number;
  name: string;
  address: string;
  type: string;
  description?: string;
  totalRating?: number;
  createdAt?: string;
  image?: Image;
  managers?: LocationManager[];
  events?: Event[];
  reviews?: Review[];
}
export interface LocationListDTO {
  id?: number;
  name: string;
  address: string;
  type: string;
  description?: string;
  totalRating?: number;
  createdAt?: string;
  image?: ImageResponse;
  reviewCount?: number;
  hasPdf?: boolean;
}
