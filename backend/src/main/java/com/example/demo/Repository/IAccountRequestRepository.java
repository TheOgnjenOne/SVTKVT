package com.example.demo.Repository;

import com.example.demo.Enums.RequestStatus;
import com.example.demo.Model.AccountRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IAccountRequestRepository extends JpaRepository<AccountRequest, Long> {

    List<AccountRequest> findByStatus(RequestStatus status);

    Optional<AccountRequest> findByEmail(String email);



    boolean existsByEmail(String email);

    boolean existsByEmailAndStatus(String email, RequestStatus status);
}
