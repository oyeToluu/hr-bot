package com.hrbotproject.hr_bot.controller;

import com.hrbotproject.hr_bot.dto.PolicyDtos.AskRequest;
import com.hrbotproject.hr_bot.dto.PolicyDtos.AskResponse;
import com.hrbotproject.hr_bot.model.PolicyDocument;
import com.hrbotproject.hr_bot.security.EmployeePrincipal;
import com.hrbotproject.hr_bot.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policy")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest req) {
        return policyService.ask(req.question());
    }

    @GetMapping("/documents")
    public List<PolicyDocument> listDocuments() {
        return policyService.listDocuments();
    }

    // HR only (enforced in SecurityConfig). uploadedByEmployeeId is
    // overwritten from the authenticated principal below - never trust
    // whatever value the client put in the request body for it.
    @PostMapping("/documents")
    public ResponseEntity<PolicyDocument> createDocument(
            @AuthenticationPrincipal EmployeePrincipal principal,
            @Valid @RequestBody PolicyDocument document
    ) {
        document.setUploadedByEmployeeId(principal.getEmployeeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createDocument(document));
    }
}
