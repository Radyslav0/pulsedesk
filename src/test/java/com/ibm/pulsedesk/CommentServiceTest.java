package com.ibm.pulsedesk;

import com.ibm.pulsedesk.model.Comment;
import com.ibm.pulsedesk.model.Ticket;
import com.ibm.pulsedesk.repository.CommentRepository;
import com.ibm.pulsedesk.repository.TicketRepository;
import com.ibm.pulsedesk.service.CommentService;
import com.ibm.pulsedesk.service.HuggingFaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private TicketRepository  ticketRepository;
    @Mock private HuggingFaceService hfService;

    @InjectMocks
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateTicketWhenAIDecidesSo() {
        Comment comment = new Comment("Alice", "The app crashes every time I open it!", "APP_REVIEW");
        comment.setId(1L);

        when(commentRepository.save(any())).thenReturn(comment);
        when(hfService.shouldCreateTicket(anyString())).thenReturn(true);
        when(hfService.generateTitle(anyString())).thenReturn("App crash on launch");
        when(hfService.generateCategory(anyString())).thenReturn(Ticket.Category.BUG);
        when(hfService.generatePriority(anyString())).thenReturn(Ticket.Priority.HIGH);
        when(hfService.generateSummary(anyString())).thenReturn("User reports app crashes on open.");

        Comment result = commentService.submitComment(comment);

        assertThat(result.isConvertedToTicket()).isTrue();
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    void shouldNotCreateTicketForCompliment() {
        Comment comment = new Comment("Bob", "Love the new design, great work!", "WEB_FORM");
        comment.setId(2L);

        when(commentRepository.save(any())).thenReturn(comment);
        when(hfService.shouldCreateTicket(anyString())).thenReturn(false);

        Comment result = commentService.submitComment(comment);

        assertThat(result.isConvertedToTicket()).isFalse();
        verify(ticketRepository, never()).save(any());
    }
}
