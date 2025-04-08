# SvrGate 방화벽 관리 Agent API 정의서

## 목차
1. [개요](#1-개요)
2. [공통 사항](#2-공통-사항)
3. [방화벽 정책 추가 API](#3-방화벽-정책-추가-api)
4. [방화벽 정책 삭제 API](#4-방화벽-정책-삭제-api)
5. [방화벽 정책 조회 API](#5-방화벽-정책-조회-api)
6. [에러 코드](#6-에러-코드)
7. [예시](#7-예시)

## 1. 개요
본 문서는 리눅스 서버의 방화벽(Firewalld) 정책을 관리하기 위한 API를 정의합니다. SvrGate와 각 리눅스 서버에 설치된 Agent 간의 통신 인터페이스를 제공합니다.

## 2. 공통 사항

### 2.1 기본 URL
```
https://{server-address}/api/v1/firewall
```

### 2.2 인증 방식
모든 API 호출은 인증이 필요합니다. API Key 인증 방식을 사용합니다.

```
X-API-Key: {api-key}
```

### 2.3 요청/응답 형식
- 모든 요청과 응답은 JSON 형식입니다.
- Content-Type: application/json
- 응답은 항상 다음 구조를 따릅니다:

```json
{
  "success": true/false,
  "code": "응답코드",
  "message": "메시지",
  "data": {} // 결과 데이터(선택적)
}
```

### 2.4 타임스탬프 형식
모든 날짜/시간 필드는 ISO 8601 형식(`YYYY-MM-DDTHH:mm:ss.sssZ`)을 사용합니다.

## 3. 방화벽 정책 추가 API

### 3.1 개요
새로운 방화벽 정책을 추가합니다.

### 3.2 엔드포인트
```
POST /api/v1/firewall/rules/add
```

### 3.3 요청 파라미터

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| rule.priority | Integer | O | 정책 우선순위 (낮은 값이 높은 우선순위) |
| ip.ipv4_ip | String | O | IPv4 형식의 IP 주소 |
| ip.bit | Integer | O | 넷마스크 비트 (32/24/16/8 중 하나) |
| port.mode | String | O | 포트 모드 (single/multi 중 하나) |
| port.port | String/Integer | O | single 모드일 경우 정수값, multi 모드일 경우 "시작-끝" 형식의 문자열 |
| protocol | String | O | 프로토콜 (tcp/udp 중 하나) |
| rule | String | O | 정책 종류 (accept/reject 중 하나) |
| log | Boolean | O | 로깅 여부 |
| use_timeout | Boolean | O | 타임아웃 사용 여부 (true/false) |
| timeout | Integer | use_timeout=true일 때 필수 | 정책 유효 시간(초), use_timeout=true인 경우에만 사용 |
| description | String | X | 정책 설명 |

### 3.4 요청 예시 (타임아웃 사용)
```json
{
  "rule": {
    "priority": 1
  },
  "ip": {
    "ipv4_ip": "192.168.1.0",
    "bit": 24
  },
  "port": {
    "mode": "multi",
    "port": "80-443"
  },
  "protocol": "tcp",
  "rule": "accept",
  "log": true,
  "use_timeout": true,
  "timeout": 3600,
  "description": "웹 서버 접근 허용 (1시간)"
}
```

### 3.4.1 요청 예시 (타임아웃 미사용)
```json
{
  "rule": {
    "priority": 1
  },
  "ip": {
    "ipv4_ip": "192.168.1.0",
    "bit": 24
  },
  "port": {
    "mode": "multi",
    "port": "80-443"
  },
  "protocol": "tcp",
  "rule": "accept",
  "log": true,
  "use_timeout": false,
  "description": "웹 서버 접근 허용 (영구)"
}
```

### 3.5 응답 파라미터

| 필드 | 타입 | 설명 |
|------|------|------|
| success | Boolean | API 호출 성공 여부 |
| code | String | 응답 코드 |
| message | String | 응답 메시지 |
| data.created_at | String | 정책 생성 시간 (ISO 8601 형식) |
| data.expires_at | String | 정책 만료 시간 (ISO 8601 형식, use_timeout=true인 경우에만 포함) |

### 3.6 응답 예시 (타임아웃 사용)
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "방화벽 정책이 성공적으로 추가되었습니다",
  "data": {
    "created_at": "2025-03-24T09:30:45.123Z",
    "expires_at": "2025-03-24T10:30:45.123Z"
  }
}
```

### 3.6.1 응답 예시 (타임아웃 미사용)
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "방화벽 정책이 성공적으로 추가되었습니다",
  "data": {
    "created_at": "2025-03-24T09:30:45.123Z"
  }
}
```

## 4. 방화벽 정책 삭제 API

### 4.1 개요
기존 방화벽 정책을 삭제합니다. 정책 매칭을 위한 파라미터를 사용합니다.

### 4.2 엔드포인트
```
POST /api/v1/firewall/rules/delete
```

### 4.3 요청 파라미터
정책을 식별하기 위한 충분한 정보를 제공해야 합니다.

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| ip.ipv4_ip | String | O | IPv4 형식의 IP 주소 |
| ip.bit | Integer | O | 넷마스크 비트 (32/24/16/8 중 하나) |
| port.mode | String | O | 포트 모드 (single/multi 중 하나) |
| port.port | String/Integer | O | single 모드일 경우 정수값, multi 모드일 경우 "시작-끝" 형식의 문자열 |
| protocol | String | O | 프로토콜 (tcp/udp 중 하나) |
| rule | String | O | 정책 종류 (accept/reject 중 하나) |

### 4.4 요청 예시
```json
{
  "ip": {
    "ipv4_ip": "192.168.1.0",
    "bit": 24
  },
  "port": {
    "mode": "multi",
    "port": "80-443"
  },
  "protocol": "tcp",
  "rule": "accept"
}
```

### 4.5 응답 파라미터

| 필드 | 타입 | 설명 |
|------|------|------|
| success | Boolean | API 호출 성공 여부 |
| code | String | 응답 코드 |
| message | String | 응답 메시지 |
| data.deleted_at | String | 정책 삭제 시간 (ISO 8601 형식) |

### 4.6 응답 예시
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "방화벽 정책이 성공적으로 삭제되었습니다",
  "data": {
    "deleted_at": "2025-03-24T10:15:30.456Z"
  }
}
```

## 5. 방화벽 정책 조회 API

### 5.1 개요
현재 적용된 방화벽 정책 목록을 조회합니다.

### 5.2 엔드포인트
```
GET /api/v1/firewall/rules
```

### 5.3 쿼리 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| protocol | String | X | 프로토콜 필터 (tcp/udp) |
| rule | String | X | 정책 종류 필터 (accept/reject) |
| ip | String | X | IP 주소 필터 |
| page | Integer | X | 페이지 번호 (기본값: 1) |
| size | Integer | X | 페이지 크기 (기본값: 20) |

### 5.4 요청 예시
```
GET /api/v1/firewall/rules?protocol=tcp&rule=accept&page=1&size=10
X-API-Key: your-api-key-here
```

### 5.5 응답 파라미터

| 필드 | 타입 | 설명 |
|------|------|------|
| success | Boolean | API 호출 성공 여부 |
| code | String | 응답 코드 |
| message | String | 응답 메시지 |
| data.total | Integer | 전체 정책 수 |
| data.page | Integer | 현재 페이지 |
| data.size | Integer | 페이지 크기 |
| data.rules | Array | 정책 목록 |
| data.rules[].priority | Integer | 정책 우선순위 |
| data.rules[].ip | Object | IP 정보 |
| data.rules[].ip.ipv4_ip | String | IPv4 주소 |
| data.rules[].ip.bit | Integer | 넷마스크 비트 |
| data.rules[].port | Object | 포트 정보 |
| data.rules[].port.mode | String | 포트 모드 (single/multi) |
| data.rules[].port.port | String/Integer | 포트 번호 또는 범위 |
| data.rules[].protocol | String | 프로토콜 |
| data.rules[].rule | String | 정책 종류 |

### 5.6 응답 예시
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "방화벽 정책 조회가 성공적으로 완료되었습니다",
  "data": {
    "total": 45,
    "page": 1,
    "size": 10,
    "rules": [
      {
        "priority": 1,
        "ip": {
          "ipv4_ip": "192.168.1.0",
          "bit": 24
        },
        "port": {
          "mode": "multi",
          "port": "80-443"
        },
        "protocol": "tcp",
        "rule": "accept"
      },
      {
        "priority": 2,
        "ip": {
          "ipv4_ip": "10.0.0.0",
          "bit": 8
        },
        "port": {
          "mode": "single",
          "port": 22
        },
        "protocol": "tcp",
        "rule": "accept"
      }
      // 추가 정책 생략...
    ]
  }
}
```

## 6. 에러 코드

| 코드 | 메시지 | 설명 |
|------|--------|------|
| SUCCESS | 요청이 성공적으로 처리되었습니다 | API 요청이 성공적으로 처리됨 |
| INVALID_REQUEST | 유효하지 않은 요청입니다 | 요청 형식이 잘못됨 |
| RULE_NOT_FOUND | 지정한 정책을 찾을 수 없습니다 | 일치하는 정책이 없음 |
| UNAUTHORIZED | 인증에 실패했습니다 | API 키가 없거나 유효하지 않음 |
| PERMISSION_DENIED | 권한이 없습니다 | 해당 작업에 대한 권한 부족 |
| INTERNAL_ERROR | 내부 서버 오류가 발생했습니다 | 서버 내부 오류 |
| INVALID_PARAMETER | 파라미터가 유효하지 않습니다 | 요청 파라미터가 잘못됨 |
| FIREWALL_ERROR | 방화벽 조작 중 오류가 발생했습니다 | 방화벽 명령 실행 중 오류 발생 |

## 7. 예시

### 7.1 정책 추가 요청 및 응답 (타임아웃 사용)

**요청:**
```
POST /api/v1/firewall/rules/add
X-API-Key: your-api-key-here
Content-Type: application/json

{
  "rule": {
    "priority": 5
  },
  "ip": {
    "ipv4_ip": "172.16.0.0",
    "bit": 16
  },
  "port": {
    "mode": "single",
    "port": 3306
  },
  "protocol": "tcp",
  "rule": "accept",
  "log": true,
  "use_timeout": true,
  "timeout": 7200,
  "description": "데이터베이스 서버 접근 허용 (2시간)"
}
```

**응답:**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "방화벽 정책이 성공적으로 추가되었습니다",
  "data": {
    "created_at": "2025-03-24T11:45:22.789Z",
    "expires_at": "2025-03-24T13:45:22.789Z"
  }
}
```

### 7.1.1 정책 추가 요청 및 응답 (타임아웃 미사용)

**요청:**
```
POST /api/v1/firewall/rules/add
X-API-Key: your-api-key-here
Content-Type: application/json

{
  "rule": {
    "priority": 5
  },
  "ip": {
    "ipv4_ip": "172.16.0.0",
    "bit": 16
  },
  "port": {
    "mode": "single",
    "port": 3306
  },
  "protocol": "tcp",
  "rule": "accept",
  "log": true,
  "use_timeout": false,
  "description": "데이터베이스 서버 접근 허용 (영구)"
}
```

**응답:**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "방화벽 정책이 성공적으로 추가되었습니다",
  "data": {
    "created_at": "2025-03-24T11:45:22.789Z"
  }
}
```

### 7.2 정책 삭제 요청 및 응답

**요청:**
```
POST /api/v1/firewall/rules/delete
X-API-Key: your-api-key-here
Content-Type: application/json

{
  "ip": {
    "ipv4_ip": "172.16.0.0",
    "bit": 16
  },
  "port": {
    "mode": "single",
    "port": 3306
  },
  "protocol": "tcp",
  "rule": "accept"
}
```

**응답:**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "방화벽 정책이 성공적으로 삭제되었습니다",
  "data": {
    "deleted_at": "2025-03-24T12:30:45.123Z"
  }
}
```

### 7.3 정책 조회 요청 및 응답

**요청:**
```
GET /api/v1/firewall/rules?rule=accept&protocol=tcp
X-API-Key: your-api-key-here
```

**응답:**
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "방화벽 정책 조회가 성공적으로 완료되었습니다",
  "data": {
    "total": 25,
    "page": 1,
    "size": 20,
    "rules": [
      // TCP Accept 정책들...
    ]
  }
}
```