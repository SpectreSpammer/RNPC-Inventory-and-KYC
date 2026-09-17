package com.rnpc.inventory.entity;
import jakarta.persistence.*;
@Entity @Table(name="rnpc_support_attachments")
public class SupportAttachment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="ticket_id",nullable=false) private SupportTicket ticket;
    @Column(nullable=false) private String filename;
    @Column(nullable=false) private String contentType;
    @Column(nullable=false) private long size;
    @Lob @Basic(fetch=FetchType.LAZY) @Column(nullable=false,columnDefinition="LONGBLOB") private byte[] data;
    public Long getId(){return id;}
    public void setId(Long value){id=value;}
    public SupportTicket getTicket(){return ticket;}
    public void setTicket(SupportTicket value){ticket=value;}
    public String getFilename(){return filename;}
    public void setFilename(String value){filename=value;}
    public String getContentType(){return contentType;}
    public void setContentType(String value){contentType=value;}
    public long getSize(){return size;}
    public void setSize(long value){size=value;}
    public byte[] getData(){return data;}
    public void setData(byte[] value){data=value;}
}
