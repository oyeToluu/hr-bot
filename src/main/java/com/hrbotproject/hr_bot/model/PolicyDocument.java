package com.hrbotproject.hr_bot.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "policy_documents")
public class PolicyDocument {

    @Id
    private String id;

    @NotBlank
    private String title;

    @NotEmpty
    @Valid
    private List<Section> sections;

    private String uploadedByEmployeeId;

    // MVP note: sections are stored as structured text (heading + content +
    // keywords) rather than parsed from an uploaded PDF/Word handbook. The
    // policy Q&A assistant does keyword matching over these sections rather
    // than true semantic search - a natural v2 upgrade.
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section {
        @NotBlank
        private String heading;

        @NotBlank
        private String content;

        @Builder.Default
        private List<String> keywords = List.of();
    }
}
