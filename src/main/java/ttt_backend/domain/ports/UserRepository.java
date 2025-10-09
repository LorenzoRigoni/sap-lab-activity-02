package ttt_backend.domain.ports;

import ttt_backend.domain.models.User;

import java.util.Optional;

/**
 * This interface represents the port for the users' repository.
 */
public interface UserRepository {

    /**
     * Save the user in the repository.
     *
     * @param user the user to save
     * @return the user saved
     */
    User save(User user);

    /**
     * Get the user saved.
     *
     * @param id the id of the user
     * @return the user
     */
    Optional<User> getUserById(String id);
}
