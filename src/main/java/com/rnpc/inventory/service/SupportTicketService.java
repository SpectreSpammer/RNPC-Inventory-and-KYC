package com.rnpc.inventory.service;
import com.rnpc.inventory.entity.*;
import com.rnpc.inventory.dto.*;
import com.rnpc.inventory.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.*;
import java.io.IOException;

@Service
public class SupportTicketService {
    private final SupportTicketRepository tickets;
    private final SupportReplyRepository replies;
    private final SupportAttachmentRepository attachments;
    private final UserService users;
    private final NotificationService notifications;
    public SupportTicketService(SupportTicketRepository tickets,SupportReplyRepository replies,SupportAttachmentRepository attachments,UserService users,NotificationService notifications){
        this.tickets=tickets;this.replies=replies;this.attachments=attachments;this.users=users;this.notifications=notifications;
    }
    public List<SupportTicket> list(String username,boolean admin){
        return admin?tickets.findAllByOrderByIdDesc():tickets.findByUser_UsernameOrderByIdDesc(username);
    }
    public SupportTicket accessible(Long id,String username,boolean admin){
        return (admin?tickets.findById(id):tickets.findByIdAndUser_Username(id,username))
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    public List<SupportReply> replies(Long id,String username,boolean admin){
        accessible(id,username,admin);return replies.findByTicket_IdOrderByIdAsc(id);
    }
    public List<SupportAttachmentRepository.Summary> attachments(Long id,String username,boolean admin){
        accessible(id,username,admin);return attachments.findByTicket_IdOrderByIdAsc(id);
    }
    public SupportAttachment attachment(Long ticketId,Long id,String username,boolean admin){
        accessible(ticketId,username,admin);return attachments.findByIdAndTicket_Id(id,ticketId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    @Transactional
    public SupportTicket create(String username,SupportTicketDto dto,List<MultipartFile> files){
        List<MultipartFile> uploads=files==null?List.of():files.stream().filter(f->!f.isEmpty()).toList();
        if(uploads.size()>5)throw new IllegalArgumentException("Attach up to five files.");
        List<SupportAttachment> ready=new ArrayList<>();
        for(MultipartFile file:uploads){
            if(file.getSize()>10L*1024*1024)throw new IllegalArgumentException("Each file must be 10 MB or smaller.");
            byte[] data;
            try{data=file.getBytes();}catch(IOException e){throw new IllegalArgumentException("An attachment could not be read. Please select it again.");}
            String type=detectType(data);
            if(type==null)throw new IllegalArgumentException("Only PNG, JPG and PDF files are accepted.");
            String name=Optional.ofNullable(file.getOriginalFilename()).orElse("attachment").replace('\\','/');
            name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","");
            if(name.isBlank())name="attachment";
            if(name.length()>200)name=name.substring(name.length()-200);
            SupportAttachment a=new SupportAttachment();a.setFilename(name);a.setContentType(type);a.setSize(data.length);a.setData(data);ready.add(a);
        }
        User user=users.findByUsername(username).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        SupportTicket ticket=new SupportTicket();ticket.setUser(user);ticket.setTopic(dto.getTopic());ticket.setMessage(dto.getMessage());
        ticket.setCreatedAt(LocalDateTime.now());ticket.setUpdatedAt(ticket.getCreatedAt());tickets.save(ticket);
        for(SupportAttachment a:ready){a.setTicket(ticket);attachments.save(a);}
        notifications.notifyAdmin("New support ticket "+ticket.getReference()+": "+ticket.getTopic().getLabel(),Notification.EntityType.SUPPORT_TICKET,ticket.getId());
        return ticket;
    }
    @Transactional
    public void respond(Long id,String username,SupportReplyDto dto){
        User admin=users.findByUsername(username).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if(admin.getRole()!=User.Role.ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        SupportTicket ticket=accessible(id,username,true);
        if(!Objects.equals(ticket.getVersion(),dto.getVersion()))throw new IllegalArgumentException("This ticket changed. Reload it before updating.");
        if(dto.getStatus()==SupportTicket.Status.ANSWERED&&(dto.getMessage()==null||dto.getMessage().isBlank()))throw new IllegalArgumentException("Write a reply before marking Answered.");
        boolean hasReply=dto.getMessage()!=null&&!dto.getMessage().isBlank();
        if(ticket.getStatus()==dto.getStatus()&&!hasReply)return;
        if(hasReply){SupportReply reply=new SupportReply();reply.setTicket(ticket);reply.setAuthor(admin);reply.setMessage(dto.getMessage());reply.setCreatedAt(LocalDateTime.now());replies.save(reply);}
        ticket.setStatus(dto.getStatus());ticket.setUpdatedAt(LocalDateTime.now());tickets.save(ticket);
        notifications.notifyCustomer(ticket.getUser(),"Support ticket "+ticket.getReference()+" updated: "+ticket.getStatus().getLabel(),Notification.EntityType.SUPPORT_TICKET,ticket.getId());
    }
    private static String detectType(byte[] b){
        if(b.length>=8&&b[0]==(byte)137&&b[1]==80&&b[2]==78&&b[3]==71&&b[4]==13&&b[5]==10&&b[6]==26&&b[7]==10)return "image/png";
        if(b.length>=3&&b[0]==(byte)255&&b[1]==(byte)216&&b[2]==(byte)255)return "image/jpeg";
        if(b.length>=5&&b[0]==37&&b[1]==80&&b[2]==68&&b[3]==70&&b[4]==45)return "application/pdf";
        return null;
    }
}
