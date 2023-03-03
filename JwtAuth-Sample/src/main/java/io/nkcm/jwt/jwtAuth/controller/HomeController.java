package io.nkcm.jwt.jwtAuth.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @RequestMapping(value = {"/home","/"})
    public String welcome(){
        return "This is sample application for JWT Authernication\n\tSuccessfully Authenticated";
    }
    
}
