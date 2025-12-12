package com.example.infrastructureproject;

import java.util.ArrayList;
import java.util.List;

public class MockDataGenerator {

    public static List<Ticket> generateMockTickets() {
        List<Ticket> tickets = new ArrayList<>();

        // High Severity - Roadhole (Road category)
        tickets.add(new Ticket(
                "TKT001",
                "Road",
                "High",
                "Jalan Sultan Ismail, Kuala Lumpur",
                "Large pothole detected on main road causing traffic hazard. Approximately 2 meters wide and 30cm deep. Multiple vehicles have been damaged.",
                "2025-11-16 08:30 AM",
                "roadhole"
        ));

        // High Severity - Tree Falldown (Environment category)
        tickets.add(new Ticket(
                "TKT002",
                "Environment",
                "High",
                "Taman Botani Perdana, Kuala Lumpur",
                "Large tree has fallen across the pedestrian pathway blocking access. Poses safety risk to park visitors. Immediate removal required.",
                "2025-11-15 02:45 PM",
                "tree_falldown"
        ));

        // Medium Severity - Burst Pipe (Utilities category)
        tickets.add(new Ticket(
                "TKT003",
                "Utilities",
                "Medium",
                "Jalan Gasing, Petaling Jaya",
                "Water pipe burst causing flooding on residential street. Water supply affected for approximately 20 households in the area.",
                "2025-11-14 11:20 AM",
                "crisafulli_burst_pipe"
        ));

        // Low Severity - No Car Park (Facilities category)
        tickets.add(new Ticket(
                "TKT004",
                "Facilities",
                "Low",
                "Plaza Low Yat, Bukit Bintang",
                "Insufficient parking spaces during peak hours. Visitors unable to find parking. Suggestion to expand parking facilities or implement better traffic flow.",
                "2025-11-13 04:15 PM",
                "nocarpark"
        ));

        // Accepted Ticket 1
        Ticket acceptedTicket = new Ticket(
                "TKT006",
                "Road",
                "High",
                "Jalan Tun Razak",
                "Traffic light malfunction causing congestion. Repair crew dispatched.",
                "2025-11-11 01:30 PM",
                "roadhole"
        );
        acceptedTicket.setStatus(Ticket.TicketStatus.ACCEPTED);
        tickets.add(acceptedTicket);

        // Accepted Ticket 2
        Ticket acceptedTicket2 = new Ticket(
                "TKT008",
                "Facilities",
                "Medium",
                "KL Sentral",
                "Escalator leading to platform 3 is not working. Maintenance scheduled.",
                "2025-11-17 10:00 AM",
                "nocarpark"
        );
        acceptedTicket2.setStatus(Ticket.TicketStatus.ACCEPTED);
        tickets.add(acceptedTicket2);

        // Rejected Ticket 1
        Ticket rejectedTicket = new Ticket(
                "TKT007",
                "Other",
                "Low",
                "Private Property",
                "Complaint about neighbor's fence painting color. This falls under private dispute.",
                "2025-11-10 11:00 AM",
                "unrelated_cosmetics"
        );
        rejectedTicket.setStatus(Ticket.TicketStatus.REJECTED);
        tickets.add(rejectedTicket);

        // Rejected Ticket 2
        Ticket rejectedTicket2 = new Ticket(
                "TKT009",
                "Environment",
                "Low",
                "Local Park",
                "Complaint about birds chirping too loudly. Not an infrastructure issue.",
                "2025-11-18 07:00 AM",
                "tree_falldown"
        );
        rejectedTicket2.setStatus(Ticket.TicketStatus.REJECTED);
        tickets.add(rejectedTicket2);

        return tickets;
    }
}
