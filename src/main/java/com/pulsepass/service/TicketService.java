package com.pulsepass.service;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;

import java.util.List;

// Contrato: PRD Servicios §21
public interface TicketService {

    TicketResponse purchase(PurchaseTicketRequest request);

    TicketResponse findByCode(String ticketCode);

    List<TicketResponse> findByUserEmail(String email);

    List<TicketResponse> findPaidTicketsByEvent(String eventCode);

    TicketResponse cancel(String ticketCode);

    TicketResponse markAsUsed(String ticketCode);
}
