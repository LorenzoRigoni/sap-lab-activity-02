package ttt_backend.adapters.out;

import ttt_backend.domain.model.User;
import ttt_backend.domain.ports.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JsonUserRepository implements UserRepository {
    private final Map<String, User> users = new HashMap<>();

    @Override
    public User save(User user) {
        this.users.put(user.id(), user);
        return user;
    }

    @Override
    public Optional<User> findUserById(String id) {
        return Optional.ofNullable(this.users.get(id));
    }
}
