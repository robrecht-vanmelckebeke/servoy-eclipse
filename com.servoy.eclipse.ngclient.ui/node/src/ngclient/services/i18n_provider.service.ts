import { Injectable } from '@angular/core';

import { SabloService } from '../../sablo/sablo.service';
import { Deferred } from '@servoy/public';

@Injectable({
  providedIn: 'root'
})
export class I18NProvider {
    private cachedMessages = {};

    private cachedPromises: { [s: string]: { promise?: Promise<any>; value?: any } } = {};
    private defaultTranslations = {};

    constructor(private sabloService: SabloService) {
    }

    public addDefaultTranslations(translations: {[key: string]: string}) {
        for (const key of Object.keys(translations)) {
            this.defaultTranslations[key] = translations[key];
        }
    }

    public getI18NMessages(...keys: string[]): Promise<any> {
        const retValue = {};
        const serverKeys = {};
        let serverKeysCounter = 0;
        keys.forEach(key => {
            if (this.cachedMessages[key] != null) {
                retValue[key] = this.cachedMessages[key];
            } else {
                serverKeys[serverKeysCounter++] = key;
            }
        });
        if (serverKeysCounter > 0) {
            const promiseA = this.sabloService.callService('i18nService', 'getI18NMessages', serverKeys, false);
            const promiseB = promiseA.then((result) => {
                for (const key of Object.keys(result)) {
                    this.cachedMessages[key] = result[key];
                    retValue[key] = result[key];
                }
                return retValue;
            }, (error) => Promise.reject(error));
            return promiseB;
        } else {
            const defered = new Deferred<any>();
            defered.resolve(retValue);
            return defered.promise;
        }
    }
    public getI18NMessage(key) {

        if (!this.cachedPromises[key]) {
            const promise = this.sabloService.callService('i18nService', 'getI18NMessages', { 0: key }, false).
                then((result) => {
                    if (promise['reject']) {
                        return Promise.reject(result);
                    } else {
                        const value = result[key];
                        this.cachedPromises[key] = {
                            value
                        };
                        return value;
                    }
                },
                    (error) => {
                        if (promise['reject']) {
                            delete this.cachedPromises[key]; // try again later
                        }
                        return Promise.reject(error);
                    }
                );
            this.cachedPromises[key] = {
                promise
            };
        }
        // return the value when available otherwise {{'mykey' | translate }} does not display anything
        if (this.cachedPromises[key].hasOwnProperty('value')) {
            return this.cachedPromises[key].value;
        }
        if (this.defaultTranslations[key]) {
            // return the default translation until we have a result from the server
            return this.defaultTranslations[key];
        }
        return this.cachedPromises[key].promise;
    }

    public flush() {
        this.cachedMessages = {};
        for (const key in this.cachedPromises) {
            if (this.cachedPromises.hasOwnProperty(key) && this.cachedPromises[key].promise) {
                this.cachedPromises[key].promise['reject'] = true;
            }
        }
        this.cachedPromises = {};
    }
}
