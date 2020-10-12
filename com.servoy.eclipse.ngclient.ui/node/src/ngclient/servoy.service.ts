import { Injectable } from '@angular/core';

import { WebsocketService } from '../sablo/websocket.service';
import { SabloService } from '../sablo/sablo.service';
import { ConverterService } from '../sablo/converter.service'
import { WindowRefService } from '../sablo/util/windowref.service';
import { LoggerService, LoggerFactory } from '../sablo/logger.service'
import { SabloDeferHelper } from '../sablo/defer.service';

import { SessionStorageService } from '../sablo/webstorage/sessionstorage.service'; 
import { DateConverter } from './converters/date_converter'
import { JSONObjectConverter } from './converters/json_object_converter'
import { JSONArrayConverter } from './converters/json_array_converter'
import { ValuelistConverter } from './converters/valuelist_converter'
import { FoundsetConverter } from './converters/foundset_converter'
import { FoundsetLinkedConverter } from './converters/foundsetLinked_converter'
import { I18NProvider } from './services/i18n_provider.service'
import { ViewportService } from './services/viewport.service'

import { IterableDiffers, IterableDiffer } from '@angular/core';

import { SpecTypesService } from '../sablo/spectypes.service';

import { FormcomponentConverter } from './converters/formcomponent_converter';
import { ComponentConverter } from './converters/component_converter';
import { LocaleService } from './locale.service';

class UIProperties {
  private uiProperties;

  constructor(private sessionStorageService: SessionStorageService) {
  }

  private getUiProperties() {
    if (!this.uiProperties) {
      const json = this.sessionStorageService.get('uiProperties');
      if (json) {
        this.uiProperties = JSON.parse(json);
      } else {
        this.uiProperties = {};
      }
    }
    return this.uiProperties;
  }

  public getUIProperty(key) {
    let value = this.getUiProperties()[key];
    if (value === undefined) {
      value = null;
    }
    return value;
  }
  public setUIProperty(key, value) {
    const uiProps = this.getUiProperties();
    if (value == null) delete uiProps[key];
    else uiProps[key] = value;
    this.sessionStorageService.set('uiProperties', JSON.stringify(uiProps))
  }
}

class SolutionSettings {
  public solutionName: string;
  public windowName: string;
  public enableAnchoring = true;
  public ltrOrientation = true;
  public mainForm: FormSettings;
  public navigatorForm: FormSettings;
  public sessionProblem: SessionProblem;
}

@Injectable()
export class ServoyService {
  private solutionSettings: SolutionSettings = new SolutionSettings();
  private uiProperties: UIProperties;

  private findModeShortCutCallback: any = null;
  private log: LoggerService;

  constructor(private websocketService: WebsocketService,
    private sabloService: SabloService,
    private windowRefService: WindowRefService,
    private sessionStorageService: SessionStorageService,
    private localeService: LocaleService,
    converterService: ConverterService,
    specTypesService: SpecTypesService,
    sabloDeferHelper: SabloDeferHelper,
    iterableDiffers: IterableDiffers,
    private logFactory: LoggerFactory,
    private viewportService: ViewportService) {

    this.log = logFactory.getLogger('ServoyService');
    this.uiProperties = new UIProperties(sessionStorageService)
    const dateConverter = new DateConverter();
    converterService.registerCustomPropertyHandler('svy_date', dateConverter);
    converterService.registerCustomPropertyHandler('Date', dateConverter);
    converterService.registerCustomPropertyHandler('JSON_obj', new JSONObjectConverter(converterService, specTypesService));
    converterService.registerCustomPropertyHandler('JSON_arr', new JSONArrayConverter(converterService, specTypesService, iterableDiffers));
    converterService.registerCustomPropertyHandler('valuelist', new ValuelistConverter(sabloService, sabloDeferHelper));
    converterService.registerCustomPropertyHandler('foundset',
      new FoundsetConverter(converterService, sabloService, sabloDeferHelper, viewportService, logFactory));
    converterService.registerCustomPropertyHandler('fsLinked',
      new FoundsetLinkedConverter(converterService, sabloService, viewportService, logFactory));
    converterService.registerCustomPropertyHandler('formcomponent', new FormcomponentConverter(converterService));
    converterService.registerCustomPropertyHandler('component', new ComponentConverter(converterService, viewportService, logFactory));
  }

  public connect() {
    // maybe do this with defer ($q)
    const solName = this.websocketService.getURLParameter('s');
    if (!solName) this.solutionSettings.solutionName = /.*\/([\$\w]+)\/.*/.exec(this.websocketService.getPathname())[1];
    else this.solutionSettings.solutionName = solName;
    this.solutionSettings.windowName = this.sabloService.getWindownr();
    let recordingPrefix;
    if (this.windowRefService.nativeWindow.location.search.indexOf('svy_record=true') > -1) {
      recordingPrefix = '/recording/websocket';

    }
    const wsSession = this.sabloService.connect('/solution/' + this.solutionSettings.solutionName,
                      { solution: this.solutionSettings.solutionName, clienttype: 2 }, recordingPrefix)
    // TODO find mode and anchors handling (anchors should be handles completely at the server side,
    // css positioning should go over the line)
    wsSession.onMessageObject((msg, conversionInfo) => {

      if (msg.clientnr && recordingPrefix) {
        const btn = <HTMLAnchorElement>this.windowRefService.nativeWindow.document.createElement('A');        // Create a <button> element
        btn.href = 'solutions/' + msg.clientnr + '.recording';
        btn.target = '_blank';
        btn.style.position = 'absolute';
        btn.style.right = '0px';
        btn.style.bottom = '0px';
        const t = this.windowRefService.nativeWindow.document.createTextNode('Download');
        btn.appendChild(t);                                // Append the text to <button>
        this.windowRefService.nativeWindow.document.body.appendChild(btn);
      }
      if (msg.windownr) {
        this.solutionSettings.windowName = msg.windownr;
      }
    });

    wsSession.onopen((evt) => {
      // update the main app window with the right size
      wsSession.callService('$windowService', 'resize',
        { size: { width: this.windowRefService.nativeWindow.innerWidth, height: this.windowRefService.nativeWindow.innerHeight } }, true);
      // set the correct locale, first test if it is set in the sessionstorage
      let locale = this.sessionStorageService.get('locale');
      if (locale) {
        const array = locale.split('-');
        this.localeService.setLocale(array[0], array[1], true);
      } else {
        locale = this.sabloService.getLocale();
        this.localeService.setLocale(locale.language, locale.country, true);
      }
    });
  }

  public getSolutionSettings(): SolutionSettings {
    return this.solutionSettings;
  }

  public getUIProperties(): UIProperties {
    return this.uiProperties;
  }

  public executeInlineScript(formname: string, script: string, params: any[]) {
    return this.sabloService.callService('formService', 'executeInlineScript',
                                          { 'formname': formname, 'script': script, 'params': params }, false);
  }

  public loaded(): Promise<any> {
    return this.localeService.isLoaded();
  }

  public setFindMode(formName: string, findmode: boolean) {
    if (findmode && this.findModeShortCutCallback == null) {
      this.findModeShortCutCallback = (event: KeyboardEvent) => {
        // perform find on ENTER
        if (event.keyCode === 13) {
          this.sabloService.callService('formService', 'performFind', { 'formname': formName, 'clear': true, 'reduce': true, 'showDialogOnNoResults': true }, true);
        }
      }
      this.windowRefService.nativeWindow.addEventListener('keyup', this.findModeShortCutCallback);
    } else if (findmode == false && this.findModeShortCutCallback != null) {
      this.windowRefService.nativeWindow.removeEventListener('keyup', this.findModeShortCutCallback);
      this.findModeShortCutCallback = null;
    }
  }
}



class AnchorConstants {
  public static readonly NORTH = 1;
  public static readonly EAST = 2;
  public static readonly SOUTH = 4;
  public static readonly WEST = 8;
}

export class FormSettings {
  public name: String;
  public size: { width: number, height: number };
}

export class SessionProblem {
  public viewUrl: string;
  public redirectUrl?: string;
  public redirectTimeout?: number;
  public stack?: string;
}


