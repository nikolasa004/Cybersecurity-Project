import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminCertificatesPage } from './admin-certificates-page';

describe('AdminCertificatesPage', () => {
  let component: AdminCertificatesPage;
  let fixture: ComponentFixture<AdminCertificatesPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminCertificatesPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminCertificatesPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
