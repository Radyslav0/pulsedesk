package com.ibm.pulsedesk.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Author must not be blank")
    @Column(nullable = false)
    private String author;

    @NotBlank(message = "Content must not be blank")
    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private String channel; // e.g. APP_REVIEW, WEB_FORM, CHAT

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean convertedToTicket = false;

    @OneToOne(mappedBy = "comment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Ticket ticket;

    public Comment(String author, String content, String channel) {
        this.author = author;
        this.content = content;
        this.channel = channel;
        this.createdAt = LocalDateTime.now();
    }
}
