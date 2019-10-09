package io.debezium.demos.auditing.admin;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AuditData {

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    @JsonProperty("client_date")
    private Date clientDate;
    @JsonProperty("usecase")
    private String useCase;
    @JsonProperty("user_name")
    private String username;

    public Date getClientDate() {
        return clientDate;
    }

    public void setClientDate(Date clientDate) {
        this.clientDate = clientDate;
    }

    public String getUseCase() {
        return useCase;
    }

    public void setUseCase(String useCase) {
        this.useCase = useCase;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
