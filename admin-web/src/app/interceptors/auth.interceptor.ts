import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

function getAccessToken(): string | null {
  const token = localStorage.getItem('adminAccessToken');
  return token && token.trim() ? token.trim() : null;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = getAccessToken();
  const router = inject(Router);

  let clonedReq = req;
  if (token) {
    clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(clonedReq).pipe(
    catchError((error) => {
      if (error.status === 401) {
        localStorage.removeItem('adminAccessToken');
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};

