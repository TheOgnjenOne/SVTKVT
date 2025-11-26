import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CommentTreeComponent } from './comment-tree';

describe('CommentTree', () => {
  let component: CommentTreeComponent;
  let fixture: ComponentFixture<CommentTreeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommentTreeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CommentTreeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
