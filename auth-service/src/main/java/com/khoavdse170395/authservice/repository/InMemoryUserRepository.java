package com.khoavdse170395.authservice.repository;

import com.khoavdse170395.authservice.model.UserInfo;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, UserInfo> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<UserInfo> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public UserInfo save(UserInfo userInfo) {
        storage.put(userInfo.id(), userInfo);
        return userInfo;
    }
}


