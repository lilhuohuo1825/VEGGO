import { HttpInterceptorFn } from '@angular/common/http';

function getAccessToken(): string | null {
  const token = localStorage.getItem('adminAccessToken');
  return token && token.trim() ? token.trim() : null;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = getAccessToken();
  if (!token) return next(req);
  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    })
  );
};

