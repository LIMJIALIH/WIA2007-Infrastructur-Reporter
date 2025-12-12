package com.example.infrastructureproject;

import java.io.Serializable;

public class Ticket implements Serializable {
    private String id;
    private String type;
    private String severity;
    private String location;
    private String description;
    private String dateTime;
    private String imageName;
    private TicketStatus status;

    public enum TicketStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        SPAM
    }

    public Ticket(String id, String type, String severity, String location,
                  String description, String dateTime, String imageName) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.location = location;
        this.description = description;
        this.dateTime = dateTime;
        this.imageName = imageName;
        this.status = TicketStatus.PENDING;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
}