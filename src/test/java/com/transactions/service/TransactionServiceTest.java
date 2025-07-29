package com.transactions.service;

import com.transactions.model.Transaction;
import com.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.transactions.model.Transaction.TransactionStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repo;

    @InjectMocks
    private TransactionService service;

    // Helper method to simplify Transaction creation
    private Transaction createTransaction(Long id, String name, double value, Transaction.TransactionStatus status, ZonedDateTime date) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setName(name);
        t.setValue(value);
        t.setStatus(status);
        t.setDate(date);
        return t;
    }

    @Test
    void save_convertsZoneToUTC_andPersists() {
        ZonedDateTime original = ZonedDateTime.of(2025, 7, 25, 20, 8, 56, 0, ZoneId.of("-05:00"));
        Transaction t = new Transaction();
        t.setDate(original);
        t.setStatus(PENDING);

        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = service.save(t);

        assertEquals(ZoneOffset.UTC, result.getDate().getOffset());
        verify(repo).save(any());
    }

    @Test
    void update_changesFieldsCorrectly() {
        Transaction existing = createTransaction(1L, "Old", 100.0, PENDING, ZonedDateTime.now().minusDays(2));
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Transaction updated = new Transaction();
        updated.setName("Updated");
        updated.setValue(150.0);
        updated.setStatus(PAID);
        updated.setDate(ZonedDateTime.now());

        Transaction result = service.update(1L, updated);

        assertEquals("Updated", result.getName());
        assertEquals(150.0, result.getValue());
        assertEquals(PAID, result.getStatus());
    }

    @Test
    void update_throwsIfTransactionIsPaid() {
        Transaction paid = createTransaction(1L, "Paid Txn", 100.0, PAID, ZonedDateTime.now());
        when(repo.findById(1L)).thenReturn(Optional.of(paid));

        Transaction updated = new Transaction();
        updated.setName("Attempted Update");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.update(1L, updated);
        });

        assertTrue(ex.getMessage().contains("Can't update paid"));
    }

    @Test
    void delete_invokesRepository() {
        Transaction t = createTransaction(1L, "To Delete", 50.0, PENDING, ZonedDateTime.now());
        when(repo.findById(1L)).thenReturn(Optional.of(t));

        service.delete(1L);

        verify(repo).findById(1L);
        verify(repo).deleteById(1L);
    }

    @Test
    void delete_throwsIfPaid() {
        Transaction t = createTransaction(1L, "Paid Txn", 50.0, PAID, ZonedDateTime.now());
        when(repo.findById(1L)).thenReturn(Optional.of(t));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.delete(1L);
        });

        assertTrue(ex.getMessage().contains("Can't delete a paid"));
        verify(repo, never()).deleteById(any());
    }

    @Test
    void makePayment_marksTransactionsAsPaidInOrder() {
        Transaction t1 = createTransaction(1L, "T1", 50.0, PENDING, ZonedDateTime.now().minusDays(3));
        Transaction t2 = createTransaction(2L, "T2", 75.0, PENDING, ZonedDateTime.now().minusDays(2));
        Transaction t3 = createTransaction(3L, "T3", 100.0, PENDING, ZonedDateTime.now().minusDays(1));

        when(repo.findAll()).thenReturn(List.of(t1, t2, t3));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.makePayment(130.0);

        assertEquals(PAID, t1.getStatus());
        assertEquals(PAID, t2.getStatus());
        assertEquals(PENDING, t3.getStatus());

        verify(repo, times(2)).save(any());
    }
}
