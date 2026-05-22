import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ActivateUserAccountPage } from './activate-user-account-page';

describe('ActivateUserAccountPage', () => {
  let component: ActivateUserAccountPage;
  let fixture: ComponentFixture<ActivateUserAccountPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActivateUserAccountPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ActivateUserAccountPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
