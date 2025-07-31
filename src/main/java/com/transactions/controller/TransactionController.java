package com.transactions.controller;

import com.transactions.model.Transaction;
import com.transactions.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<Transaction>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) @SortDefault.SortDefaults({
                    @SortDefault(sort = "date", direction = Sort.Direction.DESC)
            }) Pageable pageable
    ) {
        Page<Transaction> list = service.searchWithFilters(name, from, to, status, pageable);
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<Transaction> create(@RequestBody Transaction t) {
        Transaction newTransaction = service.save(t);
        return ResponseEntity.status(HttpStatus.CREATED).body(newTransaction);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(
            @PathVariable Long id,
            @RequestBody Transaction updatedT
    ) {
        Transaction actual = service.update(id, updatedT);
        return ResponseEntity.ok(actual);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record PaymentValueBody(Double paymentValue) {}

    @PostMapping("/pay")
    public ResponseEntity<Void> pay(@RequestBody PaymentValueBody paymentValue) {
        service.makePayment(paymentValue.paymentValue);
        return ResponseEntity.ok().build();
    }
}
