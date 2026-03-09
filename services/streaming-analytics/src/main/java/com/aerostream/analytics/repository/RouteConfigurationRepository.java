package com.aerostream.analytics.repository;

import com.aerostream.analytics.model.RouteConfigurationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RouteConfigurationRepository extends MongoRepository<RouteConfigurationDocument, String> {
}
