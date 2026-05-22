import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegisterUserPage } from './register-user-page';

describe('RegisterUserPage', () => {
  let component: RegisterUserPage;
  let fixture: ComponentFixture<RegisterUserPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterUserPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RegisterUserPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
