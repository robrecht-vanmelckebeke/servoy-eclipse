import { ICellEditorAngularComp } from '@ag-grid-community/angular';
import { ICellEditorParams } from '@ag-grid-enterprise/all-modules';
import { Directive, ElementRef, ViewChild } from '@angular/core';

import { DataGrid } from '../datagrid';

@Directive()
export class DatagridEditor implements ICellEditorAngularComp {

    @ViewChild('element') elementRef: ElementRef;
    dataGrid: DataGrid;
    params: ICellEditorParams;
    initialValue;
    instance: any;

    constructor() {
        this.instance = this;
    }

    agInit(params: ICellEditorParams): void {
        // create the cell
        this.params = params;
        this.dataGrid = params.context.componentParent;
        this.initialValue = params.value;
    }

    getValue() {
        throw new Error('Method not implemented.');
    }

    getFrameworkComponentInstance(): any {
        return this.instance;
    }
}