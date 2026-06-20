package com.logistics.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BotPreviewResponse {
    private String intent;
    private String reply;
    private Boolean suggestCreateTicket;
    private Boolean suggestViewTickets;

    public BotPreviewResponse(String intent, String reply, boolean suggestCreateTicket, boolean suggestViewTickets) {
        this.intent = intent;
        this.reply = reply;
        this.suggestCreateTicket = suggestCreateTicket;
        this.suggestViewTickets = suggestViewTickets;
    }
}
