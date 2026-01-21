package com.adib.fintrack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import okhttp3.*;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String fromEmail;

    @Value("${brevo.sender.name:FinTrack}")
    private String fromName;

    public void sendEmail(String to, String subject, String body) {
        try {
            OkHttpClient client = new OkHttpClient();

            Map<String, Object> emailData = new HashMap<>();

            emailData.put("sender", Map.of(
                    "email", fromEmail,
                    "name", fromName
            ));

            emailData.put("to", List.of(Map.of("email", to)));

            emailData.put("subject", subject);

            emailData.put("htmlContent", body);

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(emailData);

            RequestBody requestBody = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(BREVO_API_URL)
                    .addHeader("api-key", brevoApiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("Brevo API error: {}", errorBody);
                throw new RuntimeException("Email sending failed: " + errorBody);
            }

            log.info("Email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Email sending failed: {}", e.getMessage(), e);
            throw new RuntimeException("Mail server connection failed", e);
        }
    }
}