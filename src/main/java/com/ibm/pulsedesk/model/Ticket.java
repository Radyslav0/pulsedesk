package com.ibm.pulsedesk.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(nullable = false, length = 1000)
    private String summary;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    public enum Category {
        BUG, FEATURE, BILLING, ACCOUNT, OTHER
    }

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    public Ticket(String title, Category category, Priority priority, String summary, Comment comment) {
        this.title = title;
        this.category = category;
        this.priority = priority;
        this.summary = summary;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }
}
