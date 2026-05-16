import http from 'k6/http';
import { check, sleep } from 'k6';
import { login, BASE_URL, JSON_HEADERS } from './common.js';

export const options = {
  stages: [
    { duration: '30s', target: 10 },  // 워밍업
    { duration: '1m',  target: 30 },  // 부하 증가
    { duration: '2m',  target: 30 },  // 유지
    { duration: '30s', target: 0  },  // 쿨다운
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95%ile 응답시간 500ms 이내
    http_req_failed:   ['rate<0.01'],  // 오류율 1% 미만
  },
};

export default function () {
  // 로그인
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ loginId: 'k6test@everybuddy.com', password: 'Test1234!' }),
    { headers: JSON_HEADERS }
  );

  const loginOk = check(loginRes, {
    'login: status 200': (r) => r.status === 200,
    'login: accessToken 존재': (r) => JSON.parse(r.body).accessToken !== undefined,
  });

  if (!loginOk) {
    sleep(1);
    return;
  }

  const { refreshToken } = JSON.parse(loginRes.body);

  sleep(1);

  // 토큰 갱신
  const refreshRes = http.post(
    `${BASE_URL}/api/v1/auth/refresh`,
    JSON.stringify({ refreshToken }),
    { headers: JSON_HEADERS }
  );

  check(refreshRes, {
    'refresh: status 200': (r) => r.status === 200,
    'refresh: 새 accessToken 존재': (r) => JSON.parse(r.body).accessToken !== undefined,
  });

  sleep(1);
}
