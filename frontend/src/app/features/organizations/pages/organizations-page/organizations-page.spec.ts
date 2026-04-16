import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrganizationsPage } from './organizations-page';

describe('OrganizationsPage', () => {
  let component: OrganizationsPage;
  let fixture: ComponentFixture<OrganizationsPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrganizationsPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OrganizationsPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
