package com.scholix.app;

public class Grade {
    private String subject;
    private String name;
    private String grade;

    public Grade(String subject, String name, String grade) {
        this.subject = subject;
        this.name = name;
        this.grade = grade;
    }

    public String getSubject() {
        return subject;
    }

    public String getGrade() {
        return grade;
    }

    public String getName() {
        return name;
    }

}
