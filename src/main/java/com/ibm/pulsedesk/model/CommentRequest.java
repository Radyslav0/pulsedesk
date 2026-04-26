package com.ibm.pulsedesk.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound DTO for POST /comments
 */
public record CommentRequest(
        @NotBlank(message = "Author is required") String author,
        @NotBlank(message = "Content is required") String content,
        String channel
) {}
