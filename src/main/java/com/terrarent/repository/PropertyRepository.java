package com.terrarent.repository;

import com.terrarent.entity.Property;
import com.terrarent.entity.Property.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {
    List<Property> findByLandlordId(UUID landlordId);
    Optional<Property> findByIdAndLandlordId(UUID id, UUID landlordId);
    boolean existsByIdAndLandlordId(UUID id, UUID landlordId);
    Optional<Property> findByExternalId(String externalId);
    List<Property> findByStatus(PropertyStatus status);

    // For advanced filtering with JpaSpecificationExecutor
    // Page<Property> findAll(Specification<Property> spec, Pageable pageable);
}
