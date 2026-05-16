import http from 'k6/http';
import { check } from 'k6';

export const BASE_URL = 'https://api.everybuddy.cloud';

export const TEST_USER = {
  loginId: 'k6test@everybuddy.com',
  password: 'Test1234!',
};

export const JSON_HEADERS = {
  'Content-Type': 'application/json',
};

export function login() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify(TEST_USER),
    { headers: JSON_HEADERS }
  );

  check(res, { 'login 200': (r) => r.status === 200 });

  const body = JSON.parse(res.body);
  return {
    accessToken: body.accessToken,
    refreshToken: body.refreshToken,
  };
}

export function authHeaders(accessToken) {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${accessToken}`,
  };
}
