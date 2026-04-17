import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-top-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './top-navbar.html',
  styleUrl: './top-navbar.css'
})
export class TopNavbar {
  @Input() fullName = '';
  @Input() roleLabel = '';
  @Input() organization = '';
}