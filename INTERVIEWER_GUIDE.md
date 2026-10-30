# Senior Java Backend Architect — Walkthrough & Interviewer Guide

**Project**: Haqdarshak Scheme Knowledge Base (Option S1: Review and Publish Backend)  
**Author**: Senior Backend Architect  
**Tech Stack**: Spring Boot 3.3.4 (Java 21), Spring Security 6, JPA/Hibernate, MySQL 8  
**Repository**: [https://github.com/AnkitDeshpande/haqdarshak-assignment.git](https://github.com/AnkitDeshpande/haqdarshak-assignment.git)  

---

## 🧭 Executive Summary: What We Built, Why We Built It, and What Problems It Solves

### 1. What Problem Does It Solve?

In government welfare systems (like Haqdarshak’s Scheme Knowledge Base), frontline field agents guide eligible citizens to claim critical government welfare entitlements (crop loss compensation, pensions, maternal health stipends). 

In this operating environment, traditional CRUD systems suffer from **catastrophic operational failure modes**:
1. **Citizen Entitlement Misdirection (The Dirty Read Problem)**: If an administrative officer is drafting a policy revision (e.g., revising eligible land holdings from 2 hectares to 1 hectare), an in-place database update immediately exposes incomplete, draft data to thousands of field agents. Agents turn away eligible citizens or submit invalid claims that get rejected.
2. **Administrative Fraud & Collusion (The Self-Approval Problem)**: Without strict Maker-Checker enforcement, an authorized reviewer could unilaterally alter welfare disbursement criteria or payout amounts and publish it without peer oversight.
3. **Statutory Criteria Drift**: Central criteria (such as Below Poverty Line income thresholds or small-farmer land ceilings) are shared across dozens of schemes. When a statutory rule changes, disjointed manual edits cause inconsistent, contradictory scheme requirements across different departments.
4. **Lack of Evidentiary Compliance**: In government audits and judicial disputes, a system must prove *what* was changed, *when*, *by whom*, and *on the basis of which official gazette notification*.

---

### 2. What We Did & Why We Did It

| Architectural Component | What We Did | Why We Did It (Engineering Rationale) |
| :--- | :--- | :--- |
| **Immutable Version Pointers (CQRS-lite)** | The `Scheme` table stores a single `current_published_version_id` pointer. Editing a published scheme forks a new `SchemeVersion` in `DRAFT` status (Copy-on-Write). | Field agents query the committed published pointer with **zero lock waits, zero dirty reads, and zero downtime**, completely isolated from ongoing administrative draft revisions. |
| **Maker-Checker Rule (Four-Eyes Principle)** | Implemented strict programmatic validation extracting user ID directly from `SecurityContextHolder.getContext().getAuthentication().getName()`. Throws `MakerCheckerViolationException` (HTTP 403) if `createdBy` or `lastModifiedBy` matches the reviewer. | Eliminates privilege escalation, unauthorized self-publishing, and administrative collusion. Even a user holding the `CENTRAL_REVIEWER` role cannot approve their own drafts. |
| **Assignment Spec Field Model** | Modeled scheme fields with embedded `Evidence` (`text`, `location`, `sourceUrl`, `capturedDate`), `confidence`, `status`, and `note`. | Conforms 100% to the assignment's JSON schema spec. Provides verifiable provenance directly tied to the field values. |
| **Shared Eligibility Rule Cascades** | Many-to-Many mapping (`scheme_version_rules`) between shared rules and scheme versions. Updating a rule increments `rule_version` and flags linked versions (`flagged_for_re_review = true`) with audit reasons. | Notifies reviewers of upstream legislative changes **without silently mutating live published data**. Preserves operational stability until humans officially review and publish an updated draft. |
| **Diff-Level Field Audit Logging** | `AuditLog` records every individual field change (`field_name`, `old_value`, `new_value`, `changed_by`, `changed_at`, `source_reference`). | Guarantees complete non-repudiation, governance compliance, and forensic traceability. |

---

### 3. What Assumptions We Made and Why

#### Assumption 1: Stateless Authentication with JWT & Direct SecurityContext Resolution
* **What**: We chose stateless JWT authentication with roles (`STATE_REVIEWER`, `CENTRAL_REVIEWER`, `FIELD_AGENT`) embedded directly in token claims, extracting the identity from `Authentication.getName()`.
* **Why**: Stateful HTTP sessions do not scale horizontally across multi-node Kubernetes clusters without sticky sessions or distributed session stores (like Redis session replication). Stateless JWT enables high-throughput horizontal scaling and simplifies zero-trust API security.

#### Assumption 2: Maker-Checker Disqualifies Both `createdBy` AND `lastModifiedBy`
* **What**: We assumed that if User A creates a draft, and User B modifies any field in that draft, **neither User A nor User B** can act as the Central Reviewer to approve and publish that version.
* **Why**: If we only checked `createdBy`, a malicious reviewer could take an honest draft, inject fraudulent criteria or values as `lastModifiedBy`, and then approve it themselves. Checking both creator and last modifier guarantees complete Four-Eyes integrity.

#### Assumption 3: Copy-on-Write Version Branching for Published Schemes
* **What**: We assumed that once a version is `PUBLISHED`, its fields are frozen and immutable forever. Any subsequent edit on the scheme branches a new `SchemeVersion` (e.g., v1 -> v2) in `DRAFT` status, pre-populated with the published version's fields and shared rules.
* **Why**: Overwriting in-place destroys historical fidelity, creates read-write lock contention, and makes rollback impossible. Copy-on-write versioning guarantees that historical versions remain intact for audits and retroactive claim verifications.

#### Assumption 4: Shared Rule Updates Must NOT Auto-Mutate Live Data
* **What**: We assumed that when an administrator updates a shared rule (e.g., expanding land ceiling from 2.0 to 2.5 hectares), the live published scheme data must **not** automatically mutate. Instead, linked versions are marked with `flagged_for_re_review = true` and an audit record is logged.
* **Why**: In government operations, statutory changes do not automatically apply to every individual welfare scheme without departmental budget allocation and formal notification. Auto-updating live data could trigger unlawful benefit disbursements. Human-in-the-loop review is a hard legal necessity.

#### Assumption 5: Read/Write Traffic Asymmetry (99:1 Read-to-Write Ratio)
* **What**: We assumed that field agent reads on live published schemes outnumber administrative draft writes by several orders of magnitude.
* **Why**: Thousands of field workers and millions of citizens access scheme eligibility constantly throughout the day, while government policy updates occur intermittently (e.g., annual budgets, legislative sessions). Therefore, the read path is optimized for $O(1)$ pointer resolution and zero-lock execution.

#### Assumption 6: Embedded Value Object for Field Evidence
* **What**: We modeled `Evidence` as a JPA `@Embeddable` within `SchemeField` rather than a separate normalized table.
* **Why**: Evidence (gazette text, page citation, document URL, captured date) is intrinsically bound to that specific field value evaluation. It has no independent lifecycle outside that field. Modeling it as an embeddable avoids unnecessary table joins and maximizes query performance.

---

## 4. End-to-End Walkthrough & Interviewer Q&A

### Q1: How does the system guarantee zero downtime and no dirty reads during concurrent editing?
> **Answer**:  
> Through our **Immutable Read Pointer Architecture**:
> ```sql
> SELECT * FROM scheme_versions WHERE id = (SELECT current_published_version_id FROM schemes WHERE id = ?);
> ```
> Live read requests (`GET /api/schemes/{id}/live`) only resolve the committed version pointed to by `current_published_version_id`.
> When a State Reviewer edits fields, Hibernate executes updates on an uncommitted or draft `SchemeVersion` row (e.g., Version 2). The live pointer continues pointing to Version 1. Field agents experience:
> 1. Zero lock waits (no shared or exclusive row lock conflicts with editors).
> 2. Zero data leakage (draft values like proposed subsidy amounts never appear in live queries).
> 3. Zero downtime during publishing: swapping `current_published_version_id = 2` is a single atomic SQL update inside a `@Transactional` block.

---

### Q2: How does the Maker-Checker rule prevent privilege escalation or bypass?
> **Answer**:  
> 1. **Identity Extraction**: The user identity is extracted from the cryptographically verified JWT inside `SecurityContextHolder`, not from any request body parameter (which could be spoofed).
> 2. **Dual-Role Trap Protection**: Even if a senior officer holds both `ROLE_STATE_REVIEWER` and `ROLE_CENTRAL_REVIEWER` authorities in their JWT, the domain logic inspects:
>    ```java
>    if (reviewer.equalsIgnoreCase(version.getCreatedBy()) || 
>        (version.getLastModifiedBy() != null && reviewer.equalsIgnoreCase(version.getLastModifiedBy()))) {
>        throw new MakerCheckerViolationException(...);
>    }
>    ```
> 3. **Verification in Test Suite**: Verified by `MakerCheckerSecurityIntegrationTest.java`, where a user with Central Reviewer credentials attempts self-approval and is rejected with `MAKER_CHECKER_VIOLATION`.

---

### Q3: How would you scale this architecture to 50,000 schemes and 100,000 requests/second from field agents?
> **Answer**:  
> 1. **Database Read/Write Splitting**:
>    - 99.9% of traffic (`GET /api/schemes/{id}/live`) is read-only.
>    - Configure Spring Data JPA with `AbstractRoutingDataSource` to route all `@Transactional(readOnly = true)` methods to MySQL Read Replicas / AWS Aurora Read Replicas.
> 2. **Distributed Redis Caching with Pointer Keys**:
>    - Cache the JSON payload of the published version under the key `scheme:live:{schemeId}`.
>    - Because published versions are immutable, cache TTL can be set to 24 hours.
>    - When a Central Reviewer approves a new draft (`reviewAndDecide` with `APPROVE`), publish a Spring Application Event that evicts `scheme:live:{schemeId}` from Redis.
> 3. **Asynchronous Audit Logging via CDC / Kafka**:
>    - For 100k writes/sec, change `AuditService` from synchronous JPA writes to emitting events to an Apache Kafka topic, consumed asynchronously into an Elasticsearch or ClickHouse cluster for fast search and compliance reporting.

---

### Q4: How is database indexing optimized for scheme queries?
> **Answer**:  
> Explicit indexes were defined in `V1__init_schema.sql` and JPA entities:
> - `schemes`: Unique index on `scheme_ref` for $O(1)$ lookups by business code (`SCH_1001`).
> - `scheme_versions`: Composite index on `(scheme_id, status)` and index on `status` to instantly retrieve active drafts or published versions without table scans.
> - `scheme_fields`: Index on `scheme_version_id` and `field_name` for fast field joining.
> - `shared_eligibility_rules`: Unique index on `rule_code`.
> - `audit_logs`: Indexes on `scheme_version_id` and `scheme_ref` for instant historical audit retrieval.

---

## 5. Automated Test Suite Results (`mvn test`)

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.haqdarshak.publishing.ImmutableReadVersionIsolationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time: 0.35 s
[INFO] Running org.haqdarshak.publishing.MakerCheckerSecurityIntegrationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time: 0.81 s
[INFO] Running org.haqdarshak.publishing.SchemeControllerSecurityWebMvcTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time: 4.96 s
[INFO] Running org.haqdarshak.publishing.SharedRuleCascadingReReviewTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time: 0.16 s
[INFO] 
[INFO] Results:
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 6. Ready-to-Copy Postman & cURL Documentation
Refer to [**`POSTMAN_AND_CURLS.md`**](file:///c:/Users/Ankit/Desktop/Interview/POSTMAN_AND_CURLS.md) for 11 ready-to-run cURL commands covering tokens, scheme creation, field edits, maker-checker rejection, approval, live read isolation, and shared rule cascades.
