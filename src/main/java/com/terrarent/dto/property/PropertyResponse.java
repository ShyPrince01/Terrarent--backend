package com.terrarent.dto.property;

import com.terrarent.dto.image.ImageResponse;
import com.terrarent.entity.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO for property details")
public class PropertyResponse {
    @Schema(description = "Unique identifier of the property", example = "d1e2f3a4-b5c6-7890-1234-567890abcdef")
    private UUID id;
    @Schema(description = "Unique identifier of the property if imported from external partner like Booking.com", example = "bookingcom_eko_hotels")
    private String externalId;
    @Schema(description = "Title of the property listing", example = "Spacious Downtown Apartment")
    private String title;
    @Schema(description = "Detailed description of the property")
    private String description;
    @Schema(description = "Full address of the property", example = "123 Main St, Anytown, USA")
    private String address;
    @Schema(description = "Price of the property", example = "1500.00")
    private BigDecimal nightlyPrice;
    @Schema(description = "Number of bedrooms", example = "2")
    private BigDecimal annualPrice;
    @Schema(description = "Number of bedrooms", example = "2")
    private Integer bedrooms;
    @Schema(description = "Number of bathrooms", example = "1")
    private Integer bathrooms;
    @Schema(description = "Type of property", example = "APARTMENT", allowableValues = {"HOUSE", "APARTMENT", "CONDO", "TOWNHOUSE", "VILLA", "STUDIO", "OTHER"})
    private Property.PropertyType propertyType;
    @Schema(description = "Geographic coordinates (latitude,longitude)", example = "34.0522,-118.2437")
    private String coordinates;
    @Schema(description = "Current status of the property listing", example = "LIVE", allowableValues = {"PENDING", "LIVE", "REJECTED", "DELETED"})
    private Property.PropertyStatus status;
    @Schema(description = "Unique identifier of the landlord who owns this property", example = "e1f2a3b4-c5d6-7890-1234-567890abcdef")
    private UUID landlordId;
    @Schema(description = "Average rating of the property based on reviews", example = "4.50")
    private BigDecimal averageRating;
    @Schema(description = "Total number of reviews for the property", example = "10")
    private Integer numberOfReviews;
    @Schema(description = "List of images associated with the property")
    private List<ImageResponse> images;
    @Schema(description = "Set of amenities available at the property")
    private Set<AmenityResponse> amenities;
}
