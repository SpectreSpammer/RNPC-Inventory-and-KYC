package com.rnpc.inventory.dto;
import com.rnpc.inventory.entity.SupportTicket.Topic;
import jakarta.validation.constraints.*;
public class SupportTicketDto {
    @NotNull(message="Choose a subject.") private Topic topic;
    @NotBlank(message="Describe your concern.") @Size(max=5000,message="Use 5,000 characters or fewer.") private String message;
    public Topic getTopic(){return topic;}
    public void setTopic(Topic value){topic=value;}
    public String getMessage(){return message;}
    public void setMessage(String value){message=value==null?null:value.trim();}
}
