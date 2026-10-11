package com.chatbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@Service
public class OpenAiService {

    private final RestClient restClient;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;

    public OpenAiService(RestClient restClient) {
        this.restClient = restClient;
    }

    public String generateReply(String userMessage) {

        if (userMessage == null || userMessage.isBlank()) {
            return "Please enter a message.";
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,

                "instructions",
                "You are a friendly customer support chatbot. "
                        + "Give short, clear, polite and helpful answers. "
                        + "If you do not know specific company information, "
                        + "say that the customer should contact a staff member.",

                "input", userMessage
        );

        try {

            JsonNode response = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            return extractOutputText(response);

        } catch (RestClientResponseException exception) {

            System.out.println("--------------------------------");
            System.out.println("OpenAI API Request Failed");
            System.out.println("Status Code: "
                    + exception.getStatusCode());
            System.out.println("Error Response: "
                    + exception.getResponseBodyAsString());
            System.out.println("--------------------------------");

            return getFriendlyErrorMessage(
                    exception.getStatusCode().value()
            );

        } catch (Exception exception) {

            System.out.println("--------------------------------");
            System.out.println("Unexpected OpenAI Error");
            exception.printStackTrace();
            System.out.println("--------------------------------");

            return "Sorry, something went wrong while contacting the AI service.";
        }
    }

    private String extractOutputText(JsonNode response) {

        if (response == null) {
            return "No response was received from the AI.";
        }

        JsonNode outputArray = response.path("output");

        if (!outputArray.isArray()
                || outputArray.isEmpty()) {

            System.out.println(
                    "OpenAI response did not contain an output array:"
                            + response
            );

            return "The AI did not return a valid answer.";
        }

        for (JsonNode outputItem : outputArray) {

            JsonNode contentArray =
                    outputItem.path("content");

            if (!contentArray.isArray()) {
                continue;
            }

            for (JsonNode contentItem : contentArray) {

                String contentType =
                        contentItem.path("type").asText();

                if ("output_text".equals(contentType)) {

                    String text =
                            contentItem.path("text").asText();

                    if (text != null
                            && !text.isBlank()) {

                        return text;
                    }
                }
            }
        }

        System.out.println(
                "Could not find output_text in OpenAI response: "
                        + response
        );

        return "The AI did not return a text answer.";
    }

    private String getFriendlyErrorMessage(
            int statusCode
    ) {

        return switch (statusCode) {

            case 400 ->
                    "The AI request was invalid. Please check the request settings.";

            case 401 ->
                    "The OpenAI API key is invalid or missing.";

            case 403 ->
                    "You do not have permission to use this OpenAI resource.";

            case 404 ->
                    "The selected OpenAI model or API endpoint was not found.";

            case 429 ->
                    "The OpenAI usage limit was reached. Please check API billing or quota.";

            case 500, 502, 503, 504 ->
                    "The OpenAI service is temporarily unavailable. Please try again.";

            default ->
                    "Sorry, I could not generate a reply right now.";
        };
    }
}