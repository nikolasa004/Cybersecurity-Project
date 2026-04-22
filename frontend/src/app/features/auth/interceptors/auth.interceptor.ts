import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getAccessToken();

  const isAuthRequest = req.url.includes('/api/auth');

  let authReq = req;
  if (token && !isAuthRequest) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(authReq).pipe(
    catchError((error) => {
      if (isAuthRequest) {
        return throwError(() => error);
      }

      if (error instanceof HttpErrorResponse && error.status === 401) {
        return authService.refreshToken().pipe(
          switchMap((res) => {
            const newAuthReq = req.clone({
              setHeaders: { Authorization: `Bearer ${res.accessToken}` }
            });
            return next(newAuthReq);
          }),
          catchError(() => {
            authService.finalizeLogout();
            return throwError(() => error);
          })
        );
      }

      if (error instanceof HttpErrorResponse && error.status === 403) {
        authService.finalizeLogout();
      }

      return throwError(() => error);
    })
  );
};