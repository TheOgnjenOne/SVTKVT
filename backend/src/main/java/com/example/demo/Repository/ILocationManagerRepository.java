package com.example.demo.Repository;

import com.example.demo.Model.LocationManager;
import com.example.demo.Model.LocationManagerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ILocationManagerRepository extends JpaRepository<LocationManager, LocationManagerId> {

    List<LocationManager> findById_UserId(Long userId);

    List<LocationManager> findById_LocationId(Long locationId);

    Optional<LocationManager> findById_UserIdAndId_LocationId(Long userId, Long locationId);

    @Query("SELECT lm FROM LocationManager lm " +
            "WHERE lm.user.id = :userId AND lm.location.id = :locationId " +
            "AND lm.startDate <= CURRENT_DATE AND (lm.endDate IS NULL OR lm.endDate >= CURRENT_DATE)")
    Optional<LocationManager> findActiveManager(
            @Param("userId") Long userId,
            @Param("locationId") Long locationId);

    @Query("SELECT lm FROM LocationManager lm JOIN FETCH lm.user u " +
            "WHERE lm.location.id = :locationId " +
            "AND (lm.endDate IS NULL OR lm.endDate >= CURRENT_DATE)")
    List<LocationManager> findAllActiveByLocationId(
            @Param("locationId") Long locationId);

    @Query("SELECT lm FROM LocationManager lm " +
            "WHERE lm.user.id = :userId " +
            "AND (lm.endDate IS NULL OR lm.endDate >= CURRENT_DATE)")
    List<LocationManager> findAllActiveByUserId(@Param("userId") Long userId);
}
