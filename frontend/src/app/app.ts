import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './features/auth/services/auth.service';
import { CurrentUserService } from './features/users/services/current-user.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private authService = inject(AuthService);
  private currentUserService = inject(CurrentUserService);

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.currentUserService.load().subscribe({
        error: () => {
          this.authService.finalizeLogout();
        }
      });
    }
  }
}