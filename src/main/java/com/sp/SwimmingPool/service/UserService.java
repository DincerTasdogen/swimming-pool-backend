package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.dto.UserDTO;
import com.sp.SwimmingPool.model.entity.User;
import com.sp.SwimmingPool.repos.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import for transactional methods

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new staff user with a temporary password and sets UserType to STAFF.
     * The account needs activation via token.
     *
     * @param userDTO DTO containing initial user data (name, surname, email, role).
     * @return The saved User entity with its generated ID.
     * @throws RuntimeException if the email already exists.
     */
    @Transactional // Ensure atomicity
    public User createUser(UserDTO userDTO) {
        // Check if email already exists
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            log.warn("Attempted to create user with existing email: {}", userDTO.getEmail());
            throw new RuntimeException("Email already in use: " + userDTO.getEmail());
        }

        User user = new User();
        user.setName(userDTO.getName());
        user.setSurname(userDTO.getSurname());
        user.setEmail(userDTO.getEmail());
        user.setRole(userDTO.getRole());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString() + System.currentTimeMillis()));
        user.setMemberCount(userDTO.getMemberCount());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("Staff user created with temporary password for email: {}, ID: {}", savedUser.getEmail(), savedUser.getId());
        return savedUser; // Return the entity with the ID
    }

    /**
     * Deletes a user by their ID.
     *
     * @param id The ID of the user to delete.
     * @throws RuntimeException if the user is not found.
     */
    @Transactional
    public void deleteUser(int id) {
        if (!userRepository.existsById(id)) {
            log.warn("Attempted to delete non-existent user with ID: {}", id);
            throw new RuntimeException("User with id " + id + " not found");
        }
        userRepository.deleteById(id);
        log.info("User deleted successfully with ID: {}", id);
    }

    /**
     * Updates an existing user's details based on the provided DTO.
     * Does not update password or activation status here.
     *
     * @param id      The ID of the user to update.
     * @param userDTO DTO containing the new details.
     * @return A DTO representation of the updated user.
     * @throws IllegalArgumentException if the user is not found.
     */
    @Transactional
    public UserDTO updateUser(int id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Attempted to update non-existent user with ID: {}", id);
                    return new IllegalArgumentException("User not found with id: " + id);
                });

        // Update mutable fields
        user.setName(userDTO.getName());
        user.setSurname(userDTO.getSurname());
        // Consider email change implications (re-verification?)
        // user.setEmail(userDTO.getEmail());
        user.setRole(userDTO.getRole());
        user.setMemberCount(userDTO.getMemberCount()); // If applicable
        user.setUpdatedAt(LocalDateTime.now()); // Update timestamp

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", id);
        return convertToDto(updatedUser); // Return DTO from updated entity
    }

    /**
     * Retrieves a list of all users, converted to DTOs.
     *
     * @return A list of UserDTOs.
     */
    @Transactional(readOnly = true) // Read-only transaction for query
    public List<UserDTO> listAllUsers() {
        List<User> users = userRepository.findAll();
        log.debug("Retrieved {} users from database.", users.size());
        return users.stream()
                .map(this::convertToDto) // Use helper method
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the details of a specific user by ID, converted to a DTO.
     *
     * @param id The ID of the user.
     * @return The UserDTO.
     * @throws IllegalArgumentException if the user is not found.
     */
    @Transactional(readOnly = true)
    public UserDTO getUserDetails(int id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Attempted to get details for non-existent user with ID: {}", id);
                    return new IllegalArgumentException("User not found with id: " + id);
                });
        log.debug("Retrieved details for user ID: {}", id);
        return convertToDto(user); // Use helper method
    }

    /**
     * Stores a password reset/activation token for a user.
     *
     * @param userId     The ID of the user.
     * @param token      The token to store.
     * @param expiryDate The token's expiry date/time.
     * @return true if the token was stored successfully, false otherwise.
     */
    @Transactional
    public boolean storePasswordToken(int userId, String token, LocalDateTime expiryDate) {
        if (userId <= 0) {
            log.error("Attempted to store token for invalid userId: {}", userId);
            return false;
        }
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

            user.setPasswordResetToken(token);
            user.setTokenExpiryDate(expiryDate);
            userRepository.save(user);
            log.info("Stored password/activation token for userId: {}", userId);
            return true;
        } catch (IllegalArgumentException e) { // Catch specific exception
            log.error("Failed to store password token for userId {}: {}", userId, e.getMessage());
            return false;
        } catch (Exception e) { // Catch broader exceptions
            log.error("An unexpected error occurred while storing password token for userId {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Activates a staff account using a token and sets the user's password.
     * Clears the token and enables the user account.
     *
     * @param token    The activation token.
     * @param password The new password chosen by the user.
     * @return true if activation is successful, false otherwise (e.g., invalid/expired token, weak password).
     */
    @Transactional
    public boolean activateStaffWithToken(String token, String password) {
        if (token == null || token.isBlank()) {
            log.warn("Attempted activation with null or blank token.");
            return false;
        }
        try {
            User user = userRepository.findByPasswordResetToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid or non-existent token"));

            if (user.getTokenExpiryDate() == null || user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
                log.warn("Attempted activation with expired token for user ID: {}", user.getId());
                // Optionally clear the expired token from the user record here
                user.setPasswordResetToken(null);
                user.setTokenExpiryDate(null);
                userRepository.save(user);
                throw new IllegalArgumentException("Token has expired");
            }

            // Basic password validation (can be enhanced)
            if (password == null || password.length() < 8) {
                log.warn("Attempted activation for user ID {} with password shorter than 8 characters.", user.getId());
                throw new IllegalArgumentException("Password must be at least 8 characters long.");
            }

            // Encode and set the new password
            user.setPassword(passwordEncoder.encode(password));
            user.setPasswordResetToken(null); // Clear the token
            user.setTokenExpiryDate(null); // Clear expiry date
            user.setUpdatedAt(LocalDateTime.now()); // Update timestamp

            userRepository.save(user);
            log.info("Successfully activated account and set password for user ID: {}", user.getId());
            return true;

        } catch (IllegalArgumentException e) { // Catch known issues like invalid/expired token or weak password
            log.warn("Failed to activate staff account using token: {}. Reason: {}", token, e.getMessage());
            return false;
        } catch (Exception e) { // Catch unexpected database or other errors
            log.error("An unexpected error occurred during staff activation for token {}: {}", token, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Converts a User entity to a UserDTO, excluding sensitive information.
     *
     * @param user The User entity.
     * @return The corresponding UserDTO, or null if the input user is null.
     */
    public UserDTO convertToDto(User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setSurname(user.getSurname());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setMemberCount(user.getMemberCount());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
