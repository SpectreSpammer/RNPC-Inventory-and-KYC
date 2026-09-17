package com.rnpc.inventory.controller;
import com.rnpc.inventory.dto.*;
import com.rnpc.inventory.entity.*;
import com.rnpc.inventory.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.nio.charset.StandardCharsets;

@Controller
public class SupportController {
    private final SupportTicketService support;
    private final NotificationService notifications;
    public SupportController(SupportTicketService support,NotificationService notifications){this.support=support;this.notifications=notifications;}
    private boolean signedIn(Authentication a){return a!=null&&a.isAuthenticated()&&!(a instanceof AnonymousAuthenticationToken);}
    private boolean admin(Authentication a){return signedIn(a)&&a.getAuthorities().stream().anyMatch(v->v.getAuthority().equals("ROLE_ADMIN"));}
    private void requireSignedIn(Authentication a){if(!signedIn(a))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
    private void shell(Authentication a,Model m){
        m.addAttribute("currentUsername",a.getName());m.addAttribute("currentRole",admin(a)?"Admin":"Customer");
        m.addAttribute("unreadNotifications",admin(a)?notifications.getUnreadCountForAdmin():notifications.getUnreadCountForUser(a.getName()));
        m.addAttribute("recentNotifications",(admin(a)?notifications.getForAdmin():notifications.getForUser(a.getName())).stream().limit(15).toList());
        m.addAttribute("topics",SupportTicket.Topic.values());m.addAttribute("statuses",SupportTicket.Status.values());
    }
    private void listing(Authentication a,Model m,boolean all){
        shell(a,m);List<SupportTicket> tickets=support.list(a.getName(),admin(a));
        m.addAttribute("tickets",all?tickets:tickets.stream().limit(5).toList());m.addAttribute("ticketCount",tickets.size());m.addAttribute("showAll",all);
    }
    @GetMapping("/support")
    public String index(Authentication a,@RequestParam(defaultValue="false") boolean all,Model m){
        if(!signedIn(a))return "redirect:/login";
        if(admin(a))return "redirect:/admin/support";
        listing(a,m,all);m.addAttribute("ticketForm",new SupportTicketDto());return "support/index";
    }
    @GetMapping("/admin/support")
    public String adminIndex(Authentication a,Model m){
        if(!admin(a))return "redirect:/";
        listing(a,m,true);return "support/admin";
    }
    @PostMapping("/support")
    public String submit(Authentication a,@Valid @ModelAttribute("ticketForm") SupportTicketDto dto,BindingResult errors,
                         @RequestParam(value="files",required=false) List<MultipartFile> files,Model m,RedirectAttributes flash){
        requireSignedIn(a);if(admin(a))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(!errors.hasErrors()){
            try{SupportTicket ticket=support.create(a.getName(),dto,files);flash.addFlashAttribute("success","Ticket "+ticket.getReference()+" submitted. You can view it below.");return "redirect:/support#recentTickets";}
            catch(IllegalArgumentException e){errors.reject("upload",e.getMessage());}
        }
        listing(a,m,false);return "support/index";
    }
    private void detail(Long id,Authentication a,Model m){
        requireSignedIn(a);shell(a,m);
        SupportTicket ticket=support.accessible(id,a.getName(),admin(a));
        m.addAttribute("ticket",ticket);m.addAttribute("replies",support.replies(id,a.getName(),admin(a)));m.addAttribute("attachments",support.attachments(id,a.getName(),admin(a)));
        if(!m.containsAttribute("replyForm")){SupportReplyDto dto=new SupportReplyDto();dto.setStatus(ticket.getStatus());dto.setVersion(ticket.getVersion());m.addAttribute("replyForm",dto);}
    }
    @GetMapping("/support/tickets/{id}")
    public String view(@PathVariable Long id,Authentication a,Model m){detail(id,a,m);return "support/detail";}
    @GetMapping("/support/tickets/{id}/modal")
    public String modal(@PathVariable Long id,Authentication a,Model m){detail(id,a,m);return "support/detail :: ticketDetail";}
    @PostMapping("/support/tickets/{id}/reply")
    public String reply(@PathVariable Long id,Authentication a,@Valid @ModelAttribute("replyForm") SupportReplyDto dto,BindingResult errors,Model m,RedirectAttributes flash){
        if(!admin(a))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if(!errors.hasErrors()){
            try{support.respond(id,a.getName(),dto);flash.addFlashAttribute("success","Ticket updated and the customer notified.");return "redirect:/support/tickets/"+id;}
            catch(IllegalArgumentException e){errors.reject("conflict",e.getMessage());}
            catch(org.springframework.dao.OptimisticLockingFailureException e){errors.reject("conflict","This ticket changed. Reload it before updating.");}
        }
        detail(id,a,m);return "support/detail";
    }
    @GetMapping("/support/tickets/{ticketId}/attachments/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long ticketId,@PathVariable Long id,Authentication a){
        requireSignedIn(a);SupportAttachment file=support.attachment(ticketId,id,a.getName(),admin(a));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.getFilename(),StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(file.getContentType())).body(file.getData());
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(RedirectAttributes flash){flash.addFlashAttribute("uploadError","Upload up to five files, 10 MB each. Please select smaller files and submit again.");return "redirect:/support";}
}
