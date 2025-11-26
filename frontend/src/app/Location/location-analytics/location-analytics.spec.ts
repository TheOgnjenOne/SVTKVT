import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LocationAnalytics } from './location-analytics';

describe('LocationAnalytics', () => {
  let component: LocationAnalytics;
  let fixture: ComponentFixture<LocationAnalytics>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LocationAnalytics]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LocationAnalytics);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
