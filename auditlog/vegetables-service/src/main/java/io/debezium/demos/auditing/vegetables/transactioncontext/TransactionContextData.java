package io.debezium.demos.auditing.vegetables.transactioncontext;

import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="transaction_context_data")
public class TransactionContextData {

    @Id
    @Column(name="transaction_id")
    public long transactionId;

    @Column(name="user_name")
    public String userName;

    @Column(name="client_date")
    public ZonedDateTime clientDate;

    @Column(name="useCase")
    public String useCase;
}
