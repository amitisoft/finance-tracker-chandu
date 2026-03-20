package com.hackathon.finance.dto.assistant;

public record AssistantResponse(
        String reply,
        String intent,
        boolean actionTaken,
        String spokenReply
) {
}
