import http from 'k6/http';
import { check, sleep } from 'k6';
import { login, BASE_URL, authHeaders } from './common.js';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '1m',  target: 30 },
    { duration: '2m',  target: 30 },
    { duration: '30s', target: 0  },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],  // DB 조회 포함이라 1초로 여유
    http_req_failed:   ['rate<0.01'],
  },
};

export function setup() {
  return login();
}

export default function (data) {
  const headers = authHeaders(data.accessToken);

  // 랜덤 사용자 추천
  const randomRes = http.get(`${BASE_URL}/api/v1/discover/random`, { headers });
  check(randomRes, {
    'random: status 200': (r) => r.status === 200,
  });

  sleep(1);

  // 필터링 사용자 검색
  const filterRes = http.get(
    `${BASE_URL}/api/v1/discover/filter?country=KOREA&primaryLanguage=KOREAN`,
    { headers }
  );
  check(filterRes, {
    'filter: status 200': (r) => r.status === 200,
  });

  sleep(1);
}
