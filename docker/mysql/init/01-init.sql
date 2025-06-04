-- MySQL 초기 설정 스크립트

-- UTF-8 설정
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 데이터베이스 생성 (이미 존재한다면 스킵)
CREATE DATABASE IF NOT EXISTS ssom_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 사용자 권한 설정
GRANT ALL PRIVILEGES ON ssom_db.* TO 'ssom_user'@'%';
FLUSH PRIVILEGES;

-- 기본 테이블 생성 (JPA가 자동 생성하지만 미리 생성해둘 수도 있음)
USE ssom_db;

-- 시간대 설정
SET time_zone = '+09:00';