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
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Grade grade1 = (Grade) obj;
        return subject.equals(grade1.subject) &&
                name.equals(grade1.name) &&
                grade.equals(grade1.grade);
    }

    @Override
    public int hashCode() {
        return (subject + name + grade).hashCode();
    }

}
