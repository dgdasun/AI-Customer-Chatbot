package com.chatbot.service;

import com.chatbot.dto.ChatResponse;
import com.chatbot.entity.Message;
import com.chatbot.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final OpenAiService openAiService;

    public ChatService(
            MessageRepository messageRepository,
            OpenAiService openAiService
    ) {
        this.messageRepository = messageRepository;
        this.openAiService = openAiService;
    }

    public ChatResponse chat(String userMessage) {

        if (userMessage == null
                || userMessage.isBlank()) {

            return new ChatResponse(
                    "Please enter a message."
            );
        }

        String aiReply =
                openAiService.generateReply(userMessage);

        Message message = new Message();

        message.setQuestion(userMessage);
        message.setAnswer(aiReply);
        message.setCreatedAt(LocalDateTime.now());

        messageRepository.save(message);

        return new ChatResponse(aiReply);
    }
}