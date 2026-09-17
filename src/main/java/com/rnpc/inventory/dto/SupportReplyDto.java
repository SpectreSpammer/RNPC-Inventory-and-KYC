package com.rnpc.inventory.dto;
import com.rnpc.inventory.entity.SupportTicket.Status;
import jakarta.validation.constraints.*;
public class SupportReplyDto {
    @NotNull private Status status;
    @Size(max=5000,message="Use 5,000 characters or fewer.") private String message;
    @NotNull private Long version;
    @AssertTrue(message="Write a reply before marking a ticket Answered.")
    public boolean isAnswerValid(){return status!=Status.ANSWERED || (message!=null&&!message.isBlank());}
    public Status getStatus(){return status;}
    public void setStatus(Status value){status=value;}
    public String getMessage(){return message;}
    public void setMessage(String value){message=value==null?null:value.trim();}
    public Long getVersion(){return version;}
    public void setVersion(Long value){version=value;}
}
