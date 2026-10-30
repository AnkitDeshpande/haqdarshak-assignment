# Haqdarshak Scheme Knowledge Base — Review & Publish Backend (Option S1)

An enterprise-grade, high-concurrency review and publishing backend for the Haqdarshak Scheme Knowledge Base built with **Spring Boot 3 (Java 21)**, **Spring Security**, **JPA/Hibernate**, and **MySQL**.

**GitHub Repository**: [https://github.com/AnkitDeshpande/haqdarshak-assignment.git](https://github.com/AnkitDeshpande/haqdarshak-assignment.git)  
**Postman & cURL Walkthrough**: [POSTMAN_AND_CURLS.md](POSTMAN_AND_CURLS.md)  
**Interviewer Architecture Guide**: [INTERVIEWER_GUIDE.md](INTERVIEWER_GUIDE.md)  

---

## 🏛️ Architectural Overview

The system is designed around financial/civic governance standards requiring strict **Maker-Checker verification (Four-Eyes Principle)**, **uninterrupted immutable live reads**, **diff-based field audit logging**, and **shared rule cascading change detection**.

```
                           +----------------------------+
                           |  Stateless JWT Security    |
                           +--------------+-------------+
                                          |
                      +-------------------+-------------------+
                      |                                       |
             [STATE_REVIEWER]                        [CENTRAL_REVIEWER]
         (Maker: Draft & Edit)                  (Checker: Approve & Publish)
                      |                                       |
                      v                                       v
         +-------------------------+             +-------------------------+
         | Draft Version Lifecycle |             | Maker-Checker Validation|
         |  DRAFT -> IN_REVIEW     |             | (createdBy != reviewer) |
         +------------+------------+             +------------+------------+
                      |                                       |
                      |                                       v
                      |                         +----------------------------+
                      |                         | IN_REVIEW -> PUBLISHED     |
                      |                         | Atomically update pointer: |
                      |                         | current_published_version  |
                      |                         +--------------+-------------+
                      |                                        |
                      +-------------------+--------------------+
                                          |
                                          v
                              +-----------------------+
                              |   Relational Store    |
                              |  (Schemes & Versions) |
                              +-----------+-----------+
                                          |
                                          v
                              +-----------------------+
                              |   [FIELD_AGENT]       |
                              | Read-Only Live API    |
                              | GET /schemes/{id}/live|
                              | (Zero read-lock waits)|
                              +-----------------------+
```

---

## 🔑 Core Features & Architectural Guarantees

### 1. Maker-Checker Rule Enforcement (Four-Eyes Principle)
- **Hard Rule**: A user who created or last modified a draft version (`createdBy` / `lastModifiedBy`) **cannot** approve or publish it, even if they hold the `CENTRAL_REVIEWER` role.
- **Enforcement**: Direct extraction from the authenticated `SecurityContext` (`Authentication.getName()`). Throws `MakerCheckerViolationException` (HTTP 403 Forbidden).

### 2. Immutable Read Pointer Pattern
- The `Scheme` entity holds an atomic foreign key pointer: `current_published_version_id`.
- Editing an existing published scheme never touches the published version in-place. Instead, it branches a new `SchemeVersion` in `DRAFT` status (copy-on-write clone).
- Public and field agent endpoints (`GET /api/schemes/{id}/live` and `GET /api/schemes/ref/{ref}/live`) exclusively resolve `current_published_version_id`.
- This guarantees **100% read isolation**: Field agents never observe intermediate draft edits or in-review changes.

### 3. State Machine Transitions
```
               +----------------------------------+
               |                                  | (Request Changes)
               v                                  |
          +---------+      Submit       +-----------+      Approve       +-----------+
          |  DRAFT  | --------------->  | IN_REVIEW | -----------------> | PUBLISHED |
          +---------+                   +-----------+                    +-----------+
```
- Direct promotion from `DRAFT` to `PUBLISHED` without review is prevented.
- Editing an `IN_REVIEW` version is locked until approved or sent back with review notes.

### 4. Assignment Field Schema Compliance
Scheme fields conform strictly to the assignment specification:
```json
{
  "scheme_ref": "SCH_1001",
  "field": "benefit_amount",
  "value": "6000",
  "evidence": {
    "text": "Rs. 6000 per year in 3 installments",
    "location": "Page 2, Section 3.1",
    "source_url": "https://example.gov.in/scheme_doc.pdf",
    "captured_date": "2026-10-30"
  },
  "confidence": "high",
  "status": "found",
  "note": "Extracted from official state notification"
}
```

### 5. Shared Eligibility Rules & Cascading Flags
- `SharedEligibilityRule` entities are linked to scheme versions via a Many-to-Many mapping table (`scheme_version_rules`).
- Updating a shared rule increments its version and marks all associated scheme versions with `flagged_for_re_review = true` and a detailed audit reason.
- **Crucial Guarantee**: Live published data values remain completely unchanged. Reviewers are notified to evaluate whether a new draft scheme version must be created and published.

### 6. Fine-Grained Audit Logging
- Every field-level addition, update, or shared rule cascade is persisted in the `audit_logs` table tracking:
  - `scheme_version_id`
  - `field_name`
  - `old_value`
  - `new_value`
  - `changed_by`
  - `changed_at`
  - `source_reference`

---

## 🛠️ Technology Stack

- **Language**: Java 21 LTS
- **Framework**: Spring Boot 3.3.4
- **Security**: Spring Security 6 (Stateless JWT, BCrypt, RBAC via `@PreAuthorize`)
- **Persistence**: Spring Data JPA, Hibernate ORM
- **Database**: MySQL 8.x (Production/Local), H2 (In-memory test mode)
- **Database Migrations**: Flyway (V1 Schema, V2 Seed Data)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, Spring Security Test, MockMvc

---

## 📂 Project Structure

```
c:\Users\Ankit\Desktop\Interview
├── pom.xml
├── README.md
├── INTERVIEWER_GUIDE.md
├── src
│   ├── main
│   │   ├── java/org/haqdarshak/publishing
│   │   │   ├── HaqdarshakPublishingApplication.java
│   │   │   ├── config
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── SchemeController.java
│   │   │   │   └── SharedRuleController.java
│   │   │   ├── domain
│   │   │   │   ├── entity
│   │   │   │   │   ├── AuditLog.java
│   │   │   │   │   ├── Evidence.java (Embeddable)
│   │   │   │   │   ├── Scheme.java
│   │   │   │   │   ├── SchemeField.java
│   │   │   │   │   ├── SchemeVersion.java
│   │   │   │   │   └── SharedEligibilityRule.java
│   │   │   │   └── enums
│   │   │   │       ├── ConfidenceLevel.java
│   │   │   │       ├── FieldStatus.java
│   │   │   │       ├── UserRole.java
│   │   │   │       └── VersionStatus.java
│   │   │   ├── dto
│   │   │   │   ├── EvidenceDto.java
│   │   │   │   ├── SchemeFieldDto.java
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── exception
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── InvalidStateTransitionException.java
│   │   │   │   ├── MakerCheckerViolationException.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources
│   │       ├── application.properties (MySQL default)
│   │       ├── application-h2.properties (Zero-dependency profile)
│   │       └── db/migration
│   │           ├── V1__init_schema.sql
│   │           └── V2__seed_data.sql
│   └── test
│       ├── java/org/haqdarshak/publishing
│       │   ├── MakerCheckerSecurityIntegrationTest.java       (Test 1: Maker-checker)
│       │   ├── ImmutableReadVersionIsolationTest.java         (Test 2: Immutable live reads)
│       │   ├── SharedRuleCascadingReReviewTest.java           (Test 3: Shared rule cascades)
│       │   └── SchemeControllerSecurityWebMvcTest.java        (RBAC & WebMvc security)
│       └── resources
│           └── application.properties (H2 MySQL test configuration)
```

---

## 🚀 How to Run

### Option A: Quick Run with In-Memory H2 (No Database Setup Needed)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
The server starts at `http://localhost:8080`.

### Option B: Run with MySQL
1. Ensure MySQL is running on `localhost:3306`.
2. Configure credentials in `src/main/resources/application.properties` or via environment variables:
   ```bash
   export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/haqdarshak_kb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
   export SPRING_DATASOURCE_USERNAME="root"
   export SPRING_DATASOURCE_PASSWORD="your_password"
   ```
3. Run the application (Flyway automatically applies `V1` and `V2` migrations):
   ```bash
   mvn spring-boot:run
   ```

### Option C: Run the Complete Test Suite
```bash
mvn test
```
All unit and integration tests execute against the embedded H2 database in MySQL mode.

---

## 📡 REST API Quick Reference

### 1. Authentication (JWT Generation)
Generate test tokens for any role:
```bash
POST /api/auth/token
Content-Type: application/json

{
  "username": "state_reviewer_1",
  "role": "STATE_REVIEWER"
}
```
Roles available: `STATE_REVIEWER`, `CENTRAL_REVIEWER`, `FIELD_AGENT`.

### 2. Create Scheme & Initial Draft (State Reviewer)
```bash
POST /api/schemes
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "scheme_ref": "SCH_1002",
  "name": "State Crop Insurance Scheme",
  "category": "Agriculture",
  "state": "Maharashtra",
  "shared_rule_codes": ["RULE_LAND_MARGINAL"],
  "fields": [
    {
      "scheme_ref": "SCH_1002",
      "field": "benefit_amount",
      "value": "10000",
      "evidence": {
        "text": "Rs. 10000 per hectare for insured crop loss",
        "location": "Page 5, Section 2.1",
        "source_url": "https://state.gov.in/crop_insurance.pdf",
        "captured_date": "2026-10-30"
      },
      "confidence": "high",
      "status": "found",
      "note": "State notification 2026"
    }
  ]
}
```

### 3. Update Field in Draft (State Reviewer)
```bash
PUT /api/schemes/1/fields
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "field": "benefit_amount",
  "value": "12000",
  "evidence": {
    "text": "Cabinet updated subsidy to Rs. 12000",
    "location": "Gazette Notification Oct 2026",
    "source_url": "https://state.gov.in/amendment.pdf",
    "captured_date": "2026-10-30"
  },
  "confidence": "high",
  "status": "found",
  "note": "Cabinet revised subsidy rate",
  "source_reference": "Cabinet Memo #82"
}
```

### 4. Submit Draft for Review (State Reviewer)
```bash
POST /api/schemes/1/versions/1/submit
Authorization: Bearer <TOKEN>
```

### 5. Review & Publish / Request Changes (Central Reviewer)
```bash
POST /api/schemes/1/versions/1/review
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "action": "APPROVE",
  "notes": "Verified with official state department and gazette."
}
```
*Note: If the requesting user was the maker (`createdBy` or `lastModifiedBy`), this returns `403 Forbidden` (`MAKER_CHECKER_VIOLATION`).*

### 6. Immutable Live Read (Field Agent)
```bash
GET /api/schemes/1/live
Authorization: Bearer <TOKEN>
```
Or by scheme reference:
```bash
GET /api/schemes/ref/SCH_1001/live
Authorization: Bearer <TOKEN>
```

### 7. Update Shared Eligibility Rule (Central Reviewer)
```bash
PUT /api/rules/1
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "rule_code": "RULE_LAND_MARGINAL",
  "rule_name": "Marginal & Small Farmers Criterion (Revised)",
  "description": "Expanded land ceiling to 2.5 hectares",
  "criteria_json": "{\"max_land_hectares\": 2.5}"
}
```
*Triggers cascading re-review flag on all associated versions without altering live data.*
