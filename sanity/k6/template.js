import http from 'k6/http';
import { check, sleep } from 'k6';

let url = "https://dih.tau.ac.il:8443/get_tochnit_sems_service/v1/u1?idno=[IDNUMBER]&k_siduri_toar=6&k_siduri_tochnit=1&limit=1"

export const options = {
  insecureSkipTLSVerify: false,
  discardResponseBodies: false,
  tlsAuth: [ { cert: open('./test-client.cer'), key: open('./test-client.key'), }, ],
  scenarios: {
      // shared_iterations: {
      //     executor: 'shared-iterations',
      //     // common scenario configuration
      //     startTime: '0s',
      //     gracefulStop: '5s',
      //     env: { EXAMPLEVAR: 'testing' },
      //     tags: { example_tag: 'testing' },
      //     // executor-specific configuration
      //     vus: 400,
      //         iterations: 8000,
      //         maxDuration: '10s',
      // },
      per_vu_iterations: {
          executor: 'per-vu-iterations',
          startTime: '0s',
          vus: 10,
          iterations: 10,
          maxDuration: '15s',
      },
      // ramp_up: {
      //     executor: 'ramping-vus',
      //     startTime: '0s',
      //     startVUs: 0,
      //     stages: [
      //         { duration: '5s', target: 100 },
      //         { duration: '5s', target: 400 },
      //         { duration: '20s', target: 400 },
      //         { duration: '0s', target: 300 },
      //         { duration: '10s', target: 300 },
      //         { duration: '0s', target: 450 },
      //         { duration: '20s', target: 450 },
      //     ],
      //     gracefulRampDown: '0s',
      // },
  },
};

export default function () {
  let response = http.get(url);
  //console.log(`Response status code: ${response.status}`);
  //console.log(`Response body: ${response.body}`);
  check(response, {
    'is status 200': (r) => r.status === 200,
  });
  // sleep(1); // Wait for 1 second before making the next request
}
