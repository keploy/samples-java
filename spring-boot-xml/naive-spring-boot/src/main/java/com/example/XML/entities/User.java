package com.example.XML.entities;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "User")
public class User {
    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "age")
    private int age;

    @XmlElement(name = "mobile")
    private String mobile;

    public User() {}

    public User(String name, int age, String mobile) {
        this.name = name;
        this.age = age;
        this.mobile = mobile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
