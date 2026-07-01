package com.terrarent.service;

import com.terrarent.dto.property.AmenityResponse;
import com.terrarent.dto.property.PropertyFilterRequest;
import com.terrarent.dto.property.PropertyRequest;
import com.terrarent.dto.property.PropertyResponse;
import com.terrarent.dto.util.PageResponse;
import com.terrarent.entity.Amenity;
import com.terrarent.entity.Property;
import com.terrarent.entity.PropertyImage;
import com.terrarent.entity.User;
import com.terrarent.exception.ResourceNotFoundException;
import com.terrarent.repository.AmenityRepository;
import com.terrarent.repository.PropertyRepository;
import com.terrarent.repository.UserRepository;
import com.terrarent.dto.image.ImageResponse; // Import ImageResponse
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.terrarent.service.PropertySpecifications.*;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final AmenityRepository amenityRepository;
    private final ImageService imageService; // Assuming an ImageService for image handling

    public PageResponse<PropertyResponse> getAllProperties(PropertyFilterRequest filters, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<Property> spec = Specification.where(hasStatus(Property.PropertyStatus.LIVE)); // Only show LIVE properties by default

        if (filters.getAddress() != null && !filters.getAddress().isEmpty()) {
            spec = spec.and(hasAddressLike(filters.getAddress()));
        }
        if (filters.getMinPrice() != null) {
            spec = spec.and(hasPriceGreaterThanOrEqualTo(filters.getMinPrice()));
        }
        if (filters.getMaxPrice() != null) {
            spec = spec.and(hasPriceLessThanOrEqualTo(filters.getMaxPrice()));
        }
        if (filters.getMinBedrooms() != null) {
            spec = spec.and(hasBedroomsGreaterThanOrEqualTo(filters.getMinBedrooms()));
        }
        if (filters.getMaxBedrooms() != null) {
            spec = spec.and(hasBedroomsLessThanOrEqualTo(filters.getMaxBedrooms()));
        }
        if (filters.getMinBathrooms() != null) {
            spec = spec.and(hasBathroomsGreaterThanOrEqualTo(filters.getMinBathrooms()));
        }
        if (filters.getMaxBathrooms() != null) {
            spec = spec.and(hasBathroomsLessThanOrEqualTo(filters.getMaxBathrooms()));
        }

        Page<Property> propertiesPage = propertyRepository.findAll(spec, pageable);

        List<PropertyResponse> content = propertiesPage.getContent().stream()
                .map(this::mapPropertyToPropertyResponse)
                .collect(Collectors.toList());

        return PageResponse.<PropertyResponse>builder()
                .content(content)
                .totalPages(propertiesPage.getTotalPages())
                .totalElements(propertiesPage.getTotalElements())
                .size(propertiesPage.getSize())
                .number(propertiesPage.getNumber())
                .first(propertiesPage.isFirst())
                .last(propertiesPage.isLast())
                .empty(propertiesPage.isEmpty())
                .build();
    }

    // Add this inside PropertyService
    public UUID getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email))
                .getId();
    }

    public PropertyResponse getPropertyById(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
        return mapPropertyToPropertyResponse(property);
    }

    @Transactional
    public PropertyResponse createProperty(PropertyRequest request, UUID landlordId) {
        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with id: " + landlordId));

        Property property = Property.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .address(request.getAddress())
                .annualPrice(request.getAnnualPrice())
                .nightlyPrice(request.getNightlyPrice())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .propertyType(request.getPropertyType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .landlord(landlord)
                .status(Property.PropertyStatus.PENDING) // Default status for new properties
                .build();

        // Handle amenities
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            property.setAmenities(amenities);
        }

        Property savedProperty = propertyRepository.save(property);

        // Handle images (assuming imageUrls are already uploaded and we're just linking them)
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<PropertyImage> images = request.getImageUrls().stream()
                    .map(url -> PropertyImage.builder()
                            .property(savedProperty)
                            .imageUrl(url)
                            .isPrimary(request.getImageUrls().indexOf(url) == 0) // First image as primary
                            .build())
                    .collect(Collectors.toList());
            savedProperty.setImages(images); // Set images on the property entity
            propertyRepository.save(savedProperty); // Save again to cascade images
        }


        return mapPropertyToPropertyResponse(savedProperty);
    }

    @Transactional
    public PropertyResponse updateProperty(UUID id, PropertyRequest request, UUID landlordId) {
        Property property = propertyRepository.findByIdAndLandlordId(id, landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found or not owned by landlord with id: " + id));

        property.setTitle(request.getTitle());
        property.setDescription(request.getDescription());
        property.setAddress(request.getAddress());
        property.setAnnualPrice(request.getAnnualPrice());
        property.setNightlyPrice(request.getNightlyPrice());
        property.setBedrooms(request.getBedrooms());
        property.setBathrooms(request.getBathrooms());
        property.setPropertyType(request.getPropertyType());
        property.setLatitude(request.getLatitude());
        property.setLongitude(request.getLongitude());

        // Update amenities
        if (request.getAmenityIds() != null) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findAllById(request.getAmenityIds()));
            property.setAmenities(amenities);
        }

        // Image updates would be more complex, likely in a separate ImageService or specific endpoints
        // For simplicity here, we'll assume image updates are handled separately for now.

        Property updatedProperty = propertyRepository.save(property);
        return mapPropertyToPropertyResponse(updatedProperty);
    }

    @Transactional
    public void deleteProperty(UUID id, UUID landlordId) {
        if (!propertyRepository.existsByIdAndLandlordId(id, landlordId)) {
            throw new ResourceNotFoundException("Property not found or not owned by landlord with id: " + id);
        }
        propertyRepository.deleteById(id);
    }

    public PropertyResponse mapPropertyToPropertyResponse(Property property) {
        String coordinates = null;
        if (property.getLatitude() != null && property.getLongitude() != null) {
            coordinates = property.getLatitude().toString() + "," + property.getLongitude().toString();
        }

        List<ImageResponse> imageResponses = property.getImages() != null ?
                property.getImages().stream()
                        .map(image -> ImageResponse.builder()
                                .id(image.getId())
                                .url(image.getImageUrl())
                                .isPrimary(image.getIsPrimary())
                                .build())
                        .collect(Collectors.toList()) :
                List.of();

        Set<AmenityResponse> amenityResponses = property.getAmenities() != null ?
                property.getAmenities().stream()
                        .map(amenity -> AmenityResponse.builder()
                                .id(amenity.getId())
                                .name(amenity.getName())
                                .build())
                        .collect(Collectors.toSet()) :
                Set.of();

        return PropertyResponse.builder()
                .id(property.getId())
                .externalId(property.getExternalId())
                .title(property.getTitle())
                .description(property.getDescription())
                .address(property.getAddress())
                .annualPrice(property.getAnnualPrice())
                .nightlyPrice(property.getNightlyPrice())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .propertyType(property.getPropertyType())
                .coordinates(coordinates)
                .status(property.getStatus())
                .landlordId(property.getLandlord().getId())
                .averageRating(property.getAverageRating())
                .numberOfReviews(property.getNumberOfReviews())
                .images(imageResponses)
                .amenities(amenityResponses)
                .build();
    }
}
