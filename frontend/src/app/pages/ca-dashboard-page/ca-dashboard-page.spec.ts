import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CaDashboardPage } from './ca-dashboard-page';

describe('CaDashboardPage', () => {
  let component: CaDashboardPage;
  let fixture: ComponentFixture<CaDashboardPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CaDashboardPage]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CaDashboardPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
