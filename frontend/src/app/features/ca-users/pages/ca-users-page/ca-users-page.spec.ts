import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CaUsersPage } from './ca-users-page';

describe('CaUsersPage', () => {
  let component: CaUsersPage;
  let fixture: ComponentFixture<CaUsersPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CaUsersPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CaUsersPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
