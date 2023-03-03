package io.nkcm.jwt.jwtAuth.services;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // for extracting user details

        if (username.equals("Kshitiz")){
            return new User("Kshitiz","12345", new ArrayList<>());
        }else{
            throw new UsernameNotFoundException("User not found!!");
        }
    }
    
    
}
