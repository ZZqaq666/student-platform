# Learning Service Extraction Implementation Plan

**Goal:** Extract learning-service module from monolithic Spring Boot project to multi-module Maven microservices architecture

**Architecture:** New learning-service module under parent POM, using common module for shared DTOs/Result. Follows same pattern as existing qa-service module.

**Tech Stack:** Spring Boot 3.5.7, MyBatis-Plus, Redis, MySQL, Spring Cloud OpenFeign

---

### Task 1: Create pom.xml and application entry point

**Files:**
- Create: `learning-service/pom.xml`
- Create: `learning-service/src/main/java/com/student/platform/learning/LearningApplication.java`
- Create: `learning-service/src/main/resources/application.yml`

### Task 2: Copy config files

**Files (config/):**
- Create: `learning/config/GlobalExceptionHandler.java` (from exception/ with import fixes)
- Create: `learning/config/RedisConfig.java`
- Create: `learning/config/CacheConfig.java`
- Create: `learning/config/JacksonConfig.java`
- Create: `learning/config/OpenApiConfig.java`
- Create: `learning/config/CharacterEncodingFilterConfig.java`
- Create: `learning/config/WebMvcConfig.java`
- Create: `learning/config/UserIdInterceptor.java`

### Task 3: Copy entity and mapper files

**Files (entity/ and mapper/):**
- Create: `learning/entity/LearningProgress.java`, `Book.java`, `BookChapter.java`, `BookKnowledgePoint.java`, `Course.java`, `UserBook.java`, `UserProfile.java`, `UserSubjectPreference.java`, `User.java`
- Create: `learning/mapper/LearningProgressMapper.java`, `BookMapper.java`, `BookChapterMapper.java`, `BookKnowledgePointMapper.java`, `CourseMapper.java`, `UserBookMapper.java`, `UserProfileMapper.java`, `UserSubjectPreferenceMapper.java`, `UserMapper.java`
- Create: mapper XML resources

### Task 4: Copy service interfaces and implementations

**Files (service/ and service/impl/):**
- Create: service interfaces (6)
- Create: service impls (7, with Cross-module dependency fixes)

### Task 5: Copy controllers and util

**Files:**
- Create: controllers (6 - BookController modified to remove AiService)
- Create: `learning/util/RedisUtil.java`

### Task 6: Compile and fix errors

**Commands:**
- `mvn install -pl common -DskipTests -q`
- `mvn compile -pl learning-service`
