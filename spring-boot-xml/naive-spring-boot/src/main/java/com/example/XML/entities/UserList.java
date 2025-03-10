package com.example.XML.entities;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.ArrayList;

@XmlRootElement(name = "UserList")
public class UserList {

    @XmlElement(name = "groupName")
    private String groupName;

    @XmlElement(name = "User")
    private List<User> users;

    // No-arg constructor required by JAXB
    public UserList() {
        this.groupName = "Default Group"; // Set a default name
        this.users = new ArrayList<>();
    }

    public UserList(String groupName, List<User> users) {
        this.groupName = groupName;
        this.users = users != null ? users : new ArrayList<>();
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
    }

    public void addUser(User user) {
        if (user != null) {
            this.users.add(user);
        }
    }
}
