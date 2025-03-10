package com.example.XML.entities;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "People")
public class People {
    @XmlElement(name = "UserList")
    private List<UserList> userLists;

    public People() {}

    public People(List<UserList> userLists) {
        this.userLists = userLists;
    }

    public List<UserList> getUserLists() {
        return userLists;
    }

    public void setUserLists(List<UserList> userLists) {
        this.userLists = userLists;
    }
}
