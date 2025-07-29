package com.transactions.spec;

import com.transactions.model.Transaction;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;

public class TransactionSpecification {

    public static Specification<Transaction> hasName(String name) {
        return (root, query, cb) -> name == null
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Transaction> hasStatus(String status) {
        return (root, query, cb) -> status == null
                ? null
                : cb.equal(root.get("status"), Transaction.TransactionStatus.valueOf(status));
    }

    public static Specification<Transaction> dateBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> (from != null && to != null)
                ? cb.between(root.get("date"), from, to)
                : null;
    }
}
