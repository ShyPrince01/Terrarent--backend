package com.terrarent.controller;

import com.terrarent.dto.booking.BookingResponse;
import com.terrarent.dto.bookingcom.BookingSearchRequest;
import com.terrarent.dto.bookingcom.CreateExternalBookingRequest;
import com.terrarent.service.BookingComService;
import com.terrarent.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/booking-com")
@RequiredArgsConstructor
@Tag(name = "Booking.com Affiliate API", description = "Endpoints for interacting with live Booking.com hotel data")
public class BookingComController {

    private final BookingComService bookingComService;
    private final UserService userService;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userService.getUserIdByEmail(userDetails.getUsername());
        }
        throw new RuntimeException("User not authenticated.");
    }

    @Operation(summary = "Search for accommodations via Booking.com API")
    @PostMapping(value = "/search", produces = "application/json")
    public ResponseEntity<String> searchHotels(@RequestBody BookingSearchRequest request) {
        String response = bookingComService.searchAccommodations(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Book an accommodation from external live data (static local booking)")
    @PostMapping("/book")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()") // Any logged-in user can book
    public ResponseEntity<BookingResponse> bookHotel(@Valid @RequestBody CreateExternalBookingRequest request) {
        UUID renterId = getCurrentUserId();
        BookingResponse response = bookingComService.bookAccommodation(renterId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
