import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AllReviewsModalComponent } from './all-reviews-modal';

describe('AllReviewsModal', () => {
  let component: AllReviewsModalComponent;
  let fixture: ComponentFixture<AllReviewsModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AllReviewsModalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AllReviewsModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
