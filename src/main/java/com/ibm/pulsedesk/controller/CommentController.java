package com.ibm.pulsedesk.controller;

import com.ibm.pulsedesk.model.Comment;
import com.ibm.pulsedesk.model.CommentRequest;
import com.ibm.pulsedesk.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comments")
@CrossOrigin(origins = "*")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * POST /comments
     * Submit a new comment for AI triage.
     */
    @PostMapping
    public ResponseEntity<?> submitComment(@Valid @RequestBody CommentRequest request) {
        String channel = (request.channel() != null && !request.channel().isBlank())
                ? request.channel() : "UNKNOWN";

        Comment comment = new Comment(request.author(), request.content(), channel);
        Comment saved = commentService.submitComment(comment);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id",               saved.getId(),
                "author",           saved.getAuthor(),
                "content",          saved.getContent(),
                "channel",          saved.getChannel(),
                "createdAt",        saved.getCreatedAt().toString(),
                "convertedToTicket", saved.isConvertedToTicket()
        ));
    }

    /**
     * GET /comments
     * Retrieve all submitted comments.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllComments() {
        List<Map<String, Object>> result = commentService.getAllComments().stream()
                .map(c -> Map.<String, Object>of(
                        "id",               c.getId(),
                        "author",           c.getAuthor(),
                        "content",          c.getContent(),
                        "channel",          c.getChannel(),
                        "createdAt",        c.getCreatedAt().toString(),
                        "convertedToTicket", c.isConvertedToTicket()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }
}
