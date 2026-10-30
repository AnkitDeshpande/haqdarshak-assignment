-- Haqdarshak Scheme Knowledge Base Seed Data
-- Flyway Migration V2: Synthetic Schemes & Shared Rules

-- 1. Insert Shared Eligibility Rules
INSERT INTO shared_eligibility_rules (id, rule_code, rule_name, description, criteria_json, rule_version, updated_by, updated_at)
VALUES 
(1, 'RULE_LAND_MARGINAL', 'Marginal & Small Farmers Criterion', 'Cultivable land holding up to 2 hectares (5 acres)', '{"max_land_hectares": 2.0, "land_types": ["irrigated", "unirrigated"]}', 1, 'admin_central', CURRENT_TIMESTAMP),
(2, 'RULE_INCOME_BPL', 'Below Poverty Line (BPL) Household', 'Annual family income below state poverty threshold', '{"max_annual_income_inr": 120000, "bpl_card_required": true}', 1, 'admin_central', CURRENT_TIMESTAMP);

-- 2. Insert Scheme: PM-KISAN (SCH_1001)
INSERT INTO schemes (id, scheme_ref, name, category, state, current_published_version_id, created_at, updated_at)
VALUES 
(1, 'SCH_1001', 'PM-KISAN Samman Nidhi', 'Agriculture', 'Central', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. Insert Published Version 1 for SCH_1001
INSERT INTO scheme_versions (id, scheme_id, version_number, status, review_notes, flagged_for_re_review, re_review_reason, created_by, last_modified_by, approved_by, created_at, updated_at, published_at)
VALUES 
(1, 1, 1, 'PUBLISHED', 'Approved by central reviewer after cross-verification with official portal.', FALSE, NULL, 'state_reviewer_mh', 'state_reviewer_mh', 'central_reviewer_1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 4. Insert Scheme Fields adhering strictly to JSON Schema Spec (11 columns matched to 11 values)
INSERT INTO scheme_fields (id, scheme_version_id, field_name, field_value, evidence_text, evidence_location, evidence_source_url, evidence_captured_date, confidence, status, note)
VALUES 
(1, 1, 'benefit_amount', '6000', 'Rs. 6000 per year in 3 equal installments of Rs. 2000 each', 'Page 2, Section 3.1', 'https://example.gov.in/pmkisan_doc.pdf', '2026-10-30', 'HIGH', 'FOUND', 'Extracted from official state notification'),
(2, 1, 'eligibility_criteria', 'Small and marginal farmer families having cultivable landholding up to 2 hectares', 'Landholding criteria defined for small and marginal land owners', 'Page 3, Section 4.2', 'https://example.gov.in/pmkisan_guidelines.pdf', '2026-10-30', 'HIGH', 'FOUND', 'Verified against central database'),
(3, 1, 'disbursement_frequency', 'Every 4 months', 'Disbursement schedule specified as tri-annual direct benefit transfer', 'Page 4, Section 5.1', 'https://example.gov.in/pmkisan_faq.pdf', '2026-10-30', 'HIGH', 'FOUND', 'Standard operational guideline');

-- 5. Associate Scheme Version 1 with Shared Rule 1
INSERT INTO scheme_version_rules (scheme_version_id, rule_id)
VALUES (1, 1);

-- 6. Insert Initial Audit Log for Scheme Version 1
INSERT INTO audit_logs (scheme_version_id, scheme_ref, field_name, old_value, new_value, changed_by, changed_at, source_reference)
VALUES 
(1, 'SCH_1001', 'benefit_amount', NULL, '6000', 'state_reviewer_mh', CURRENT_TIMESTAMP, 'https://example.gov.in/pmkisan_doc.pdf'),
(1, 'SCH_1001', 'eligibility_criteria', NULL, 'Small and marginal farmer families having cultivable landholding up to 2 hectares', 'state_reviewer_mh', CURRENT_TIMESTAMP, 'https://example.gov.in/pmkisan_guidelines.pdf');
