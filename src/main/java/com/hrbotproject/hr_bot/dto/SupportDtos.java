package com.hrbotproject.hr_bot.dto;

import com.hrbotproject.hr_bot.model.SupportTicket;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class SupportDtos {

    /** One line of the bot chat, as the employee saw it. */
    public record ChatLine(
            @NotNull @Pattern(regexp = "user|bot") String from,
            @NotBlank @Size(max = 2000) String text
    ) {}

    /** Employee: "Talk to a person in HR". */
    public record OpenTicketRequest(
            SupportTicket.Source source,
            @NotBlank @Size(max = 2000, message = "please keep your message under 2000 characters") String message,
            @Size(max = 20) List<@Valid ChatLine> chat
    ) {}

    public record ReplyRequest(
            @NotBlank @Size(max = 2000, message = "please keep your message under 2000 characters") String message
    ) {}

    /** Small numbers for the menu badges. */
    public record SupportCounts(long waitingForHr, long unread) {}
}