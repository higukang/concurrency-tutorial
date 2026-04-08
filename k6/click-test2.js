import http from 'k6/http';

// 개선 step1 : synchronized 적용
export let options = {
    vus: 100,        // 동시 사용자
    duration: '10s', // 10초 동안
};

export default function () {
    http.post('http://localhost:8080/api/click2');
}