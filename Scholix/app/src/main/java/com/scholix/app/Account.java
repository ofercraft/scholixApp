package com.scholix.app;

public class Account {
    private String username;
    private String password;
    private boolean isEditing = false;
    private String source = "Classroom"; // Default
    private int year = 0; // Only relevant for Bar Ilan (1, 2, 3)

    public Account(String username, String password, String source) {
        this.username = username;
        this.password = password;
        this.source = source;
    }

    // Getters & Setters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getSource() { return source; }
    public boolean isEditing() { return isEditing; }
    public int getYear() { return year; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setSource(String source) { this.source = source; }
    public void setEditing(boolean editing) { this.isEditing = editing; }
    public void setYear(int year) { this.year = year; }
}
