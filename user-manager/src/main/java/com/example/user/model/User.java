package com.example.user.model;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;

@Data
@ToString
@Accessors(chain = true)
public class User {

    @Id
    private long id;
    private String name;
    private Integer age;
    private String birthday;

}