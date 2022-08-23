import http from 'k6/http';
import { check, group, sleep } from 'k6';
import {SharedArray} from 'k6/data';
import papaparse from './papa/papaparse.js'

//open csv file
const csvdata = open ('./data.csv');

let domain = 'END_POINT'
let url = 'https://' + domain + '/<MICROSERVICE_NAME>/v1/u1'
export const options = {
    insecureSkipTLSVerify: true,
    tlsAuth: [{domains: [domain], cert: open('./crt.pem'),key: open('./key.pem'),}, ],
    discardResponseBodies: true,
    scenarios: {
//        shared_iterations: {
//            executor: 'shared-iterations',
//            // common scenario configuration
//            startTime: '0s',
//            gracefulStop: '5s',
//            env: { EXAMPLEVAR: 'testing' },
//            tags: { example_tag: 'testing' },
//            // executor-specific configuration
//            vus: 400,
//            iterations: 8000,
//            maxDuration: '10s',
//        },
       per_vu_iterations: {
           executor: 'per-vu-iterations',
           startTime: '0s',
           vus: 40,
           iterations: 4000,
           maxDuration: '30s',
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


export function setup (){
    let results = papaparse.parse(csvdata,{header: true });
    return results.data;
}


export default function () {
    const body=JSON.stringify(
    {
        _COL: VAL,
        _COL: VAL,
        _COL: VAL,
        _COL_FROM: 'FROM_VAL',
        _COL_TO: 'TO_VAL',
    });
    const params = {
    headers:{'Content-Type': 'application/json','compression':'gzip, deflate, br',},
};

group('<MICROSERVICE_NAME>', (_) => {
    // send request
    const res = http.request('GET',url,body,params);
    check(res, {'is status 200': (r) => r.status === 200, });
    });
}
