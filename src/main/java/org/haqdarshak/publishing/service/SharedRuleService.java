package org.haqdarshak.publishing.service;

import org.haqdarshak.publishing.dto.request.SharedRuleRequest;
import org.haqdarshak.publishing.dto.response.SharedRuleResponse;

import java.util.List;

public interface SharedRuleService {

    SharedRuleResponse createRule(SharedRuleRequest request, String currentUsername);

    SharedRuleResponse updateRule(Long ruleId, SharedRuleRequest request, String currentUsername);

    List<SharedRuleResponse> getAllRules();

    SharedRuleResponse getRuleById(Long ruleId);
}
