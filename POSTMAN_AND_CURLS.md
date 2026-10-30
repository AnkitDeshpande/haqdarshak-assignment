# Postman & cURL Walkthrough Guide

This document contains **ready-to-use cURL commands** you can copy and paste directly into your terminal or import into Postman (`Import -> Raw text`).

---

## 🚀 Quick Start: Launch the Application

In a terminal, start the backend in zero-dependency local H2 mode:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
*The service starts on `http://localhost:8080`.*

---

## 📋 Complete End-to-End Walkthrough Workflow

### Step 1: Generate JWT Tokens for Each Persona

We generate stateless JWT tokens with designated RBAC roles.

#### 1.1 State Reviewer (Maker) — Rahul
```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "state_reviewer_rahul",
    "role": "STATE_REVIEWER"
  }'
```
*Copy the `token` from the response for subsequent state reviewer commands.*

#### 1.2 Central Reviewer 1 (Checker A) — Priya
```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "central_reviewer_priya",
    "role": "CENTRAL_REVIEWER"
  }'
```

#### 1.3 Central Reviewer 2 (Checker B / Dual Role) — Rahul (The Maker himself)
```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "state_reviewer_rahul",
    "role": "CENTRAL_REVIEWER"
  }'
```
*(Used to demonstrate that Maker-Checker rejects Rahul even if he holds the Central Reviewer role!)*

#### 1.4 Field Agent (Read-Only Citizen Worker) — Amit
```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "field_agent_amit",
    "role": "FIELD_AGENT"
  }'
```

---

### Step 2: Create a Shared Eligibility Rule (Central Reviewer)

Central administration defines a shared statutory rule that multiple schemes will reference.

```bash
curl -X POST http://localhost:8080/api/rules \
  -H "Authorization: Bearer <PRIYA_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "rule_code": "RULE_LAND_MARGINAL",
    "rule_name": "Marginal Farmer Land Ceiling",
    "description": "Cultivable land holding up to 2.0 hectares",
    "criteria_json": "{\"max_land_hectares\": 2.0, \"category\": \"marginal\"}"
  }'
```

---

### Step 3: Create Scheme with Initial Draft Version 1 (State Reviewer)

State Reviewer Rahul creates a new scheme linked to the shared rule and adhering strictly to the assignment's JSON schema.

```bash
curl -X POST http://localhost:8080/api/schemes \
  -H "Authorization: Bearer <RAHUL_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "scheme_ref": "SCH_1001",
    "name": "PM-KISAN Samman Nidhi",
    "category": "Agriculture",
    "state": "Central",
    "shared_rule_codes": ["RULE_LAND_MARGINAL"],
    "fields": [
      {
        "scheme_ref": "SCH_1001",
        "field": "benefit_amount",
        "value": "6000",
        "evidence": {
          "text": "Rs. 6000 per year in 3 installments of Rs. 2000 each",
          "location": "Page 2, Section 3.1",
          "source_url": "https://example.gov.in/pmkisan_guidelines.pdf",
          "captured_date": "2026-10-30"
        },
        "confidence": "high",
        "status": "found",
        "note": "Extracted from official central notification"
      },
      {
        "scheme_ref": "SCH_1001",
        "field": "disbursement_frequency",
        "value": "Every 4 Months",
        "evidence": {
          "text": "Transferred directly into bank accounts every 4 months",
          "location": "Page 3, Section 4.1",
          "source_url": "https://example.gov.in/pmkisan_guidelines.pdf",
          "captured_date": "2026-10-30"
        },
        "confidence": "high",
        "status": "found",
        "note": "Standard operational guideline"
      }
    ]
  }'
```
*Returns Scheme ID: `1`, Draft Version ID: `1`, Status: `DRAFT`.*

---

### Step 4: Update a Field in Draft & Record Audit Diff (State Reviewer)

State Reviewer Rahul updates the field `disbursement_frequency`.

```bash
curl -X PUT http://localhost:8080/api/schemes/1/fields \
  -H "Authorization: Bearer <RAHUL_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "field": "disbursement_frequency",
    "value": "Tri-annually (April, August, December)",
    "evidence": {
      "text": "Disbursed across three specific annual cycles",
      "location": "Gazette Memo 2026-12",
      "source_url": "https://example.gov.in/pmkisan_calendar.pdf",
      "captured_date": "2026-10-30"
    },
    "confidence": "high",
    "status": "found",
    "note": "Updated disbursement window breakdown",
    "source_reference": "Official Gazette Notification #99"
  }'
```

---

### Step 5: Submit Draft for Review (State Reviewer)

State Reviewer Rahul transitions the draft from `DRAFT` to `IN_REVIEW`.

```bash
curl -X POST http://localhost:8080/api/schemes/1/versions/1/submit \
  -H "Authorization: Bearer <RAHUL_TOKEN>"
```
*Status is now `IN_REVIEW`. The draft is now locked for editing.*

---

### Step 6: 🚨 DEMO HARD RULE: Maker Attempts Approval (Must Fail with 403)

Rahul attempts to approve his own draft using his Central Reviewer token.

```bash
curl -X POST http://localhost:8080/api/schemes/1/versions/1/review \
  -H "Authorization: Bearer <RAHUL_CENTRAL_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "APPROVE",
    "notes": "Attempting to approve my own draft submission"
  }'
```

#### Expected HTTP 403 Response:
```json
{
  "success": false,
  "message": "MAKER_CHECKER_VIOLATION: Maker-Checker Violation: User 'state_reviewer_rahul' created or last modified draft version 1. A different CENTRAL_REVIEWER must review and publish this scheme."
}
```

---

### Step 7: Independent Central Reviewer Approves & Publishes (Success)

Central Reviewer Priya approves and publishes Version 1.

```bash
curl -X POST http://localhost:8080/api/schemes/1/versions/1/review \
  -H "Authorization: Bearer <PRIYA_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "APPROVE",
    "notes": "Verified against the Union Budget Gazette 2026. Approved for nationwide publishing."
  }'
```
*Status is now `PUBLISHED`. The scheme's `current_published_version_id` atomically points to `1`.*

---

### Step 8: Field Agent Reads Live Published Scheme

Field Agent Amit queries the public/live endpoint.

```bash
curl -X GET http://localhost:8080/api/schemes/1/live \
  -H "Authorization: Bearer <AMIT_TOKEN>"
```
*Returns Version 1 with `benefit_amount: 6000`.*

Or by Scheme Reference code:
```bash
curl -X GET http://localhost:8080/api/schemes/ref/SCH_1001/live \
  -H "Authorization: Bearer <AMIT_TOKEN>"
```

---

### Step 9: 🛡️ DEMO IMMUTABLE READ ISOLATION: Edit Scheme While Live

State Reviewer Rahul edits the published scheme, proposing an increase in `benefit_amount` to `9000`.

```bash
curl -X PUT http://localhost:8080/api/schemes/1/fields \
  -H "Authorization: Bearer <RAHUL_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "field": "benefit_amount",
    "value": "9000",
    "evidence": {
      "text": "Cabinet proposal to enhance PM-KISAN annual grant to Rs. 9000",
      "location": "Cabinet Proposal Doc #204",
      "source_url": "https://example.gov.in/pmkisan_cabinet_proposal.pdf",
      "captured_date": "2026-10-30"
    },
    "confidence": "medium",
    "status": "found",
    "note": "Pending central budget ratification",
    "source_reference": "Cabinet Proposal 2026-B"
  }'
```
*Behind the scenes, the system automatically branched a **NEW Version 2** in `DRAFT` status with `benefit_amount: 9000`.*

#### Now, Field Agent queries the live endpoint again:
```bash
curl -X GET http://localhost:8080/api/schemes/1/live \
  -H "Authorization: Bearer <AMIT_TOKEN>"
```
**CRITICAL VERIFICATION**: The response **still returns Version 1 with `benefit_amount: 6000`**.  
The field agent experiences **zero disruption, zero draft leakage, and zero read locks**.

---

### Step 10: 🔄 DEMO SHARED RULE CASCADING RE-REVIEW

Central Reviewer Priya updates the shared rule `RULE_LAND_MARGINAL` (e.g. increasing land ceiling to 2.5 hectares).

```bash
curl -X PUT http://localhost:8080/api/rules/1 \
  -H "Authorization: Bearer <PRIYA_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "rule_code": "RULE_LAND_MARGINAL",
    "rule_name": "Marginal Farmer Land Ceiling - Expanded",
    "description": "Expanded land ceiling from 2.0 to 2.5 hectares",
    "criteria_json": "{\"max_land_hectares\": 2.5, \"category\": \"marginal_and_semi_medium\"}"
  }'
```

#### Inspect the Scheme's Version 1:
```bash
curl -X GET http://localhost:8080/api/schemes/1/versions/1 \
  -H "Authorization: Bearer <PRIYA_TOKEN>"
```
*Notice that Version 1 now shows:*
- `"flagged_for_re_review": true`
- `"re_review_reason": "Shared eligibility rule 'RULE_LAND_MARGINAL' was updated to v2 by central_reviewer_priya"`
- **Live published field values remain completely untouched!**

---

### Step 11: View Complete Field Audit Trail

Query the complete historical diff log for the scheme:

```bash
curl -X GET http://localhost:8080/api/schemes/1/audit \
  -H "Authorization: Bearer <PRIYA_TOKEN>"
```
*Returns detailed audit entries with `field_name`, `old_value`, `new_value`, `changed_by`, `changed_at`, and `source_reference`.*
