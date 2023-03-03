package com.example.samplesjavajwt.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException{
//        you can enter any username but the password must be "keploy"
//        if connected to database, then instead of hardcoded password, we should get the password from database
        return new User(userName, "keploy", new ArrayList<>());
    }

}
