package com.transactions.model;

import jakarta.persistence.*;

import java.time.ZonedDateTime;

@Entity // Indicates this class maps to a db table
@Table(name="payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB assigns primary key
    private Long id;

    @Column(name = "payment_value")
    private Double value;

    @Column(columnDefinition = "VARCHAR(255) CHECK (status IN ('PENDING', 'COMPLETED'))")
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    public enum PaymentStatus {
        PENDING,
        COMPLETED,
    }

    public Payment() {}

    public void setId(Long id) { this.id = id;}
    public Long getId() {return  id;}
    public Double getValue() {return value;}
    public void setValue(Double value) {this.value = value;}
    public Payment.PaymentStatus getStatus() {return status;}
    public void setStatus(Payment.PaymentStatus status) {this.status = status;}
}
