import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-image-slider',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './image-slider.component.html',
  styleUrl: './image-slider.component.scss',
})
export class ImageSliderComponent {
  @Input() images: string[] = [];
  @Input() alt: string = 'Product image';
  @Input() showDots: boolean = true;
  @Input() showCounter: boolean = false;

  currentIndex = 0;

  nextImage(event: Event): void {
    event.stopPropagation();
    if (this.images.length > 0) {
      this.currentIndex = (this.currentIndex + 1) % this.images.length;
    }
  }

  prevImage(event: Event): void {
    event.stopPropagation();
    if (this.images.length > 0) {
      this.currentIndex = (this.currentIndex - 1 + this.images.length) % this.images.length;
    }
  }

  goToImage(index: number, event: Event): void {
    event.stopPropagation();
    this.currentIndex = index;
  }
}
