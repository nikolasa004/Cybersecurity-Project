import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { SidebarNav } from '../sidebar-nav/sidebar-nav';
import { TopNavbar } from '../top-navbar/top-navbar';
import { NavigationItem } from '../../../core/models/navigation-item';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, SidebarNav, TopNavbar],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.css'
})
export class AppShell {
  @Input() sidebarTitle = '';
  @Input() navItems: NavigationItem[] = [];
  @Input() fullName = '';
  @Input() roleLabel = '';
  @Input() organization = '';
}