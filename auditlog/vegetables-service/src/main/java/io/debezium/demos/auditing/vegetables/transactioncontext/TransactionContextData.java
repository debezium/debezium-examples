package io.debezium.demos.auditing.vegetables.transactioncontext;

import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
