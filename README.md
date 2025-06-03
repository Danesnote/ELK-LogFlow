# 공지사항 API

RESTful API 기반의 공지사항 관리 시스템입니다.

## 주요 기능

- 공지사항 CRUD 
- 파일 첨부 기능
- 검색 기능 (제목, 내용, 등록일자)
- 조회수 관리
- 페이징 처리

## 기술 스택

- Java 17
- Spring Boot 3.5.0
- Spring Data JPA
- Hibernate
- H2 Database
- Redis (캐싱)
- Lombok

## 성능 최적화 전략

1. **캐싱**
   - Redis를 사용하여 자주 조회되는 공지사항 캐싱
   - 조회수 증가 시 캐시 무효화

2. **페이징 처리**
   - 모든 목록 조회에 페이징 적용
   - 페이지 크기 최적화

3. **파일 업로드 최적화**
   - 파일 크기 제한 (10MB)
   - 비동기 파일 처리

4. **데이터베이스 최적화**
   - N+1 문제 해결을 위한 Fetch Join 사용

## api 명세
   - http://localhost:8080/swagger-ui/index.html
     - http://localhost:8080/api-docs

## 추후 개선전략

1. ElasticSearch 도입
   - 검색 성능 향상 (like 검색의 한계 / 속도적 향상)
2. DB 인덱스 생성