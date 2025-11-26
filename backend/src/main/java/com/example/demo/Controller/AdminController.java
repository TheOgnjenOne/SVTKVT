package com.example.demo.Controller;

import com.example.demo.DTOs.ManagerDTOs.ManagerAssignmentRequest;
import com.example.demo.DTOs.RegistrationDTOs.RejectionRequest;
import com.example.demo.DTOs.ManagerDTOs.ManagerInfoDTO;
import com.example.demo.DTOs.UserDTOs.AvailableUserDTO;
import com.example.demo.Model.AccountRequest;
import com.example.demo.Services.IAccountRequestService;
import com.example.demo.Services.ILocationManagerService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final IAccountRequestService accountRequestService;
    private final ILocationManagerService locationManagerService;

    @Autowired
    public AdminController(IAccountRequestService accountRequestService, ILocationManagerService locationManagerService) {
        this.accountRequestService = accountRequestService;
        this.locationManagerService = locationManagerService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AccountRequest>> getPendingRequests() {
        logger.info("Admin fetching list of pending account requests.");
        return ResponseEntity.ok(accountRequestService.getPendingRequests());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<String> approveRequest(@PathVariable Long id) {
        try {
            accountRequestService.approveRequest(id);
            logger.info("Account request with ID {} approved successfully. New user created.", id);
            return ResponseEntity.ok("Request approved successfully. User account created.");
        } catch (EntityNotFoundException e) {
            logger.warn("Attempt to approve non-existent request ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.error("Failed to approve request ID {} due to state: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<String> rejectRequest(@PathVariable Long id, @Valid @RequestBody RejectionRequest rejectionRequest) {
        try {
            accountRequestService.rejectRequest(id, rejectionRequest.getReason());
            logger.info("Account request with ID {} rejected. Reason: {}", id, rejectionRequest.getReason());
            return ResponseEntity.ok("Request rejected successfully.");
        } catch (EntityNotFoundException e) {
            logger.warn("Attempt to reject non-existent request ID: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.error("Failed to reject request ID {} due to state: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/manager/assign")
    public ResponseEntity<String> assignManager(@Valid @RequestBody ManagerAssignmentRequest request) {
        try {
            locationManagerService.assignManager(
                    request.getUserId(),
                    request.getLocationId(),
                    request.getStartDate(),
                    request.getEndDate()
            );
            logger.info("Manager assigned. User ID: {}, Location ID: {}, Period: {} to {}",
                    request.getUserId(), request.getLocationId(), request.getStartDate(), request.getEndDate());
            return ResponseEntity.ok("User successfully assigned as manager for the location.");
        } catch (EntityNotFoundException e) {
            logger.warn("Failed to assign manager: User or location not found. User ID: {}, Location ID: {}",
                    request.getUserId(), request.getLocationId());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (IllegalStateException e) {
            logger.error("Failed to assign manager due to state: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @PostMapping("/manager/unassign")
    public ResponseEntity<String> unassignManager(@RequestBody ManagerAssignmentRequest request) {
        try {
            locationManagerService.unassignManager(request.getUserId(), request.getLocationId());
            logger.info("Manager unassigned. User ID: {}, Location ID: {}", request.getUserId(), request.getLocationId());
            return ResponseEntity.ok("User successfully unassigned as manager for the location.");
        } catch (EntityNotFoundException e) {
            logger.warn("Failed to unassign manager: User or location not found. User ID: {}, Location ID: {}",
                    request.getUserId(), request.getLocationId());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (IllegalStateException e) {
            logger.error("Failed to unassign manager due to state: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/location/{locationId}/managers")
    public ResponseEntity<List<ManagerInfoDTO>> getActiveManagersForLocation(@PathVariable Long locationId) {
        try {
            List<ManagerInfoDTO> managers = locationManagerService.getActiveManagersForLocation(locationId);
            logger.info("Admin retrieved {} active managers for location ID: {}", managers.size(), locationId);
            return ResponseEntity.ok(managers);
        } catch (EntityNotFoundException e) {
            logger.warn("Attempt to retrieve managers for non-existent location ID: {}", locationId);
            return ResponseEntity.status(404).body(null);
        } catch (Exception e) {
            logger.error("Internal server error while fetching managers for location ID {}: {}", locationId, e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/users/available")
    public ResponseEntity<List<AvailableUserDTO>> getAvailableUsers() {
        try {
            List<AvailableUserDTO> users = locationManagerService.getUsersAvailableForAssignment();
            logger.info("Admin retrieved list of {} users available for manager assignment.", users.size());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Internal server error while fetching available users: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }
}