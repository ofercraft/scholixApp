package com.scholix.app;

public class ScheduleItem {
    public int hourNum;
    public String subject;
    public String teacher;
    public String colorClass;
    public String changes; // Combined change information
    public boolean isExam;
    public String examSubject;

    public ScheduleItem(int hourNum, String subject, String teacher, String colorClass, String changes) {
        this.hourNum = hourNum;
        this.subject = subject;
        this.teacher = teacher;
        this.colorClass = colorClass;
        this.changes = changes;
        this.isExam = false;
        this.examSubject = "";
    }

    // Append new change info (with a newline if already present)
    public void addChange(String changeText) {
        if (changeText == null || changeText.isEmpty()) return;
        if (this.changes == null || this.changes.isEmpty()) {
            this.changes = changeText;
        } else {
            this.changes += "\n" + changeText;
        }
    }
    public void setColorClass(String colorClass) {
        this.colorClass=colorClass;
    }
    // Set exam details
    public void setExam(String examTitle) {
        System.out.println("DASDas");
        this.isExam = true;
        this.examSubject = examTitle;
    }
    @Override
    public String toString() {
        return "ScheduleItem{" +
                "hourNum=" + hourNum +
                ", subject='" + subject + '\'' +
                ", teacher='" + teacher + '\'' +
                ", colorClass='" + colorClass + '\'' +
                ", changes='" + changes + '\'' +
                ", isExam=" + isExam +
                ", examSubject='" + examSubject + '\'' +
                '}';
    }

}
