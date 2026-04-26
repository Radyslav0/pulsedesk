package com.ibm.pulsedesk.service;

import com.ibm.pulsedesk.model.Comment;
import com.ibm.pulsedesk.model.Ticket;
import com.ibm.pulsedesk.repository.CommentRepository;
import com.ibm.pulsedesk.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final HuggingFaceService hfService;

    public CommentService(CommentRepository commentRepository,
                          TicketRepository ticketRepository,
                          HuggingFaceService hfService) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.hfService = hfService;
    }

    @Transactional
    public Comment submitComment(Comment comment) {
        Comment saved = commentRepository.save(comment);
        log.info("Comment #{} saved. Starting AI triage...", saved.getId());

        boolean createTicket = hfService.shouldCreateTicket(saved.getContent());
        if (createTicket) {
            String title    = hfService.generateTitle(saved.getContent());
            Ticket.Category category = hfService.generateCategory(saved.getContent());
            Ticket.Priority priority = hfService.generatePriority(saved.getContent());
            String summary  = hfService.generateSummary(saved.getContent());

            Ticket ticket = new Ticket(title, category, priority, summary, saved);
            ticketRepository.save(ticket);

            saved.setConvertedToTicket(true);
            commentRepository.save(saved);
            log.info("Ticket created for comment #{}: [{}] {} ({})", saved.getId(), category, title, priority);
        } else {
            log.info("Comment #{} did not qualify for a ticket.", saved.getId());
        }

        return saved;
    }

    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }
}
