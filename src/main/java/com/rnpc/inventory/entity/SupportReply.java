package com.rnpc.inventory.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="rnpc_support_replies")
public class SupportReply {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="ticket_id",nullable=false) private SupportTicket ticket;
    @ManyToOne(optional=false) @JoinColumn(name="author_id",nullable=false) private User author;
    @Column(nullable=false,columnDefinition="TEXT") private String message;
    @Column(nullable=false) private LocalDateTime createdAt;
    public Long getId(){return id;}
    public void setId(Long value){id=value;}
    public SupportTicket getTicket(){return ticket;}
    public void setTicket(SupportTicket value){ticket=value;}
    public User getAuthor(){return author;}
    public void setAuthor(User value){author=value;}
    public String getMessage(){return message;}
    public void setMessage(String value){message=value;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(LocalDateTime value){createdAt=value;}
}
