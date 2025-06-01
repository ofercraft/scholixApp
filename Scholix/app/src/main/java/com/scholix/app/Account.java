package com.scholix.app;

public class Account {
    private String name;
    private String username;
    private String password;
    private boolean isEditing = false;
    private String source = "Classroom"; // Default
    private int year = 0; // Only relevant for Bar Ilan (1, 2, 3)
    private int location; //0 - main, 1 - secondary...

    public Account(String username, String password, String source, String name) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.source = source;
    }

    // Getters & Setters
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getName() { return name; }

    public String getSource() { return source; }
    public boolean isEditing() { return isEditing; }
    public int getYear() { return year; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setName(String name) { this.name = name; }

    public boolean isMain() {
        return location==0;
    }

    public void setMain(boolean main) {
        location=0;
    }
    public void setLocation(int location) { this.location = location; }

    public void setSource(String source) { this.source = source; }
    public void setEditing(boolean editing) { this.isEditing = editing; }
    public void setYear(int year) { this.year = year; }
}
