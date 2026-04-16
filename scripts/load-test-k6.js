/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-16
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
  scenarios: {
    runtime_validation: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.K6_RATE || 10),
      timeUnit: '1s',
      duration: __ENV.K6_DURATION || '1m',
      preAllocatedVUs: Number(__ENV.K6_VUS || 10),
    },
  },
};

const baseUrl = __ENV.LICENSE_MANAGER_URL || 'http://localhost:8080';
const clientKey = __ENV.LICENSE_CLIENT_KEY || '';
const licenseKey = __ENV.LICENSE_KEY || 'lic_load_test';
const productCode = __ENV.PRODUCT_CODE || 'desktop-app';

export default function () {
  const fingerprint = `k6-${__VU}-${__ITER}`;
  const response = http.post(
    `${baseUrl}/api/v1/runtime/licenses/validate`,
    JSON.stringify({
      key: licenseKey,
      productCode,
      fingerprint,
      version: __ENV.CLIENT_VERSION || '1.0.0',
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${clientKey}`,
      },
    },
  );

  check(response, {
    'runtime validation returns a handled response': (res) => res.status >= 200 && res.status < 500,
    'runtime validation is json': (res) => (res.headers['Content-Type'] || '').includes('application/json'),
  });
  sleep(1);
}
