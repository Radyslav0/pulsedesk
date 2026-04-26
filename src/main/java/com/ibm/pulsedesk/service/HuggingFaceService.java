package com.ibm.pulsedesk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.pulsedesk.model.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Service that communicates with the Hugging Face Inference API
 * using facebook/bart-large-mnli (zero-shot classification) to analyse comments.
 */
@Service
public class HuggingFaceService {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceService.class);

    @Value("${huggingface.api.url}")
    private String apiUrl;

    @Value("${huggingface.api.token}")
    private String apiToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /** Returns true when the comment describes a real problem / request. */
    public boolean shouldCreateTicket(String commentText) {
        List<String> labels = List.of("support issue", "compliment or general feedback");
        String topLabel = classify(commentText, labels);
        log.info("shouldCreateTicket top label: '{}'", topLabel);
        return topLabel.contains("support issue");
    }

    /** Generates a short title based on category + first words of comment. */
    public String generateTitle(String commentText) {
        Ticket.Category category = generateCategory(commentText);
        String snippet = commentText.length() > 60 ? commentText.substring(0, 60) + "..." : commentText;
        return "[" + category.name() + "] " + snippet;
    }

    /** Classifies the comment into one of: bug, feature, billing, account, other. */
    public Ticket.Category generateCategory(String commentText) {
        List<String> labels = List.of("bug or crash", "feature request", "billing or payment", "account or login", "other");
        String topLabel = classify(commentText, labels);
        log.info("generateCategory top label: '{}'", topLabel);
        return parseCategory(topLabel);
    }

    /** Assigns priority: low, medium, or high. */
    public Ticket.Priority generatePriority(String commentText) {
        List<String> labels = List.of("high priority urgent", "medium priority", "low priority");
        String topLabel = classify(commentText, labels);
        log.debug("generatePriority top label: '{}'", topLabel);
        return parsePriority(topLabel);
    }

    /** Produces a concise summary — first 200 chars of the comment. */
    public String generateSummary(String commentText) {
        // bart-large-mnli is a classifier, not a text generator
        // so we use the comment itself trimmed as the summary
        String clean = commentText.trim();
        return clean.length() > 200 ? clean.substring(0, 200) + "..." : clean;
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    /**
     * Calls the zero-shot classification endpoint and returns the top label.
     */
    private String classify(String text, List<String> candidateLabels) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(apiToken);

        Map<String, Object> body = Map.of(
                "inputs", text,
                "parameters", Map.of("candidate_labels", candidateLabels)
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return parseTopLabel(response.getBody());
        } catch (Exception e) {
            log.error("Hugging Face API call failed: {}", e.getMessage());
            return fallbackAnalysis(text);
        }
    }

    /**
     * bart-large-mnli returns:
     * { "labels": ["support issue", "compliment"], "scores": [0.95, 0.05] }
     * The first label is always the top (highest score).
     */
    private String parseTopLabel(String responseBody) {
        log.info("HF raw response: {}", responseBody);
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            // Формат: [{"label":"support issue","score":0.93}, ...]
            if (root.isArray() && root.size() > 0) {
                return root.get(0).get("label").asText();
            }
            // Запасной формат: {"labels": [...]}
            if (root.has("labels") && root.get("labels").isArray()) {
                return root.get("labels").get(0).asText();
            }
        } catch (Exception e) {
            log.warn("Could not parse HF response: {}", e.getMessage());
        }
        return "";
    }

    /**
     * Rule-based fallback when API is unreachable.
     */
    private String fallbackAnalysis(String input) {
        String lower = input.toLowerCase();
        if (lower.contains("bug") || lower.contains("crash") || lower.contains("error") ||
                lower.contains("broken") || lower.contains("fail") || lower.contains("not working"))
            return "bug or crash";
        if (lower.contains("feature") || lower.contains("add") || lower.contains("improve") ||
                lower.contains("would love") || lower.contains("suggest"))
            return "feature request";
        if (lower.contains("billing") || lower.contains("charge") || lower.contains("invoice") ||
                lower.contains("payment") || lower.contains("refund"))
            return "billing or payment";
        if (lower.contains("account") || lower.contains("login") || lower.contains("password") ||
                lower.contains("sign") || lower.contains("access"))
            return "account or login";
        if (lower.contains("love") || lower.contains("great") || lower.contains("thanks") ||
                lower.contains("awesome") || lower.contains("good job"))
            return "compliment or general feedback";
        return "other";
    }

    private Ticket.Category parseCategory(String raw) {
        if (raw.contains("bug"))     return Ticket.Category.BUG;
        if (raw.contains("feature")) return Ticket.Category.FEATURE;
        if (raw.contains("billing") || raw.contains("payment")) return Ticket.Category.BILLING;
        if (raw.contains("account") || raw.contains("login"))   return Ticket.Category.ACCOUNT;
        return Ticket.Category.OTHER;
    }

    private Ticket.Priority parsePriority(String raw) {
        if (raw.contains("high"))   return Ticket.Priority.HIGH;
        if (raw.contains("medium")) return Ticket.Priority.MEDIUM;
        return Ticket.Priority.LOW;
    }
}