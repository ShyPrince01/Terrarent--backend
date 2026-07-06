package com.terrarent.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Booking entity

import com.terrarent.dto.booking.BookingResponse;
import com.terrarent.dto.landlord.LandlordDashboardResponse;
import com.terrarent.dto.landlord.PricingUpdateRequest;
import com.terrarent.dto.property.PropertyResponse;
import com.terrarent.entity.Booking;
import com.terrarent.entity.Property;
import com.terrarent.exception.ResourceNotFoundException;
import com.terrarent.repository.BookingRepository;
import com.terrarent.repository.PropertyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LandlordService {

    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final PropertyService propertyService; // Reuse propertyService for mapping

    public LandlordDashboardResponse getLandlordDashboardMetrics(UUID landlordId) {
        List<Property> landlordProperties = propertyRepository.findByLandlordId(landlordId);
        long totalProperties = landlordProperties.size();
        long activeListings = landlordProperties.stream()
                .filter(p -> p.getStatus() == Property.PropertyStatus.LIVE)
                .count();

        List<Booking> bookings = bookingRepository.findByPropertyLandlordId(landlordId);
        BigDecimal totalRevenue = bookings.stream()
                .map(Booking::getProperty)
                .filter(property -> property != null && property.getNightlyPrice() != null)
                .map(Property::getNightlyPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal occupancyRate = BigDecimal.ZERO;
        if (totalProperties > 0) {
            occupancyRate = BigDecimal.valueOf(bookings.size())
                    .divide(BigDecimal.valueOf(totalProperties), 2, RoundingMode.HALF_UP);
        }

        return LandlordDashboardResponse.builder()
                .totalProperties(totalProperties)
                .activeListings(activeListings)
                .totalRevenue(totalRevenue)
                .occupancyRate(occupancyRate)
                .build();
    }

    public List<PropertyResponse> getLandlordProperties(UUID landlordId) {
        return propertyRepository.findByLandlordId(landlordId).stream()
                .map(propertyService::mapPropertyToPropertyResponse)
                .collect(Collectors.toList());
    }

    public PropertyResponse getLandlordPropertyById(UUID id, UUID landlordId) {
        Property property = propertyRepository.findByIdAndLandlordId(id, landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found or not owned by landlord with id: " + id));
        return propertyService.mapPropertyToPropertyResponse(property);
    }

    @Transactional
    public PropertyResponse updatePropertyPricing(UUID id, PricingUpdateRequest request, UUID landlordId) {
        Property property = propertyRepository.findByIdAndLandlordId(id, landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found or not owned by landlord with id: " + id));

        if (request.getAnnualPrice() != null) {
            property.setAnnualPrice(request.getAnnualPrice());
        }
        if (request.getNightlyPrice() != null) {
            property.setNightlyPrice(request.getNightlyPrice());
        }

        Property updatedProperty = propertyRepository.save(property);
        return propertyService.mapPropertyToPropertyResponse(updatedProperty);
    }

    public List<BookingResponse> getPropertyAvailability(UUID propertyId, UUID landlordId) {
        // Ensure the landlord owns the property
        Property property = propertyRepository.findByIdAndLandlordId(propertyId, landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found or not owned by landlord with id: " + propertyId));

        return bookingRepository.findByPropertyId(propertyId).stream()
                .map(this::mapBookingToBookingResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingRequests(UUID landlordId) {
        return bookingRepository.findByPropertyLandlordId(landlordId).stream()
                .map(this::mapBookingToBookingResponse)
                .collect(Collectors.toList());
    }

    // Helper method to map Booking entity to BookingResponse DTO
    private BookingResponse mapBookingToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .property(propertyService.mapPropertyToPropertyResponse(booking.getProperty()))
                .renter(UserService.mapUserToUserResponse(booking.getRenter())) // Assuming a static mapper in UserService for simple user mapping
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
