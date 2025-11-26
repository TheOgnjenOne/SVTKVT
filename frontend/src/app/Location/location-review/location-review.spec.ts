import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LocationReview } from './location-review';

describe('LocationReview', () => {
  let component: LocationReview;
  let fixture: ComponentFixture<LocationReview>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LocationReview]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LocationReview);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
