import { Component, OnInit } from '@angular/core';

import { ServoyService } from './servoy.service';
import { AllServiceService } from './allservices.service';
import { FormService } from './form.service';
import { WebsocketService } from '../sablo/websocket.service';
import { LoadingIndicatorService } from '../sablo/util/loading-indicator/loading-indicator.service';
import { ServerDataService } from './services/serverdata.service';
import { I18NProvider } from './services/i18n_provider.service';

@Component({
  selector: 'svy-main',
  templateUrl: './main.component.html'
})

export class MainComponent implements OnInit {
  title = 'Servoy NGClient';
  i18n_reconnecting_feedback: string;
  formStyle = { position: 'absolute', top: '0px', bottom: '0px' };
  navigatorStyle = { position: 'absolute', top: '0px', bottom: '0px' };

  constructor(private servoyService: ServoyService,
          private i18nProvider: I18NProvider,
          private formservice: FormService,
          public websocketService: WebsocketService,
          public loadingIndicatorService: LoadingIndicatorService,
          allService: AllServiceService,
          serverData: ServerDataService) {
    allService.init();
    serverData.init();
    this.servoyService.connect();
  }

  ngOnInit() {
      this.i18nProvider.getI18NMessages(
              'servoy.ngclient.reconnecting').then((val)=> {
                this.i18n_reconnecting_feedback = val['servoy.ngclient.reconnecting'];
      });
  }

  public get mainForm() {
    if (this.sessionProblem) return null;
    const mainForm = this.servoyService.getSolutionSettings().mainForm;
    if (mainForm && mainForm.name) return mainForm.name;
    return null;
  }

  public get navigatorForm() {
    if (this.sessionProblem) return null;
    const navigatorForm = this.servoyService.getSolutionSettings().navigatorForm;
    if (navigatorForm && navigatorForm.name &&
        navigatorForm.name.lastIndexOf('default_navigator_container.html') === -1)
        return navigatorForm.name;
    return null;
  }

  hasDefaultNavigator(): boolean {
    const cache = this.mainForm? this.formservice.getFormCacheByName(this.mainForm.toString()): null;
    return cache && cache.getComponent('svy_default_navigator') != null;
  }

  public get sessionProblem() {
    return this.servoyService.getSolutionSettings().sessionProblem;
  }

  public getNavigatorStyle() {
    const ltrOrientation = this.servoyService.getSolutionSettings().ltrOrientation;
    const orientationVar1 = ltrOrientation ? 'left' : 'right';
    const orientationVar2 = ltrOrientation ? 'right' : 'left';

    this.navigatorStyle['width'] = this.servoyService.getSolutionSettings().navigatorForm.size.width + 'px';
    this.navigatorStyle[orientationVar1] = '0px';
    delete this.navigatorStyle[orientationVar2];
    return this.navigatorStyle;
  }

  public getFormStyle() {
    const ltrOrientation = this.servoyService.getSolutionSettings().ltrOrientation;
    const orientationVar1 = ltrOrientation ? 'right' : 'left';
    const orientationVar2 = ltrOrientation ? 'left' : 'right';
    this.formStyle[orientationVar1] = '0px';
    this.formStyle[orientationVar2] = this.servoyService.getSolutionSettings().navigatorForm.size.width + 'px';
    return this.formStyle;
  }
}
