package com.example.demo.Controller;

import com.example.demo.DTOs.LoginDTOs.LoginRequest;
import com.example.demo.DTOs.LoginDTOs.LoginResponse;
import com.example.demo.DTOs.RegistrationDTOs.RegistrationRequest;
import com.example.demo.Enums.RequestStatus;
import com.example.demo.JwtAuth.JwtUtil;
import com.example.demo.Model.AccountRequest;
import com.example.demo.Model.User;
import com.example.demo.Services.IAccountRequestService;
import com.example.demo.Services.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final IAccountRequestService accountRequestService;
    private final IUserService userService;

    @Autowired
    public AuthenticationController(AuthenticationManager authenticationManager,
                                    JwtUtil jwtUtil,
                                    IAccountRequestService accountRequestService, IUserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.accountRequestService = accountRequestService;
        this.userService = userService;
    }


    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@Valid @RequestBody RegistrationRequest registrationRequest) {
        Map<String, String> response = new HashMap<>();
        try {
            accountRequestService.submitRequest(registrationRequest);
            logger.info("Zahtev za registraciju poslat: Email={}", registrationRequest.getEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            logger.warn("Neuspešan zahtev za registraciju: Email={}. Razlog: {}", registrationRequest.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/resubmit")
    public ResponseEntity<Map<String, String>> resubmitRequest(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Map<String, String> response = new HashMap<>();

        try {
            accountRequestService.resubmitRequest(email);
            response.put("message", "Zahtev je ponovo poslat na proveru.");
            logger.info("Zahtev za ponovno slanje poslat: Email={}", email);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            response.put("message", e.getMessage());
            logger.warn("Neuspešno ponovno slanje zahteva: Email={}. Razlog: {}", email, e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        Map<String, String> response = new HashMap<>();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();

            User user = userService.findByEmail(email);

            if (user == null) {
                response.put("message", "Korisnik nije pronađen!");
                logger.warn("Pokušaj prijave sa pronađenim JWT-om, ali korisnik ne postoji u DB: Email={}", email);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            String role = user.getRole().name();
            String jwt = jwtUtil.generateToken(email, role);

            logger.info("Korisnik se USPEŠNO prijavio: Email={}, Rola={}", email, role);

            return ResponseEntity.ok(new LoginResponse(jwt));

        } catch (AuthenticationException e) {
            String email = loginRequest.getEmail();

            Optional<AccountRequest> requestOpt = accountRequestService.findByEmail(email);
            if (requestOpt.isPresent()) {
                RequestStatus status = requestOpt.get().getStatus();

                if (status == RequestStatus.PENDING || status == RequestStatus.REJECTED) {
                    logger.warn("Odbijena prijava za korisnika: Email={}. Status zahteva: {}", email, status.name());

                    Map<String, String> pendingResponse = new HashMap<>();
                    pendingResponse.put("status", "REQUEST_STATUS_CHECK");
                    pendingResponse.put("email", email);

                    return new ResponseEntity<>(pendingResponse, HttpStatus.ACCEPTED);
                }
            }

            response.put("message", "Netacni email ili sifra!");
            logger.warn("Neuspešna prijava: Netacni podaci za Email={}. Razlog: {}", loginRequest.getEmail(), e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }


    @GetMapping("/current-user")
    public ResponseEntity<Map<String, String>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Pokušaj pristupa '/current-user' bez autentifikacije.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        logger.info("Dohvaćeni detalji trenutnog korisnika: Email={}", email);
        Map<String, String> response = new HashMap<>();
        response.put("email", email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/registration-status")
    public ResponseEntity<?> getRegistrationStatus(@RequestParam String email) {

        Optional<AccountRequest> requestOpt = accountRequestService.findByEmail(email);

        if (requestOpt.isEmpty()) {
            logger.info("Provera statusa registracije: Zahtev za email {} NIJE PRONAĐEN.", email);
            return ResponseEntity.ok().body(Map.of("status", "NOT_FOUND", "reason", ""));
        }

        AccountRequest request = requestOpt.get();
        String reason = request.getRejectionReason() != null ? request.getRejectionReason() : "";

        logger.info("Provera statusa registracije za email {}: Status={}", email, request.getStatus().name());

        return ResponseEntity.ok().body(Map.of(
                "status", request.getStatus().name(),
                "reason", reason
        ));
    }


}