package org.xu.newjob.entity;

import lombok.Data;

import java.util.Date;

@Data
public class LoginTicket {
    private int id;
    private int userId;
    private String ticket;
    private int status;
    private Date expired;
}
