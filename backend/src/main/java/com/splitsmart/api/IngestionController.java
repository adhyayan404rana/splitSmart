package com.splitsmart.api;

import com.splitsmart.ingestion.ExpenseDraft;
import com.splitsmart.ingestion.NlpPipelineEngine;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final NlpPipelineEngine nlpPipelineEngine;

    @Data
    public static class ParseRequest {
        private String text;
    }

    @PostMapping("/parse")
    public ResponseEntity<ExpenseDraft> parseText(@RequestBody ParseRequest request) {
        if (request.getText() == null || request.getText().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        ExpenseDraft draft = nlpPipelineEngine.processNaturalLanguageInput(request.getText());
        return ResponseEntity.ok(draft);
    }
}
