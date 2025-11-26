import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RequestPending } from './request-pending';

describe('RequestPending', () => {
  let component: RequestPending;
  let fixture: ComponentFixture<RequestPending>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RequestPending]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RequestPending);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
