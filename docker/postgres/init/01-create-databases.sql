-- 서비스별 database 생성
-- PostgreSQL 초기화 스크립트: 컨테이너 최초 기동 시 자동 실행
-- POSTGRES_USER(travel)로 연결된 상태에서 실행됨

CREATE DATABASE auth;
CREATE DATABASE schedule;
CREATE DATABASE place;
CREATE DATABASE monitor;
CREATE DATABASE briefing;
CREATE DATABASE alternative;
CREATE DATABASE payment;

-- 권한 부여
GRANT ALL PRIVILEGES ON DATABASE auth        TO travel;
GRANT ALL PRIVILEGES ON DATABASE schedule    TO travel;
GRANT ALL PRIVILEGES ON DATABASE place       TO travel;
GRANT ALL PRIVILEGES ON DATABASE monitor     TO travel;
GRANT ALL PRIVILEGES ON DATABASE briefing    TO travel;
GRANT ALL PRIVILEGES ON DATABASE alternative TO travel;
GRANT ALL PRIVILEGES ON DATABASE payment     TO travel;
