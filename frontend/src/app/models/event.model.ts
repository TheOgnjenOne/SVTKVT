import { Location } from './location.model';
import {Image, ImageResponse} from './image.model';
import { Review } from './review.model';

export interface Event {
  id?: number;
  name: string;
  address: string;
  type: string;
  date: string;
  price?: number;
  isRecurrent: boolean;
  location?: Location;
  image?: Image;
  reviews?: Review[];
}
export interface EventRequest {
  name: string;
  address: string;
  type: string;
  date: string;
  price: number | null;
  recurrent: boolean;
  locationId: string;
}
export interface EventResponse {
  id: number;
  name: string;
  address: string;
  type: string;
  date: string;
  price: number | null;
  recurrent: boolean;
  locationId: number;
  locationName: string;
  image: ImageResponse | null;
}


