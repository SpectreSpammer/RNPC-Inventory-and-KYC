package com.rnpc.inventory.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="rnpc_support_tickets", indexes=@Index(name="idx_support_ticket_user", columnList="user_id"))
public class SupportTicket {
    public enum Topic {
        ORDER("Order enquiry"), REPAIR("Repair follow-up"), APPOINTMENT("Appointment"), WARRANTY("Warranty"), GENERAL("General question");
        private final String label;
        Topic(String label){this.label=label;}
        public String getLabel(){return label;}
    }
    public enum Status {
        OPEN("Open"), IN_PROGRESS("In Progress"), ANSWERED("Answered"), CLOSED("Closed");
        private final String label;
        Status(String label){this.label=label;}
        public String getLabel(){return label;}
    }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="user_id",nullable=false)
    private User user;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32,columnDefinition="VARCHAR(32)")
    private Topic topic;
    @Column(nullable=false,columnDefinition="TEXT")
    private String message;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32,columnDefinition="VARCHAR(32)")
    private Status status=Status.OPEN;
    @Column(nullable=false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Version
    private Long version;
    public String getReference(){return String.format("SUP-%05d",id);}
    public Long getId(){return id;}
    public void setId(Long value){id=value;}
    public User getUser(){return user;}
    public void setUser(User value){user=value;}
    public Topic getTopic(){return topic;}
    public void setTopic(Topic value){topic=value;}
    public String getMessage(){return message;}
    public void setMessage(String value){message=value;}
    public Status getStatus(){return status;}
    public void setStatus(Status value){status=value;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(LocalDateTime value){createdAt=value;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
    public Long getVersion(){return version;}
    public void setVersion(Long value){version=value;}
}
