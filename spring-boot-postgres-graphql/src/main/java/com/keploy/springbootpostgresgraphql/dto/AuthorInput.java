package com.keploy.springbootpostgresgraphql.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorInput {
    private String firstName;
    private String lastName;
}
