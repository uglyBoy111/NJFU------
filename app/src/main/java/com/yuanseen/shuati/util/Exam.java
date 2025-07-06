package com.yuanseen.shuati.util;

import java.util.Date;

public class Exam {
    private String name;
    private Date date;

    public Exam(String name, Date date) {
        this.name = name;
        this.date = date;
    }

    // Getter和Setter方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
