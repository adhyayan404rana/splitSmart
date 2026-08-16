package com.splitsmart.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitsmart.settlement.MarkSettledRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:paymentdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class PaymentIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void testGetPaymentIntents() throws Exception {
        UUID groupId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/payments/groups/" + groupId + "/intents"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].fromUserName").exists())
                .andExpect(jsonPath("$[0].upiIntentString").exists())
                .andExpect(jsonPath("$[0].qrCodeBase64").exists());
    }

    @Test
    @WithMockUser
    void testGetQrCodeImage() throws Exception {
        String upiIntent = "upi://pay?pa=sarah@okaxis&pn=Sarah&am=1500.00&cu=INR&tn=SplitSmart";

        mockMvc.perform(get("/api/v1/payments/qr").param("upiIntent", upiIntent))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    @WithMockUser
    void testMarkAsSettled() throws Exception {
        UUID groupId = UUID.randomUUID();
        UUID debtorId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID creditorId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        MarkSettledRequest request = MarkSettledRequest.builder()
                .groupId(groupId)
                .debtorId(debtorId)
                .creditorId(creditorId)
                .amountCents(200000L)
                .currency("INR")
                .note("Goa Trip Settlement")
                .transactionRef("UPI-TEST-12345")
                .build();

        mockMvc.perform(post("/api/v1/payments/settle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
