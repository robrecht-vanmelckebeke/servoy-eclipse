import { Component, OnInit, AfterViewInit, Inject, ViewChild, ElementRef, Renderer2, QueryList, ViewChildren, OnDestroy } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { EditorSessionService, ISelectionChangedListener } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';

@Component({
    selector: 'selection-decorators',
    templateUrl: './mouseselection.component.html',
    styleUrls: ['./mouseselection.component.css']
})
// this should include lasso and all selection logic from mouseselection.js and dragselection.js
export class MouseSelectionComponent implements OnInit, AfterViewInit, ISelectionChangedListener {

    @ViewChild('lasso', { static: false }) lassoRef: ElementRef;
    @ViewChildren('selected') selectedRef: QueryList<ElementRef>;

    nodes: Array<SelectionNode> = new Array<SelectionNode>();
    contentInit: boolean = false;
    topAdjust: number;
    leftAdjust: number;
    contentRect: DOMRect;
    lassostarted: boolean = false;

    mousedownpoint: Point;

    constructor(protected readonly editorSession: EditorSessionService, @Inject(DOCUMENT) private doc: Document, protected readonly renderer: Renderer2,
        protected urlParser: URLParserService) {
        this.editorSession.addSelectionChangedListener(this);
    }

    ngOnInit(): void {
        this.editorSession.requestSelection();
        let content = this.doc.querySelector('.content-area') as HTMLElement;
        content.addEventListener('mousedown', (event) => this.onMouseDown(event));
        content.addEventListener('mouseup', (event) => this.onMouseUp(event));
        content.addEventListener('mousemove', (event) => this.onMouseMove(event));
    }

    ngAfterViewInit(): void {
        this.doc.querySelector('iframe').addEventListener('load', () => {
            this.contentInit = true;
            this.calculateAdjustToMainRelativeLocation();
            this.createNodes(this.editorSession.getSelection())
        });

        this.editorSession.stateListener.subscribe(id => {
        if (id === 'showWireframe') {
            Array.from(this.selectedRef).forEach((selectedNode) => {
                if (!this.editorSession.getState().showWireframe) {
                    this.renderer.removeClass(selectedNode.nativeElement, 'showWireframe');
                }
            });

            setTimeout(()=> {
                this.redrawDecorators();
                if (this.editorSession.getState().showWireframe) {
                    this.applyWireframe();
                }
            }, 400);//TODO can we do this without timeouts? how do we know the iframe content is there?
        }});
    }
    redrawDecorators() {
        if (this.nodes.length > 0) {
            const iframe = this.doc.querySelector('iframe');
            Array.from(this.nodes).forEach(selected => {
                let node = iframe.contentWindow.document.querySelectorAll('[svy-id="' + selected.svyid + '"]')[0];
                if (node === undefined) return;
                const position = node.getBoundingClientRect();
                selected.style = {
                    height: position.height + 'px',
                    width: position.width + 'px',
                    top: position.top + this.topAdjust + 'px',
                    left: position.left + this.leftAdjust + 'px',
                    display: 'block'
                };
            });
        }
    }

    selectionChanged(selection: Array<string>): void {
        if (this.contentInit) {
            this.createNodes(selection);
        }
    }

    private createNodes(selection: Array<string>) {
        let newNodes = new Array<SelectionNode>();
        if (selection.length > 0) {
            let iframe = this.doc.querySelector('iframe');
            let elements = iframe.contentWindow.document.querySelectorAll('[svy-id]');
            if (elements.length == 0) {
                setTimeout(() => this.createNodes(selection), 400);
                return;
            }
            Array.from(elements).forEach(node => {
                if (selection.indexOf(node.getAttribute('svy-id')) >= 0) {
                    let position = node.getBoundingClientRect();
                    this.adjustElementRect(node, position);
                    let style = {
                        height: position.height + 'px',
                        width: position.width + 'px',
                        top: position.top + this.topAdjust + 'px',
                        left: position.left + this.leftAdjust + 'px',
                        display: 'block'
                    };

                    newNodes.push({
                        style: style,
                        isResizable: this.urlParser.isAbsoluteFormLayout() ? {t:true, l:true, b:true, r:true} : {t:false, l:false, b:false, r:false},
                        svyid: node.getAttribute('svy-id')
                    })
                }
            });
        }

        this.nodes = newNodes;
    }

    private calculateAdjustToMainRelativeLocation() {
        if (!this.topAdjust) {
            let content = this.doc.querySelector('.content-area') as HTMLElement;
            this.contentRect = content.getBoundingClientRect()
            let computedStyle = window.getComputedStyle(content, null)
            this.topAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace("px", ""));
            this.leftAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace("px", ""))
        }
    }

    private onMouseDown(event: MouseEvent) {
        this.lassostarted = false;
        let point = { x: event.pageX, y: event.pageY };
        let frameElem = this.doc.querySelector('iframe');
        let frameRect = frameElem.getBoundingClientRect();
        point.x = point.x - frameRect.left;
        point.y = point.y - frameRect.top;
        this.calculateAdjustToMainRelativeLocation();

        let elements = frameElem.contentWindow.document.querySelectorAll('[svy-id]');
        let found = Array.from(elements).find((node) => {
            let position = node.getBoundingClientRect();
            this.adjustElementRect(node, position);
            let addToSelection = false;
            if (position.x <= point.x && position.x + position.width >= point.x && position.y <= point.y && position.y + position.height >= point.y) {
                addToSelection = true;
            }
            else if (parseInt(window.getComputedStyle(node, ":before").height) > 0) {
                const computedStyle = window.getComputedStyle(node, ":before");
                //the top and left positions of the before pseudo element are computed as the sum of:
                //top/left position of the element, padding Top/Left of the element and margin Top/Left of the pseudo element
                const top = position.top + parseInt(window.getComputedStyle(node).paddingTop) + parseInt(computedStyle.marginTop);
                const left = position.left + parseInt(window.getComputedStyle(node).paddingLeft) + parseInt(computedStyle.marginLeft);
                const height = parseInt(computedStyle.height);
                const width = parseInt(computedStyle.width);
                if (point.y >= top && point.x >= left && point.y <= top + height && point.x <= left + width) {
                    addToSelection = true;
                }
            }

            if (addToSelection) {
                let id = node.getAttribute('svy-id');
                let selection = this.editorSession.getSelection();
                let newNode = {
                    style: {
                        height: position.height + 'px',
                        width: position.width + 'px',
                        top: position.top + this.topAdjust + 'px',
                        left: position.left + this.leftAdjust + 'px',
                        display: 'block'
                    },
                    isResizable: this.urlParser.isAbsoluteFormLayout() ? {t:true, l:true, b:true, r:true} : {t:false, l:false, b:false, r:false},
                    svyid: node.getAttribute('svy-id')
                };
                if (event.ctrlKey || event.metaKey) {
                    let index = selection.indexOf(id);
                    if (index >= 0) {
                        this.nodes.splice(index, 1);
                        selection.splice(index, 1);
                    }
                    else {
                        this.nodes.push(newNode);
                        selection.push(id);
                    }
                }
                else {
                    let newNodes = new Array<SelectionNode>();
                    newNodes.push(newNode);
                    this.nodes = newNodes;
                    selection = [id];
                }
                this.editorSession.setSelection(selection, this);
                this.selectedRef.changes.subscribe(() => {
                    this.applyWireframe();
                })
                return node;
            }
        });
        if (!found) {
            this.nodes = [];
            this.editorSession.setSelection([], this);

            this.renderer.setStyle(this.lassoRef.nativeElement, 'left', event.pageX - this.contentRect.left + 'px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'top', event.pageY - this.contentRect.top + 'px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'width', '0px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'height', '0px');

            this.lassostarted = true;
            this.mousedownpoint = { x: event.pageX, y: event.pageY };
        }
    }

    private onMouseUp(event: MouseEvent) {
        if (this.lassostarted && this.mousedownpoint.x != event.pageX && this.mousedownpoint.y != event.pageY) {
            let frameElem = this.doc.querySelector('iframe');
            let frameRect = frameElem.getBoundingClientRect();
            let elements = frameElem.contentWindow.document.querySelectorAll('[svy-id]');
            let newNodes = new Array<SelectionNode>();
            let newSelection = new Array<string>();
            Array.from(elements).forEach((node) => {
                let position = node.getBoundingClientRect();
                this.adjustElementRect(node, position);
                if (this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + frameRect.x, y: position.y + frameRect.y }) ||
                    this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + frameRect.x + position.width, y: position.y + frameRect.y }) ||
                    this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + frameRect.x, y: position.y + frameRect.y + position.height }) ||
                    this.rectangleContainsPoint({ x: event.pageX, y: event.pageY }, { x: this.mousedownpoint.x, y: this.mousedownpoint.y }, { x: position.x + frameRect.x + position.width, y: position.y + frameRect.y + position.height })) {
                    let newNode: SelectionNode = {
                        style: {
                            height: position.height + 'px',
                            width: position.width + 'px',
                            top: position.top + this.topAdjust + 'px',
                            left: position.left + this.leftAdjust + 'px',
                            display: 'block'
                        },
                        svyid: node.getAttribute('svy-id')
                    };
                    newNodes.push(newNode);
                    newSelection.push(node.getAttribute('svy-id'))
                }
            });
            this.nodes = newNodes;
            this.editorSession.setSelection(newSelection, this);
        }
        this.lassostarted = false;
        this.applyWireframe();
    }

    private applyWireframe() {
        Array.from(this.selectedRef).forEach((selectedNode) => {
            this.applyWireframeForNode(selectedNode);
        });
    }
    applyWireframeForNode(selectedNode: ElementRef<any>) {
        let iframe = this.doc.querySelector('iframe');
        let node = iframe.contentWindow.document.querySelectorAll('[svy-id="' + selectedNode.nativeElement.getAttribute('id') + '"]')[0];
        if (node === undefined) return;
        const position = node.getBoundingClientRect();
        // TODO is && node.getAttribute('svy-layoutname') needed??
        if (node.classList.contains('svy-layoutcontainer') && !node.getAttribute('data-maincontainer') && position.width > 0 && position.height > 0) {
        	this.renderer.setAttribute(selectedNode.nativeElement, 'svytitle', node.getAttribute('svy-title'));
            if (this.editorSession.getState().showWireframe) {
            	this.renderer.addClass(selectedNode.nativeElement, 'showWireframe');
            }
            selectedNode.nativeElement.style.setProperty('--svyBackgroundColor', window.getComputedStyle(node).backgroundColor);
            if (node.classList.contains('maxLevelDesign')) {
            //fix for IE container background, the one above is still needed for the ::before pseudoelement
            selectedNode.nativeElement.style.setProperty('backgroundColor', window.getComputedStyle(node).backgroundColor);
            this.renderer.addClass(selectedNode.nativeElement, 'maxLevelDesign');
		    }
	    }
    }

    private onMouseMove(event: MouseEvent) {
        if (this.lassostarted) {
            if (event.pageX < this.mousedownpoint.x) {
                this.renderer.setStyle(this.lassoRef.nativeElement, 'left', event.pageX - this.contentRect.left + 'px');
            }
            if (event.pageY < this.mousedownpoint.y) {
                this.renderer.setStyle(this.lassoRef.nativeElement, 'top', event.pageY - this.contentRect.top + 'px');
            }
            let currentWidth = event.pageX - this.mousedownpoint.x;
            let currentHeight = event.pageY - this.mousedownpoint.y;
            this.renderer.setStyle(this.lassoRef.nativeElement, 'width', Math.abs(currentWidth) + 'px');
            this.renderer.setStyle(this.lassoRef.nativeElement, 'height', Math.abs(currentHeight) + 'px');
        }
    }

    private rectangleContainsPoint(p1: Point, p2: Point, toCheck: Point): boolean {
        if (p1.x > p2.x) {
            let temp = p1.x;
            p1.x = p2.x;
            p2.x = temp;
        }
        if (p1.y > p2.y) {
            let temp = p1.y;
            p1.y = p2.y;
            p2.y = temp;
        }
        if (p1.x <= toCheck.x && p2.x >= toCheck.x && p1.y <= toCheck.y && p2.y >= toCheck.y) {
            return true;
        }
        return false;
    }

    private adjustElementRect(node: Element, position: DOMRect) {
        if (position.width == 0 || position.height == 0) {
            let correctWidth = position.width;
            let correctHeight = position.height;
            let currentNode = node.parentElement;
            while (correctWidth == 0 || correctHeight == 0) {
                let parentPosition = currentNode.getBoundingClientRect();
                correctWidth = parentPosition.width;
                correctHeight = parentPosition.height;
                currentNode = currentNode.parentElement;
            }
            if (position.width == 0) position.width = correctWidth;
            if (position.height == 0) position.height = correctHeight;
        }
    }
}
export class SelectionNode {
    svyid:string;
    style: any;
    isResizable?: ResizeDefinition;
}
class Point {
    x: number;
    y: number;
}
class ResizeDefinition {
    t: boolean;
    l: boolean;
    b: boolean;
    r: boolean;
}
