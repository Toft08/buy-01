package com.buyapp.mediaservice.repository;

import com.buyapp.mediaservice.model.Avatar;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AvatarRepository extends MongoRepository<Avatar, String> {
    Optional<Avatar> findByUserId(String userId);

    void deleteByUserId(String userId);

    boolean existsByUserId(String userId);
}

