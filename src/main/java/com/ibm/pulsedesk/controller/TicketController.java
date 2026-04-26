package com.ibm.pulsedesk.controller;

import com.ibm.pulsedesk.model.Ticket;
import com.ibm.pulsedesk.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/tickets")
@CrossOrigin(origins = "*")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * GET /tickets
     * Return all generated tickets.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllTickets() {
        List<Map<String, Object>> result = ticketService.getAllTickets().stream()
                .map(this::toMap)
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /tickets/{ticketId}
     * Return a single ticket by ID.
     */
    @GetMapping("/{ticketId}")
    public ResponseEntity<?> getTicketById(@PathVariable Long ticketId) {
        Optional<Ticket> ticket = ticketService.getTicketById(ticketId);
        return ticket
                .<ResponseEntity<?>>map(t -> ResponseEntity.ok(toMap(t)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(Ticket t) {
        return Map.of(
                "id",        t.getId(),
                "title",     t.getTitle(),
                "category",  t.getCategory().name(),
                "priority",  t.getPriority().name(),
                "summary",   t.getSummary(),
                "createdAt", t.getCreatedAt().toString(),
                "commentId", t.getComment().getId()
        );
    }
}
