import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import {SabloModule} from '../../sablo/sablo.module'

import { ServoyDefaultHTMLView } from './htmlview';

import {FormatFilterPipe,SvyFormat, FormattingService} from '../../ngclient/servoy_public'

describe("ServoyDefaultHTMLView", () => {
  let component: ServoyDefaultHTMLView;
  let fixture: ComponentFixture<ServoyDefaultHTMLView>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyDefaultHTMLView],
      imports: [SabloModule],
      providers: [FormatFilterPipe,FormattingService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyDefaultHTMLView);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
