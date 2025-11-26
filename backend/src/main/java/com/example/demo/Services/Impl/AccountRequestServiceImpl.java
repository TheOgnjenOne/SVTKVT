package com.example.demo.Services.Impl;


import com.example.demo.DTOs.RegistrationDTOs.RegistrationRequest;
import com.example.demo.Enums.RequestStatus;
import com.example.demo.Enums.UserRole;
import com.example.demo.Model.AccountRequest;
import com.example.demo.Model.User;
import com.example.demo.Repository.IAccountRequestRepository;
import com.example.demo.Repository.IUserRepository;
import com.example.demo.Services.IAccountRequestService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AccountRequestServiceImpl implements IAccountRequestService {

    private final IAccountRequestRepository accountRequestRepository;
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;


    @Autowired
    public AccountRequestServiceImpl(IAccountRequestRepository accountRequestRepository,
                                     IUserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     EmailService emailService) {
        this.accountRequestRepository = accountRequestRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;

    }

    @Override
    public ResponseEntity<Map<String, String>> submitRequest(RegistrationRequest registrationRequest) {
        Map<String, String> response = new HashMap<>();
        Optional<AccountRequest> existingRequest = accountRequestRepository.findByEmail(registrationRequest.getEmail());

        if (existingRequest.isPresent()) {
            RequestStatus status = existingRequest.get().getStatus();
            response.put("status", status.name());

            return ResponseEntity.ok(response);
        }

        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setEmail(registrationRequest.getEmail());
        accountRequest.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        accountRequest.setAddress(registrationRequest.getAddress());
        accountRequest.setStatus(RequestStatus.PENDING);
        accountRequest.setCreatedAt(LocalDateTime.now());
        accountRequestRepository.save(accountRequest);

        response.put("status", "CREATED");
        return ResponseEntity.ok(response);
    }

    @Override
    public void resubmitRequest(String email) {
        Optional<AccountRequest> existingRequest = accountRequestRepository.findByEmail(email);

        if (existingRequest.isPresent() && existingRequest.get().getStatus() == RequestStatus.REJECTED) {
            AccountRequest request = existingRequest.get();
            request.setStatus(RequestStatus.PENDING);
            request.setRejectionReason(null);
            request.setCreatedAt(LocalDateTime.now());

            accountRequestRepository.save(request);
        } else {
            throw new IllegalStateException("Nije moguće ponovo poslati zahtev.");
        }
    }


    @Override
    @Transactional
    public void approveRequest(Long requestId) {
        AccountRequest request = findRequestById(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Ovaj zahtev za registraciju je već obrađen.");
        }

        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getPassword());
        newUser.setAddress(request.getAddress());
        newUser.setRole(UserRole.USER);

        String email = request.getEmail();
        String name = email.split("@")[0];
        newUser.setName(name);

        userRepository.save(newUser);

        request.setStatus(RequestStatus.ACCEPTED);
        accountRequestRepository.save(request);

        emailService.sendSimpleEmail(
                request.getEmail(),
                "Zahtev za registraciju odobren",
                "Vaš zahtev za registraciju je odobren! Možete se prijaviti."
        );
    }

    @Override
    @Transactional
    public void rejectRequest(Long requestId, String reason) {
        AccountRequest request = findRequestById(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Ovaj zahtev za registraciju je već obrađen.");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason);
        accountRequestRepository.save(request);

        emailService.sendSimpleEmail(
                request.getEmail(),
                "Zahtev za registraciju odbijen",
                "Vaš zahtev za registraciju je odbijen. Razlog: " + reason
        );
    }

    @Override
    public List<AccountRequest> getPendingRequests() {
        return accountRequestRepository.findByStatus(RequestStatus.PENDING);
    }

    @Override
    public List<AccountRequest> getRejectedRequests() {
        return accountRequestRepository.findByStatus(RequestStatus.REJECTED);
    }

    @Override
    public List<AccountRequest> getAcceptedRequests() {
        return accountRequestRepository.findByStatus(RequestStatus.ACCEPTED);
    }

    @Override
    public List<AccountRequest> getAllRequests() {
        return accountRequestRepository.findAll();
    }

    private AccountRequest findRequestById(Long requestId) {
        return accountRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Nije pronađen zahtev za registraciju sa ID: " + requestId));
    }

    @Override
    public Optional<AccountRequest> findByEmail(String email) {
        return Optional.ofNullable(accountRequestRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Nije pronađen zahtev za registraciju sa email: " + email)));
    }
}

