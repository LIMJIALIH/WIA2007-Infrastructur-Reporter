package com.example.infrastructureproject;

import java.util.ArrayList;
import java.util.List;

public class TicketManager {
    private static TicketManager instance;
    private List<Ticket> allTickets;

    private TicketManager() {
        allTickets = new ArrayList<>();
        // Mock data removed - now using real Supabase data
        // Tickets are now loaded from Supabase via TicketRepository
    }

    public static synchronized TicketManager getInstance() {
        if (instance == null) {
            instance = new TicketManager();
        }
        return instance;
    }

    public List<Ticket> getAllTickets() {
        return new ArrayList<>(allTickets);
    }

    public void addTicket(Ticket ticket) {
        // Generate ID if not set
        if (ticket.getId() == null || ticket.getId().isEmpty()) {
            ticket.setId("TKT" + String.format("%03d", allTickets.size() + 1));
        }

        // Ensure ticket has PENDING status
        if (ticket.getStatus() == null) {
            ticket.setStatus(Ticket.TicketStatus.PENDING);
        }

        allTickets.add(ticket);
    }

    public void updateTicket(Ticket updatedTicket) {
        for (int i = 0; i < allTickets.size(); i++) {
            if (allTickets.get(i).getId().equals(updatedTicket.getId())) {
                allTickets.set(i, updatedTicket);
                break;
            }
        }
    }

    public Ticket getTicketById(String ticketId) {
        for (Ticket ticket : allTickets) {
            if (ticket.getId().equals(ticketId)) {
                return ticket;
            }
        }
        return null;
    }

    // Get tickets by status for filtering
    public List<Ticket> getTicketsByStatus(Ticket.TicketStatus status) {
        List<Ticket> filtered = new ArrayList<>();
        for (Ticket ticket : allTickets) {
            if (ticket.getStatus() == status) {
                filtered.add(ticket);
            }
        }
        return filtered;
    }

    // Get tickets count by status
    public int getTicketCountByStatus(Ticket.TicketStatus status) {
        int count = 0;
        for (Ticket ticket : allTickets) {
            if (ticket.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    // Get total tickets count
    public int getTotalTicketCount() {
        return allTickets.size();
    }

    // Delete ticket by ID
    public void deleteTicket(String ticketId) {
        for (int i = 0; i < allTickets.size(); i++) {
            if (allTickets.get(i).getId().equals(ticketId)) {
                allTickets.remove(i);
                break;
            }
        }
    }
}
