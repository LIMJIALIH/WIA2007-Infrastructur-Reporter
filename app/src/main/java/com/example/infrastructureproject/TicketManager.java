package com.example.infrastructureproject;

import java.util.ArrayList;
import java.util.List;

public class TicketManager {
    private static TicketManager instance;
    private List<Ticket> allTickets;

    private TicketManager() {
        allTickets = new ArrayList<>();
        // Initialize with mock data
        allTickets.addAll(MockDataGenerator.generateMockTickets());
        
        // Add 3 more tickets to get 8 total (4 pending, 2 rejected, 2 accepted)
        allTickets.add(new Ticket(
                "TKT006",
                "Road",
                "Medium",
                "Jalan Ampang, Kuala Lumpur",
                "Street light not working for the past week. Dark at night causing safety concerns.",
                "2025-11-11 06:30 PM",
                "streetlight_broken"
        ));
        
        allTickets.add(new Ticket(
                "TKT007",
                "Utilities",
                "High",
                "Taman Tun Dr Ismail",
                "Manhole cover missing on main road. Dangerous for vehicles and pedestrians.",
                "2025-11-10 03:15 PM",
                "manhole_missing"
        ));
        
        allTickets.add(new Ticket(
                "TKT008",
                "Environment",
                "Low",
                "KLCC Park",
                "Overflowing trash bin near jogging track. Needs immediate attention.",
                "2025-11-09 07:45 AM",
                "trash_overflow"
        ));
        
        // Set statuses to match User_UI: 4 PENDING, 2 REJECTED, 2 ACCEPTED
        if (allTickets.size() >= 8) {
            allTickets.get(0).setStatus(Ticket.TicketStatus.PENDING);
            allTickets.get(1).setStatus(Ticket.TicketStatus.PENDING);
            allTickets.get(2).setStatus(Ticket.TicketStatus.PENDING);
            allTickets.get(3).setStatus(Ticket.TicketStatus.PENDING);
            allTickets.get(4).setStatus(Ticket.TicketStatus.REJECTED);  // Spam ticket
            allTickets.get(5).setStatus(Ticket.TicketStatus.REJECTED);
            allTickets.get(6).setStatus(Ticket.TicketStatus.ACCEPTED);
            allTickets.get(7).setStatus(Ticket.TicketStatus.ACCEPTED);
        }
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
