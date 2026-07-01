package com.terrarent.dto.bookingcom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for creating a static booking on an external hotel from live data")
public class CreateExternalBookingRequest {

    @NotBlank(message = "Hotel ID is required")
    @Schema(description = "External partner's unique hotel ID", example = "bookingcom_eko_hotels")
    private String hotelId;

    @NotBlank(message = "Title is required")
    @Schema(description = "Title of the external hotel", example = "Eko Hotels & Suites")
    private String title;

    @NotBlank(message = "Description is required")
    @Schema(description = "Detailed description of the hotel")
    private String description;

    @NotBlank(message = "Address is required")
    @Schema(description = "Full physical address of the hotel", example = "Plot 1415 Adetokunbo Ademola St, Victoria Island, Lagos")
    private String address;

    @NotNull(message = "Nightly price is required")
    @Schema(description = "Nightly rate for booking", example = "150000.00")
    private BigDecimal nightlyPrice;

    @Schema(description = "Primary image URL of the hotel", example = "https://images.unsplash.com/photo-1566073771259-6a8506099945")
    private String imageUrl;

    @NotNull(message = "Booking date is required")
    @FutureOrPresent(message = "Booking date must be in the present or future")
    @Schema(description = "Desired date for the stay", example = "2026-06-15")
    private LocalDate bookingDate;

    @Schema(description = "Latitude of the hotel", example = "6.4267")
    private BigDecimal latitude;

    @Schema(description = "Longitude of the hotel", example = "3.4301")
    private BigDecimal longitude;

    @Schema(description = "Optional status of the booking", example = "CONFIRMED", allowableValues = {"PENDING", "CONFIRMED", "CANCELLED", "COMPLETED"})
    private String status;
}
