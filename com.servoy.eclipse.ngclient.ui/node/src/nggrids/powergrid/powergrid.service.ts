import { Injectable } from '@angular/core';

@Injectable()
export class PowergridService {
    public iconConfig: any;
    public toolPanelConfig: any;
    public gridOptions: any;
    public localeText: any;
    public columnOptions: any;
    public mainMenuItemsConfig: any;

    /**
     * Creates an empty icon configuration object
     *
     * @return object
     */
    createIconConfig() {
        return {
            iconGroupExpanded: 'glyphicon glyphicon-minus ag-icon',
            iconGroupContracted: 'glyphicon glyphicon-plus ag-icon',
            iconRefreshData: 'glyphicon glyphicon-refresh'
        };
    }

    /**
     * Creates an empty toolpanel configuration object
     *
     * @return object
     */
    createToolPanelConfig() {
        return {};
    }

    /**
     * Creates an empty mainMenuItems configuration object
     *
     * @return object
     */
    createMainMenuItemsConfig() {
        return {};
    }
}
