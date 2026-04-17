import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ActivateAccountPage } from './activate-account-page';

describe('ActivateAccountPage', () => {
  let component: ActivateAccountPage;
  let fixture: ComponentFixture<ActivateAccountPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActivateAccountPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ActivateAccountPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
