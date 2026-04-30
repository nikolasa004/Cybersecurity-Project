import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, of, tap } from 'rxjs';
import { UserProfile } from '../models/user-profile.model';
import { UserService } from './user.service';

@Injectable({
  providedIn: 'root'
})
export class CurrentUserService {
  private userService = inject(UserService);

  private currentUserSubject = new BehaviorSubject<UserProfile | null>(null);
  currentUser$ = this.currentUserSubject.asObservable();

  private loaded = false;

  load(forceReload = false): Observable<UserProfile> {
    if (this.loaded && !forceReload && this.currentUserSubject.value) {
      return of(this.currentUserSubject.value);
    }

    return this.userService.getCurrentUserProfile().pipe(
      tap((user) => {
        this.currentUserSubject.next(user);
        this.loaded = true;
      })
    );
  }

  setCurrentUser(user: UserProfile): void {
    this.currentUserSubject.next(user);
    this.loaded = true;
  }

  getSnapshot(): UserProfile | null {
    return this.currentUserSubject.value;
  }

  clear(): void {
    this.currentUserSubject.next(null);
    this.loaded = false;
  }
}