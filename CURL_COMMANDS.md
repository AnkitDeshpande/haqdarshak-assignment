# Complete cURL Commands Reference Guide

This document provides ready-to-run cURL commands for testing the **Haqdarshak Scheme Review & Publish Backend**.

You can run these in:
* **Terminal / Command Prompt / Git Bash / PowerShell**
* **Postman** (`Import` ➔ `Raw text` ➔ Paste any cURL command)

---

## ⚙️ 1. Start the Application

### Option A: In-Memory Mode (Zero Database Setup)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### Option B: MySQL Mode
```bash
mvn spring-boot:run
```

The application runs on `http://localhost:8080`.

---

## 🔑 2. Automated Token Setup (Quick Copy-Paste)

### PowerShell:
```powershell
# 1. State Reviewer (Rahul)
$RAHUL_TOKEN = (Invoke-RestMethod -Uri "http://localhost:8080/api/auth/token" -Method POST -ContentType "application/json" -Body '{"username":"state_reviewer_rahul","role":"STATE_REVIEWER"}').data.token

# 2. Central Reviewer (Priya)
$PRIYA_TOKEN = (Invoke-RestMethod -Uri "http://localhost:8080/api/auth/token" -Method POST -ContentType "application/json" -Body '{"username":"central_reviewer_priya","role":"CENTRAL_REVIEWER"}').data.token

# 3. Rahul with Central Reviewer Role (for Maker-Checker Violation Demo)
$RAHUL_CENTRAL_TOKEN = (Invoke-RestMethod -Uri "http://localhost:8080/api/auth/token" -Method POST -ContentType "application/json" -Body '{"username":"state_reviewer_rahul","role":"CENTRAL_REVIEWER"}').data.token

# 4. Field Agent (Amit)
$AMIT_TOKEN = (Invoke-RestMethod -Uri "http://localhost:8080/api/auth/token" -Method POST -ContentType "application/json" -Body '{"username":"field_agent_amit","role":"FIELD_AGENT"}').data.token
```

### Linux / Mac / Git Bash:
```bash
# 1. State Reviewer (Rahul)
export RAHUL_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/token -H "Content-Type: application/json" -d '{"username":"state_reviewer_rahul","role":"STATE_REVIEWER"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 2. Central Reviewer (Priya)
export PRIYA_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/token -H "Content-Type: application/json" -d '{"username":"central_reviewer_priya","role":"CENTRAL_REVIEWER"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 3. Rahul with Central Reviewer Role (for Maker-Checker Violation Demo)
export RAHUL_CENTRAL_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/token -H "Content-Type: application/json" -d '{"username":"state_reviewer_rahul","role":"CENTRAL_REVIEWER"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 4. Field Agent (Amit)
export AMIT_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/token -H "Content-Type: application/json" -d '{"username":"field_agent_amit","role":"FIELD_AGENT"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
```

---

## 📡 3. Step-by-Step cURL Commands

### 1. Generate Token (Manual)
```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "state_reviewer_rahul",
    "role": "STATE_REVIEWER"
  }'
```

---

### 2. Create Shared Eligibility Rule (Central Reviewer)
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

### 3. Create Scheme & Initial Draft (State Reviewer)
*Strictly conforms to the assignment JSON schema specification:*
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

---

### 4. Update Field in Draft & Record Audit Diff (State Reviewer)
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

### 5. Submit Draft for Review (State Reviewer)
Transitions version from `DRAFT` to `IN_REVIEW`:
```bash
curl -X POST http://localhost:8080/api/schemes/1/versions/1/submit \
  -H "Authorization: Bearer <RAHUL_TOKEN>"
```

---

### 6. 🚨 Maker-Checker Violation Demo (Self-Approval Rejected)
Rahul attempts to approve his own draft using his Central Reviewer token:
```bash
curl -X POST http://localhost:8080/api/schemes/1/versions/1/review \
  -H "Authorization: Bearer <RAHUL_CENTRAL_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "APPROVE",
    "notes": "Attempting self-approval"
  }'
```
#### Expected Response (HTTP 403 Forbidden):
```json
{
  "success": false,
  "message": "MAKER_CHECKER_VIOLATION: Maker-Checker Violation: User 'state_reviewer_rahul' created or last modified draft version 1. A different CENTRAL_REVIEWER must review and publish this scheme."
}
```

---

### 7. Independent Central Reviewer Approves and Publishes (Success)
Central Reviewer Priya reviews and approves:
```bash
curl -X POST http://localhost:8080/api/schemes/1/versions/1/review \
  -H "Authorization: Bearer <PRIYA_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "APPROVE",
    "notes": "Verified against the Union Budget Gazette 2026. Approved for nationwide publishing."
  }'
```

---

### 8. Field Agent Reads Live Published Scheme
Field Agent Amit queries the live endpoint:
```bash
curl -X GET http://localhost:8080/api/schemes/1/live \
  -H "Authorization: Bearer <AMIT_TOKEN>"
```
*Or by scheme reference code:*
```bash
curl -X GET http://localhost:8080/api/schemes/ref/SCH_1001/live \
  -H "Authorization: Bearer <AMIT_TOKEN>"
```

---

### 9. 🛡️ Immutable Read Isolation Demo: Edit Scheme While Live
State Reviewer Rahul edits the scheme, updating `benefit_amount` to `9000`:
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
*A new **Version 2 (DRAFT)** is branched.*

Now, Field Agent queries the live endpoint again:
```bash
curl -X GET http://localhost:8080/api/schemes/1/live \
  -H "Authorization: Bearer <AMIT_TOKEN>"
```
**Verification**: Field Agent **still receives Version 1 with `benefit_amount: 6000`**.  
The live read experiences **zero draft leakage and zero downtime**.

---

### 10. 🔄 Shared Rule Cascading Re-Review Demo
Central Reviewer Priya updates the shared rule:
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

Inspect Version 1:
```bash
curl -X GET http://localhost:8080/api/schemes/1/versions/1 \
  -H "Authorization: Bearer <PRIYA_TOKEN>"
```
*Shows `"flagged_for_re_review": true`, while live published data values remain unchanged.*

---

### 11. View Scheme Audit Trail
```bash
curl -X GET http://localhost:8080/api/schemes/1/audit \
  -H "Authorization: Bearer <PRIYA_TOKEN>"
```
*Returns the complete audit diff history.*
