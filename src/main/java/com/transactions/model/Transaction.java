package com.transactions.model;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity // Indicates this class maps to a db table
@Table(name="transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB assigns primary key
    private Long id;

    private String name;
    private ZonedDateTime date;

    @Column(name = "transaction_value")
    private Double value;

    @Column(columnDefinition = "VARCHAR(255) CHECK (status IN ('PENDING', 'PAID', 'REJECTED'))")
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    public enum TransactionStatus {
        PENDING,
        PAID,
        REJECTED,
    }

    public Transaction() {}

    public void setId(Long id) { this.id = id;}
    public Long getId() {return  id;}
    public String getName() {return name;}
    public void setName(String name) { this.name = name;}
    public ZonedDateTime getDate() {return date;}
    public void setDate(ZonedDateTime date) { this.date = date;}
    public Double getValue() {return value;}
    public void setValue(Double value) {this.value = value;}
    public TransactionStatus getStatus() {return status;}
    public void setStatus(TransactionStatus status) {this.status = status;}
}
