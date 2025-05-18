package com.sp.SwimmingPool.controller;

import com.sp.SwimmingPool.dto.UserDTO;
import com.sp.SwimmingPool.model.entity.User; // Import User entity
import com.sp.SwimmingPool.service.EmailService;
import com.sp.SwimmingPool.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/staff") // Base path for staff-related operations
@RequiredArgsConstructor // Lombok for constructor injection of final fields
public class UserController {

    private final UserService userService; // Marked final for injection
    private final EmailService emailService; // Marked final for injection

    /**
     * Endpoint for Admins to create a new staff user.
     * Sends an activation email to the new staff member.
     *
     * @param userDTO DTO containing the initial staff details.
     * @return ResponseEntity containing the created UserDTO or an error message.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createStaffUser(@RequestBody UserDTO userDTO) {
        log.info("Received request to create staff user with email: {}", userDTO.getEmail());
        try {
            // UserService.createUser now returns the saved User entity
            User savedUser = userService.createUser(userDTO);

            // Check if user was actually saved and has an ID
            if (savedUser == null || savedUser.getId() <= 0) {
                log.error("User creation failed unexpectedly for email: {}", userDTO.getEmail());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to save user data.");
            }

            log.info("Staff user created successfully with ID: {}", savedUser.getId());

            // Generate a unique token for activation
            String token = UUID.randomUUID().toString();
            boolean tokenStored = userService.storePasswordToken(savedUser.getId(), token,
                    LocalDateTime.now().plusDays(7)); // 7-day expiry for activation

            if (!tokenStored) {
                log.error("Failed to store activation token for user ID: {}", savedUser.getId());
                // Consider cleanup or alternative handling if token storage fails
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to generate activation process.");
            }

            log.info("Activation token stored for user ID: {}", savedUser.getId());

            // Send invitation email with the activation token
            emailService.sendStaffInvitation(savedUser.getEmail(), token);

            // Convert the saved User entity back to DTO for the response
            UserDTO responseDto = userService.convertToDto(savedUser);

            return new ResponseEntity<>(responseDto, HttpStatus.CREATED);

        } catch (RuntimeException e) { // Catch specific exceptions like email already exists
            log.warn("Failed to create staff user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST) // 400 for known issues like duplicate email
                    .body(e.getMessage());
        } catch (Exception e) { // Catch unexpected errors
            log.error("Unexpected error creating staff user or sending invitation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during staff creation.");
        }
    }

    /**
     * Endpoint for Admins to list all users (staff and potentially others).
     *
     * @return ResponseEntity containing a list of UserDTOs or a no content status.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> listAllUsers() {
        try {
            List<UserDTO> users = userService.listAllUsers();
            if (users.isEmpty()) {
                log.info("No users found in the system.");
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            log.info("Retrieved {} users.", users.size());
            return new ResponseEntity<>(users, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error listing all users: {}", e.getMessage(), e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Endpoint for Admins to get details of a specific user by ID.
     *
     * @param id The ID of the user.
     * @return ResponseEntity containing the UserDTO or a not found/error status.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserDetails(@PathVariable("id") int id) {
        log.info("Request to get details for user ID: {}", id);
        try {
            UserDTO user = userService.getUserDetails(id);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (IllegalArgumentException e) { // User not found
            log.warn("User details request failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error getting user details for ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred retrieving user details.");
        }
    }

    /**
     * Endpoint for Admins to update a user's details.
     *
     * @param id      The ID of the user to update.
     * @param userDTO DTO containing the updated information.
     * @return ResponseEntity containing the updated UserDTO or an error status.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable("id") int id, @RequestBody UserDTO userDTO) {
        log.info("Request to update user ID: {}", id);
        try {
            UserDTO updatedUser = userService.updateUser(id, userDTO);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (IllegalArgumentException e) { // User not found
            log.warn("User update failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating user ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred updating the user.");
        }
    }

    /**
     * Endpoint for Admins to delete a user by ID.
     *
     * @param id The ID of the user to delete.
     * @return ResponseEntity indicating success (No Content) or an error status.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HttpStatus> deleteUser(@PathVariable("id") int id) {
        log.info("Request to delete user ID: {}", id);
        try {
            userService.deleteUser(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Success, no body needed
        } catch (RuntimeException e) { // User not found or other service layer exception
            log.warn("User deletion failed for ID {}: {}", id, e.getMessage());
            // Distinguish between Not Found and other errors if needed
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Or Bad Request depending on error
        } catch (Exception e) {
            log.error("Unexpected error deleting user ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Public endpoint for staff members to activate their account using a token
     * and set their initial password.
     *
     * @param request Map containing "token" and "password".
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/activate") // Path relative to /api/staff -> /api/staff/activate
    public ResponseEntity<?> activateStaffAccount(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String password = request.get("password");
        log.info("Received activation request for token: {}", token != null ? token.substring(0, Math.min(token.length(), 8)) + "..." : "null");

        // Basic validation
        if (token == null || token.isBlank()) {
            log.warn("Activation request failed: Token is missing or blank.");
            return ResponseEntity.badRequest().body("Aktivasyon token'ı gereklidir.");
        }
        if (password == null || password.isBlank()) {
            log.warn("Activation request failed for token {}: Password is missing or blank.", token);
            return ResponseEntity.badRequest().body("Şifre gereklidir.");
        }
        if (password.length() < 8) {
            log.warn("Activation request failed for token {}: Password is too short.", token);
            return ResponseEntity.badRequest().body("Şifre en az 8 karakter olmalıdır.");
        }

        try {
            boolean activated = userService.activateStaffWithToken(token, password);
            if (activated) {
                log.info("Staff account activated successfully for token starting with: {}", token.substring(0, Math.min(token.length(), 8)));
                return ResponseEntity.ok("Hesabınız başarıyla aktifleştirildi.");
            } else {
                // Service method returns false for known issues (invalid/expired token, etc.)
                log.warn("Failed to activate staff account for token starting with: {}. Reason likely invalid/expired token.", token.substring(0, Math.min(token.length(), 8)));
                return ResponseEntity.badRequest().body("Geçersiz veya süresi dolmuş token."); // Keep error generic for security
            }
        } catch (Exception e) {
            // Catch unexpected errors from the service layer
            log.error("Unexpected error during staff activation for token starting with {}: {}", token.substring(0, Math.min(token.length(), 8)), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Hesap aktifleştirme sırasında beklenmedik bir hata oluştu.");
        }
    }
}
