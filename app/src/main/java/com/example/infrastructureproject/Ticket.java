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
    private String imageUrl; // Full URL to image in Supabase Storage
    private String reporterId; // User ID of reporter from Supabase
    private String username;
    private TicketStatus status;
    private String reason; // Reason for accept/reject
    private String assignedTo; // Engineer assigned to
    private String councilNotes; // Additional notes from council

    public enum TicketStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        SPAM,
        UNDER_REVIEW
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
        this.username = "Anonymous";
        this.status = TicketStatus.PENDING;
        this.reason = "";
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

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    
    // Get display-friendly status text
    public String getStatusDisplayText() {
        if (status == null) return "Pending";
        switch (status) {
            case ACCEPTED:
                return "Completed";
            case REJECTED:
            case SPAM:
                return "SPAM";
            case UNDER_REVIEW:
                return "Under Review";
            case PENDING:
            default:
                return "Pending";
        }
    }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    
    public String getCouncilNotes() { return councilNotes; }
    public void setCouncilNotes(String councilNotes) { this.councilNotes = councilNotes; }
    
    // Get image resource ID from image name
    public int getImageResId(android.content.Context context) {
        return context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
    }
}