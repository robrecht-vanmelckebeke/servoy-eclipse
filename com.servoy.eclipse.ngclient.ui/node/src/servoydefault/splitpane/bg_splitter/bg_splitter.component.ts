import { Component, Input, Output, OnChanges,SimpleChanges, EventEmitter, HostListener, AfterContentInit, ContentChildren, QueryList, Renderer2, ViewEncapsulation, ViewChild, ElementRef } from '@angular/core';

import { BGPane } from './bg_pane.component'
@Component( {
    selector: 'bg-splitter',
    template: '<div class="split-panes {{orientation}}" #element><ng-content></ng-content></div>',
    styleUrls: ['./bg_splitter.css'],
    encapsulation: ViewEncapsulation.None
} )
export class BGSplitter implements AfterContentInit , OnChanges {

    @Input() orientation = "vertical"
    @Input() divSize;
    @Input() divLocation;

    @Output() onDividerChange = new EventEmitter();

    private drag = false;
    private handler;

    @ContentChildren( BGPane )
    private panes: QueryList<BGPane>;

    @ViewChild( 'element' , {static: true})
    private elementRef: ElementRef;

    constructor( private readonly renderer: Renderer2 ) {
        this.handler = this.renderer.createElement( "div" );
        this.renderer.addClass( this.handler, "split-handler" );

        this.handler.addEventListener( 'mousedown', ( ev ) => {
            ev.preventDefault();
            this.drag = true;
        } );
    }
    
    ngOnChanges(changes:SimpleChanges) {
        if (changes["divSize"]  && changes["divSize"].currentValue >= 0 ) {
            let styleName = "width";
            if (this.orientation == "vertical") styleName = "height";
            this.renderer.setStyle(this.handler,styleName, changes["divSize"].currentValue +"px");
            this.adjustLocation(null, this.divLocation);
        }
        if (changes["divLocation"] && changes["divLocation"].currentValue >= 0) {
            this.adjustLocation(null, changes["divLocation"].currentValue);
        }
    }

    ngAfterContentInit() {
        let index = 1;
        this.panes.forEach(( item ) => {
            item.index = index++;
            if ( item.minSize == undefined ) item.minSize = 0;
        } )
        this.renderer.insertBefore( this.elementRef.nativeElement, this.handler, this.panes.last.element.nativeElement );
        
        this.adjustLocation(null,this.divLocation);
    }

    @HostListener( 'document:mouseup', ['$event'] )
    mouseup( event ) {
        if ( this.drag ) {
            let dividerLocation;
            if(this.orientation == 'vertical' ) {
                dividerLocation =this.handler.style.top; 
            }
            else {
                dividerLocation =this.handler.style.left; 
            }
            this.onDividerChange.emit( dividerLocation ? parseInt(dividerLocation.substring(0, dividerLocation.length - 2)) : 0);
        }
        this.drag = false;
    }

    @HostListener( 'mousemove', ['$event'] )
    mousemove( event ) {
        if ( !this.drag ) return;
        this.adjustLocation(event);
    }
    
    private adjustLocation(event?,wantedPosition?) {
        if (!this.panes || this.panes.length != 2) return;
        var bounds = this.elementRef.nativeElement.getBoundingClientRect();
        var pos = 0;
        if ( this.orientation == 'vertical' ) {

            var height = bounds.bottom - bounds.top;
            pos = wantedPosition >= 0?wantedPosition:event != undefined?event.clientY - bounds.top:height/2;

            if ( pos < this.panes.first.minSize ) return;
            if ( height - pos < this.panes.last.minSize ) return;
            this.renderer.setStyle( this.handler, "top", pos + 'px' );
            this.renderer.setStyle( this.panes.first.element.nativeElement, "height", pos + 'px' );
            this.renderer.setStyle( this.panes.last.element.nativeElement, "top", ( pos + this.handler.clientHeigth ) + 'px' );

        } else {

            var width = bounds.right - bounds.left;
            pos =  wantedPosition >= 0?wantedPosition:event != undefined?event.clientX - bounds.left:width/2;

            if ( pos < this.panes.first.minSize ) return;
            if ( width - pos < this.panes.last.minSize ) return;

            this.renderer.setStyle( this.handler, "left", pos + 'px' );
            this.renderer.setStyle( this.panes.first.element.nativeElement, "width", pos + 'px' );
            this.renderer.setStyle( this.panes.last.element.nativeElement, "left", ( pos + this.handler.clientWidth ) + 'px' );
        }
    }
}
