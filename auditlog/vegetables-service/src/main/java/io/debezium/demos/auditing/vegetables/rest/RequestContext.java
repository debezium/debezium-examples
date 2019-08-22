package io.debezium.demos.auditing.vegetables.rest;

import java.time.ZonedDateTime;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestContext {

    public String userName;
    public ZonedDateTime date;
}
