package ttt_backend.domain.ports;

import ttt_backend.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findUserById(String id);
}
