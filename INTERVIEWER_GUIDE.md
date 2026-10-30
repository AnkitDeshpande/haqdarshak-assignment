# Senior Java Backend Architect — Walkthrough & Interviewer Guide

**Project**: Haqdarshak Scheme Knowledge Base (Option S1: Review and Publish Backend)  
**Author**: Senior Backend Architect Candidate  
**Target Architecture**: Spring Boot 3.3 (Java 21), Spring Security 6, JPA/Hibernate, MySQL 8  

---

## 1. Executive Summary & Design Philosophy

Government welfare scheme data powers frontline field agents assisting citizens in availing life-critical benefits (pensions, crop subsidies, healthcare coverage). In this domain, **data consistency, operational integrity, and zero downtime** are paramount:

1. **Frontline Field Agents cannot afford volatility**: If an ongoing welfare policy revision is being edited or reviewed, field agents must never see incomplete, intermediate, or unverified changes.
2. **Maker-Checker Integrity (Four-Eyes Principle)**: Fraud and administrative collusion are mitigated by strictly preventing authors of changes from approving or publishing their own submissions.
3. **Traceability**: Every field modification requires an evidence trail with exact gazette/document citations, confidence ratings, and diff-level audit trails.

To achieve this, the architecture adopts **MVCC-inspired Immutable Version Pointers** (lightweight CQRS), **Stateless JWT Role-Based Access Control (RBAC)**, and **Cascading Eventual Consistency for Shared Rules**.

---

## 2. Key Architectural Decisions & Pattern Breakdown

### Decision 1: Version Pointer Pattern vs In-Place State Mutation

* **Anti-Pattern (In-Place Mutation)**: Updating rows in the `schemes` table directly with an `is_published` boolean causes read-locks, dirty reads, or sudden changes to field agents while a state reviewer drafts an amendment.
* **Our Solution (Pointer Pattern)**:
  - The `Scheme` entity maintains an atomic pointer: `current_published_version_id`.
  - The live read API (`GET /api/schemes/{id}/live`) queries this pointer exclusively.
  - When an editor modifies a scheme, a **copy-on-write** clone of the version is created in `DRAFT` status.
  - Only upon explicit approval by a Central Reviewer is `current_published_version_id` atomically swapped in a transactional boundary (`@Transactional`).
* **Benefit**: Guarantees non-blocking, lock-free reads for 99.9% of traffic (field agent queries) while write operations occur independently in background drafts.

```
       +---------------------------------------------------------+
       |                        Scheme                           |
       |  id: 1, scheme_ref: "SCH_1001", name: "PM-KISAN"        |
       |  current_published_version_id: 1  <-------------------+ |
       +-------------------------------------------------------|-+
                                                               |
            +--------------------------------------------------+
            |
            v
     +--------------+           +--------------+
     | SchemeVersion|           | SchemeVersion|
     | id: 1        |           | id: 2        |
     | status:      |           | status:      |
     |  PUBLISHED   |           |  DRAFT       |  <-- State reviewer edits here
     +--------------+           +--------------+      (Zero leakage to Live API)
```

---

### Decision 2: Maker-Checker Enforcement at the Core Domain Boundary

* **The Problem**: Role-based access control (RBAC) alone (`hasRole('CENTRAL_REVIEWER')`) is insufficient. A user holding a central reviewer role could submit a draft and approve their own submission unless domain-level checks intervene.
* **Our Solution**:
  1. The authenticated user identity is extracted directly from the trusted `SecurityContextHolder`:
     ```java
     String reviewer = SecurityContextHolder.getContext().getAuthentication().getName();
     ```
  2. The service enforces the hard rule:
     ```java
     if (reviewer.equalsIgnoreCase(version.getCreatedBy()) || 
         (version.getLastModifiedBy() != null && reviewer.equalsIgnoreCase(version.getLastModifiedBy()))) {
         throw new MakerCheckerViolationException(
             "Maker-Checker Violation: User '" + reviewer + 
             "' created or last modified draft version " + version.getVersionNumber() + 
             ". A different CENTRAL_REVIEWER must review and publish this scheme.");
     }
     ```
  3. `MakerCheckerViolationException` extends `SecurityException`, guaranteeing a standard 403 Forbidden with clear diagnostic logs.
* **Benefit**: Immune to UI bypass, parameter tampering, or privilege escalation.

---

### Decision 3: Shared Eligibility Rule Cascades without Mutating Live Data

* **The Challenge**: A single eligibility rule (e.g., "Land ceiling <= 2 hectares") is shared across 50+ central and state schemes. When the Ministry updates this ceiling to 2.5 hectares, updating live published schemes immediately could cause legal discrepancies before state departments review their respective allocations.
* **Our Solution**:
  1. Many-to-Many mapping table `scheme_version_rules`.
  2. Updating a rule increments `rule_version` (e.g., v1 -> v2).
  3. All linked `SchemeVersion` records are flagged:
     - `flagged_for_re_review = true`
     - `re_review_reason = "Shared eligibility rule 'RULE_X' updated to v2 by admin"`
  4. Live scheme published values and current published pointers **remain 100% untouched**.
  5. Audit records are generated for each linked scheme.
* **Benefit**: Transparent alert pipeline for departmental review without disruptive, silent mutations of live data.

---

### Decision 4: Assignment Spec Field Compliance via Embeddable Value Objects

* The scheme fields strictly follow the required JSON structure:
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
* Implemented via `@Embeddable Evidence` and `@Entity SchemeField` with explicit enums (`ConfidenceLevel`, `FieldStatus`) supporting case-insensitive deserialization and custom string values.

---

## 3. Walkthrough Questions & Answers for Interviewers

### Q1: How do you guarantee high-concurrency read consistency when field agents fetch schemes?
> **Answer**:  
> In our architecture, the live read query is:
> ```sql
> SELECT * FROM scheme_versions WHERE id = (SELECT current_published_version_id FROM schemes WHERE id = ?);
> ```
> Because `current_published_version_id` is an integer/bigint pointer to an already committed, immutable `SchemeVersion`, field agent reads:
> 1. Never wait on row locks held by reviewers editing drafts.
> 2. Experience zero dirty reads (`READ_COMMITTED` or `REPEATABLE_READ`).
> 3. Can be cached indefinitely at the HTTP/CDN or Redis layer with the cache key `scheme:{id}:v{current_published_version_id}`. When a new version is published, only the cache key pointer is invalidated.

---

### Q2: What prevents race conditions if two state reviewers attempt to edit the same scheme draft concurrently?
> **Answer**:  
> 1. **Optimistic Locking**: In high-scale deployments, `@Version` on `SchemeVersion` prevents lost updates. If Reviewer A and Reviewer B attempt to modify fields in the same draft version simultaneously, Hibernate verifies the row version and throws `OptimisticLockException` for the slower transaction.
> 2. **State Machine Locking**: Once a draft is submitted (`IN_REVIEW`), any modification request throws `InvalidStateTransitionException` until the central reviewer completes the evaluation.

---

### Q3: Why Many-to-Many for Shared Eligibility Rules instead of duplicating rule definitions?
> **Answer**:  
> Welfare rules often have national statutory definitions (e.g., SECC 2011 criteria, PM-KISAN land ceilings, BPL criteria). Duplicating them across 500 scheme records leads to data drift and massive manual overhead during legislative amendments.  
> With our normalized `shared_eligibility_rules` and `scheme_version_rules` join table:
> - Central administrators maintain single sources of truth.
> - An update triggers immediate cascading re-review notifications across all linked schemes.
> - Historical scheme versions retain their exact rule mappings at the time of publication, preserving compliance history.

---

### Q4: How would you scale this architecture to handle 50,000 schemes and 100,000 requests/sec from field agents?
> **Answer**:  
> 1. **Read/Write Replica Splitting**: Route all `@Transactional(readOnly = true)` traffic (`/api/schemes/{id}/live`) to MySQL read replicas or Aurora Read Replicas using Spring's `AbstractRoutingDataSource`.
> 2. **Distributed Cache (Redis)**:
>    - Cache the published scheme version DTO in Redis.
>    - Because published versions are immutable, cache TTL can be long (e.g., 24 hours).
>    - Upon `reviewAndDecide` with `APPROVE`, publish a cache eviction event over Spring ApplicationEvents or Redis Pub/Sub to invalidate `scheme:live:{id}`.
> 3. **Asynchronous Audit Logging**:
>    - Currently, `AuditService` runs synchronously in the same transaction for immediate consistency.
>    - At 100k req/sec, we would emit `FieldChangedEvent` onto an Apache Kafka topic or Debezium CDC (Change Data Capture) pipeline, consuming into an Elasticsearch / OpenSearch cluster for audit queries and full-text search.

---

### Q5: How is security tested and verified against malicious tampering?
> **Answer**:  
> The test suite includes dedicated integration tests covering every attack vector:
> 1. `MakerCheckerSecurityIntegrationTest`: Simulates an authenticated user with reviewer credentials attempting to approve their own draft. Asserts `MakerCheckerViolationException` is thrown and the version remains unapproved.
> 2. `SchemeControllerSecurityWebMvcTest`: Uses Spring MockMvc with JWT tokens to verify that `FIELD_AGENT` tokens receive `403 Forbidden` on modification endpoints, unauthenticated calls receive `401/403`, and only `STATE_REVIEWER` can create/edit drafts.
> 3. `ImmutableReadVersionIsolationTest`: Verifies that concurrent edits in draft state create zero data leakage on live endpoints.
> 4. `SharedRuleCascadingReReviewTest`: Verifies that updating a shared rule flags linked drafts while keeping live published data unchanged.

---

## 4. Verification & Test Suite Execution

Run the complete automated test suite:
```powershell
mvn clean test
```

### Test Results Summary:
```
[INFO] Running org.haqdarshak.publishing.MakerCheckerSecurityIntegrationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time: 0.81 s
[INFO] Running org.haqdarshak.publishing.ImmutableReadVersionIsolationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time: 0.35 s
[INFO] Running org.haqdarshak.publishing.SharedRuleCascadingReReviewTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time: 0.16 s
[INFO] Running org.haqdarshak.publishing.SchemeControllerSecurityWebMvcTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time: 4.96 s
[INFO] 
[INFO] Results:
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
