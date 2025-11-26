package com.example.demo.Repository;

import com.example.demo.Model.Review;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.List;

@Repository
public interface IReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    @Query("SELECT r FROM Review r WHERE r.location.id = :locationId AND r.deleted = false")
    List<Review> findByLocationId(@Param("locationId") Long locationId);


    @Query("SELECT AVG(r.overallRating) FROM Review r WHERE r.location.id = :locationId AND r.deleted = false ")
    Double calculateAverageOverallRatingByLocationId(@Param("locationId") Long locationId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.location.id = :locationId AND r.deleted = false")
    Long countByLocationIdAndDeletedFalse(@Param("locationId") Long locationId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.location.id = :locationId AND r.deleted = false AND r.isHidden = false")
    Long countActiveAndVisibleByLocationId(@Param("locationId") Long locationId);

    List<Review> findByLocationIdAndDeletedFalseOrderByCreatedAtDesc(Long locationId, PageRequest pageable);}
