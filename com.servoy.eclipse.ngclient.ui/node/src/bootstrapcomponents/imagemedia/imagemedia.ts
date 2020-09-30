import { Component, OnInit, Renderer2, Input, OnChanges, SimpleChanges } from '@angular/core';
import { ServoyBootstrapBasefield } from '../bts_basefield';

@Component({
  selector: 'servoybootstrap-imagemedia',
  templateUrl: './imagemedia.html',
  styleUrls: ['./imagemedia.scss']
})
export class ServoyBootstrapImageMedia extends ServoyBootstrapBasefield implements OnInit, OnChanges {

  @Input() media;
  @Input() alternate;

  imageURL: string = "bootstrapcomponents/imagemedia/images/empty.gif";

  constructor(renderer: Renderer2) {
    super(renderer);
}

  ngOnInit() {
    super.ngOnInit();
    this.updateImageURL(this.media, this.dataProviderID);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes) {
      for ( const property of Object.keys(changes) ) {
          const change = changes[property];
          switch ( property ) {
              case 'media':
                this.updateImageURL(change.currentValue, null);
                  break;
              case 'dataProviderID':
                  this.updateImageURL(null, change.currentValue);
                  break;
      }
    }
    super.ngOnChanges(changes);
    }
  }

  private updateImageURL(media: any, dataProvider: any) {
    if (media || this.media) {
      if (media) this.imageURL = media;
      // do nothing if data provider changed but media is defined
    } else if(dataProvider && dataProvider.url) {
      this.imageURL = dataProvider.url
    } else if (!dataProvider && this.servoyApi.isInDesigner()) {
      this.imageURL = "bootstrapcomponents/imagemedia/media.png"
    } else if (!dataProvider){
      this.imageURL = "bootstrapcomponents/imagemedia/images/empty.gif"
    } else {
      this.imageURL = dataProvider;
    }
  }
}