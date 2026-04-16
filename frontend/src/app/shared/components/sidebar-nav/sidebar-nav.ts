import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NavigationItem } from '../../../core/models/navigation-item';

@Component({
  selector: 'app-sidebar-nav',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './sidebar-nav.html',
  styleUrl: './sidebar-nav.css'
})
export class SidebarNav {
  @Input() title = '';
  @Input() items: NavigationItem[] = [];
}