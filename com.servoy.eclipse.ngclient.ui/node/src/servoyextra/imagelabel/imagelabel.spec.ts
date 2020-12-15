import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyExtraImageLabel } from './imagelabel';

describe('ImageLabelComponent', () => {
  let component: ServoyExtraImageLabel;
  let fixture: ComponentFixture<ServoyExtraImageLabel>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyExtraImageLabel ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyExtraImageLabel);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml']);
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});