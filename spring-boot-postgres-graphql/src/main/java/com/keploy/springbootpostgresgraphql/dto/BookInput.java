package com.keploy.springbootpostgresgraphql.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookInput {
    private String name;
    private int pageCount;
    private int authorId;
    private Integer categoryId;
}
