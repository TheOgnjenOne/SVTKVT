package com.example.demo.DTOs.ReviewDTOs;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ReviewRequestDTO {
    private Long eventId;
    private Long locationId;

    @Size(max = 1000, message = "Komentar ne sme biti duži od 1000 karaktera.")
    private String commentText;

    @NotNull(message = "Ocena performansa je obavezna.")
    @Min(value = 1, message = "Ocena mora biti između 1 i 5.")
    @Max(value = 5, message = "Ocena mora biti između 1 i 5.")
    private Integer performanceRating;

    @NotNull(message = "Ocena zvuka/svetla je obavezna.")
    @Min(value = 1, message = "Ocena mora biti između 1 i 5.")
    @Max(value = 5, message = "Ocena mora biti između 1 i 5.")
    private Integer soundLightingRating;

    @NotNull(message = "Ocena prostora je obavezna.")
    @Min(value = 1, message = "Ocena mora biti između 1 i 5.")
    @Max(value = 5, message = "Ocena mora biti između 1 i 5.")
    private Integer venueRating;

    @NotNull(message = "Ukupna ocena je obavezna.")
    @Min(value = 1, message = "Ocena mora biti između 1 i 5.")
    @Max(value = 5, message = "Ocena mora biti između 1 i 5.")
    private Integer overallRating;


    // --- Getteri i Setteri ---

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public Integer getPerformanceRating() {
        return performanceRating;
    }

    public void setPerformanceRating(Integer performanceRating) {
        this.performanceRating = performanceRating;
    }

    public Integer getSoundLightingRating() {
        return soundLightingRating;
    }

    public void setSoundLightingRating(Integer soundLightingRating) {
        this.soundLightingRating = soundLightingRating;
    }

    public Integer getVenueRating() {
        return venueRating;
    }

    public void setVenueRating(Integer venueRating) {
        this.venueRating = venueRating;
    }

    public Integer getOverallRating() {
        return overallRating;
    }

    public void setOverallRating(Integer overallRating) {
        this.overallRating = overallRating;
    }
}
