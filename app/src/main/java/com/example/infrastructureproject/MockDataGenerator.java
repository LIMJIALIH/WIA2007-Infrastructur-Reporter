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

        // Spam - Unrelated Cosmetics
        Ticket spamTicket = new Ticket(
                "TKT005",
                "Other",
                "Low",
                "Online Advertisement",
                "Selling cosmetics and beauty products. Special discount 50% off! Contact us now for amazing deals. This is not related to infrastructure.",
                "2025-11-12 09:00 AM",
                "unrelated_cosmetics"
        );
        tickets.add(spamTicket);

        return tickets;
    }
}
