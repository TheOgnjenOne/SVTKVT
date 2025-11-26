import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegistrationRequests } from './registration-reqests';

describe('RegistrationReqests', () => {
  let component: RegistrationRequests;
  let fixture: ComponentFixture<RegistrationRequests>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegistrationRequests]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegistrationRequests);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
