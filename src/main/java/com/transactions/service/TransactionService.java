package com.transactions.service;

import com.transactions.model.Payment;
import com.transactions.model.Transaction;
import com.transactions.repository.PaymentRepository;
import com.transactions.repository.TransactionRepository;
import com.transactions.spec.TransactionSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository repo;
    private final PaymentRepository paymentRepo;

    public TransactionService(TransactionRepository repo, PaymentRepository paymentRepo) {
        this.repo = repo;
        this.paymentRepo = paymentRepo;
    }

    public List<Transaction> getAll() {
        return repo.findAll();
    }

    public Page<Transaction> searchWithFilters(String name,
                                              LocalDate from,
                                              LocalDate to,
                                              String status,
                                              Pageable pageable) {
        Specification<Transaction> spec = Specification.unrestricted();
        if (name != null) {
            spec = spec.and(TransactionSpecification.hasName(name));
        }
        if (from != null && to != null) {
            spec = spec.and(TransactionSpecification.dateBetween(from, to));
        }
        if (status != null) {
            spec = spec.and(TransactionSpecification.hasStatus(status));
        }

        return repo.findAll(spec, pageable);
    }

    public Transaction save(Transaction t) {
        ZonedDateTime date = t.getDate();
        if (date != null) {
            t.setDate(date.withZoneSameInstant(ZoneOffset.UTC));
        }

        List<Payment> payments = paymentRepo.findAll().stream().filter(payment -> payment.getStatus() == Payment.PaymentStatus.PENDING).toList();

        Double transactionValue = t.getValue();

        for (Payment p : payments) {
            double paymentValue = p.getValue();

            if (transactionValue <= 0) {
                break; // We've paid off the transaction
            }

            if (paymentValue <= transactionValue) {
                // Use the entire payment
                transactionValue -= paymentValue;
                p.setStatus(Payment.PaymentStatus.COMPLETED);
            } else {
                // Payment is larger than what's left to pay — partially use it
                double remainingPayment = paymentValue - transactionValue;
                p.setValue(remainingPayment);
                // Optionally update status or leave as partially paid
                t.setStatus(Transaction.TransactionStatus.PAID);
            }

            paymentRepo.save(p); // Persist changes
        }

        t.setValue(transactionValue);
        if (transactionValue == 0) {
            t.setStatus(Transaction.TransactionStatus.PAID);
        }

        return repo.save(t);
    }

    @Transactional
    public void makePayment(Double paymentValue) {
        List<Transaction> pending = repo.findAll().stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.PENDING)
                .sorted((a,b) -> a.getDate().compareTo(b.getDate()))
                .toList();

        if (pending.isEmpty()) {
            Payment newPayment = new Payment();
            newPayment.setValue(paymentValue);

            newPayment.setStatus(paymentValue == 0 ? Payment.PaymentStatus.COMPLETED : Payment.PaymentStatus.PENDING );
            paymentRepo.save(newPayment);
        }

        for (Transaction t : pending) {
            if (paymentValue >= t.getValue()) {
                paymentValue -= t.getValue();
                t.setStatus(Transaction.TransactionStatus.PAID);

                repo.save(t);
            } else {
                Payment newPayment = new Payment();
                newPayment.setValue(paymentValue);


                newPayment.setStatus(paymentValue == 0 ? Payment.PaymentStatus.COMPLETED : Payment.PaymentStatus.PENDING );
                paymentRepo.save(newPayment);
                break;
            }
        }
    }

    @Transactional
    public Transaction update(Long id, Transaction updated){
        Transaction current = repo.findById(id)
                .orElseThrow(() -> new RuntimeException(("Transaction not found")));
        if (current.getStatus() == Transaction.TransactionStatus.PAID) {
            throw new RuntimeException("Can't update paid trasnaction");
        }
        ZonedDateTime date = updated.getDate();
        if (date != null) {
            updated.setDate(date.withZoneSameInstant(ZoneOffset.UTC));
        }
        current.setName(updated.getName());
        current.setDate(updated.getDate());
        current.setValue(updated.getValue());
        current.setStatus(updated.getStatus());
        return repo.save(current);
    }

    @Transactional
    public void delete(Long id){
        Transaction t = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (t.getStatus() == Transaction.TransactionStatus.PAID) {
            throw new RuntimeException("Can't delete a paid transaction");
        }
        repo.deleteById(id);
    }
}


