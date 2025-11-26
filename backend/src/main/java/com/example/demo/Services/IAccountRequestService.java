package com.example.demo.Services;

import com.example.demo.DTOs.RegistrationDTOs.RegistrationRequest;
import com.example.demo.Model.AccountRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IAccountRequestService {

    ResponseEntity<Map<String, String>> submitRequest(RegistrationRequest requestDTO);

    void resubmitRequest(String email);

    List<AccountRequest> getPendingRequests();

    List<AccountRequest> getRejectedRequests();

    List<AccountRequest> getAcceptedRequests();

    List<AccountRequest> getAllRequests();

    void approveRequest(Long requestId);

    void rejectRequest(Long requestId, String reason);

    Optional<AccountRequest> findByEmail(String email);
}
