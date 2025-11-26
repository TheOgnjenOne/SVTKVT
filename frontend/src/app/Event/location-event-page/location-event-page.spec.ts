import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LocationEventPage } from './location-event-page';

describe('LocationEventPage', () => {
  let component: LocationEventPage;
  let fixture: ComponentFixture<LocationEventPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LocationEventPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LocationEventPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
