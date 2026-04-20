import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IssueCertificatePage } from './issue-certificate-page';

describe('IssueCertificatePage', () => {
  let component: IssueCertificatePage;
  let fixture: ComponentFixture<IssueCertificatePage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IssueCertificatePage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IssueCertificatePage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
