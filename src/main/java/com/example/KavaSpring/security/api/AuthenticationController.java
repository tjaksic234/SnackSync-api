package com.example.KavaSpring.security.api;

import com.example.KavaSpring.models.dao.User;
import com.example.KavaSpring.repository.UserRepository;
import com.example.KavaSpring.security.api.dto.LoginRequest;
import com.example.KavaSpring.security.api.dto.LoginResponse;
import com.example.KavaSpring.security.api.dto.RegisterUserRequest;
import com.example.KavaSpring.security.utils.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
public class AuthenticationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("register")
    public ResponseEntity<String> register(@RequestBody RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail()) || request.getEmail() == null) {
            return new ResponseEntity<>("Email is already taken or it is not entered in correct format!", HttpStatus.BAD_REQUEST);
        }

        if (request.getFirstName() == null || request.getLastName() == null) {
            return new ResponseEntity<>("First name and last name are required fields", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        return new ResponseEntity<>("User registered successfully", HttpStatus.OK);
    }

    @PostMapping("login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
        ));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtUtils.generateJwtToken(authentication);

        ResponseCookie cookie = jwtUtils.createJwtCookie(token);


        return  ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse(token));
    }


}
