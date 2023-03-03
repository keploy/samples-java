package com.example.samplesjavajwt.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor

public class AuthenticationResponse {

    private final String jwt;
}
