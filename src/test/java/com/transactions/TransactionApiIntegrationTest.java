package com.transactions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transactions.model.Transaction;
import com.transactions.model.Transaction.TransactionStatus;
import com.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TransactionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository repo;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDb() {
        repo.deleteAll();
    }

    private Transaction createTransaction(String name, double value, Transaction.TransactionStatus status, ZonedDateTime date) {
        Transaction t = new Transaction();
        t.setName(name);
        t.setValue(value);
        t.setStatus(status);
        t.setDate(date);
        return repo.save(t); // persists and returns with ID
    }

    @Test
    void postTransaction_savesWithUtcDate() throws Exception {
        Transaction t = createTransaction("Groceries", 523467.35, Transaction.TransactionStatus.PENDING,ZonedDateTime.parse("2025-07-29T18:00:00-05:00[America/Chicago]") );

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        Transaction saved = repo.findAll().getFirst();
        assertThat(saved.getDate().getOffset()).isEqualTo(ZonedDateTime.now().withZoneSameInstant(java.time.ZoneOffset.UTC).getOffset());
    }

    @Test
    void getTransactions_returnsFilteredResults() throws Exception {
        createTransaction("Alice", 100.0, Transaction.TransactionStatus.PENDING, ZonedDateTime.now().minusDays(1));
        createTransaction("Bob", 200.0, Transaction.TransactionStatus.PAID, ZonedDateTime.now().minusDays(2));

        mockMvc.perform(get("/api/transactions")
                        .param("name", "Alice")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alice"));
    }

    @Test
    void updateTransaction_modifiesFields() throws Exception {
        ZonedDateTime date = ZonedDateTime.now().minusDays(2);
        Transaction transaction = new Transaction();
        transaction.setName("Original");
        transaction.setValue(50.0);
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        transaction.setDate(date);

        // Create transaction first
        String response = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Transaction created = objectMapper.readValue(response, Transaction.class);

        // Prepare update
        Transaction updated = new Transaction();
        updated.setName("Updated");
        updated.setValue(75.0);
        updated.setStatus(Transaction.TransactionStatus.PENDING);
        updated.setDate(ZonedDateTime.now());

        // Perform PUT
        mockMvc.perform(put("/api/transactions/{id}", created.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"))
                .andExpect(jsonPath("$.value").value(75.0));
    }

    @Test
    void deleteTransaction_removesIt() throws Exception {
        Transaction t = createTransaction("ToDelete", 10.0, Transaction.TransactionStatus.PENDING, ZonedDateTime.now());

        mockMvc.perform(delete("/api/transactions/{id}", t.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions")
                        .param("name", "ToDelete")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void paymentEndpoint_marksTransactionsAsPaidCorrectly() throws Exception {
        // Oldest first (will be paid first)
        createTransaction("T1", 50.0, Transaction.TransactionStatus.PENDING, ZonedDateTime.now().minusDays(3));
        createTransaction("T2", 75.0, Transaction.TransactionStatus.PENDING, ZonedDateTime.now().minusDays(2));
        createTransaction("T3", 100.0, Transaction.TransactionStatus.PENDING, ZonedDateTime.now().minusDays(1));

        // Total = 225. We'll pay 130, enough for T1 and T2 (partial T3 is ignored)

        mockMvc.perform(post("/api/transactions/payment")
                        .param("paymentValue", "130"))
                .andExpect(status().isOk());

        // Fetch all transactions and verify statuses
        mockMvc.perform(get("/api/transactions")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.name == 'T1')].status").value("PAID"))
                .andExpect(jsonPath("$.content[?(@.name == 'T2')].status").value("PAID"))
                .andExpect(jsonPath("$.content[?(@.name == 'T3')].status").value("PENDING"));
    }
}
