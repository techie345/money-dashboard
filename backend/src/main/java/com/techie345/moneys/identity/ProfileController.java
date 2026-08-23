package com.techie345.moneys.identity;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.ObjectProvider;
import com.techie345.moneys.audit.AuditService;
import com.techie345.moneys.audit.ChangeOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProfileController {
    private final ObjectProvider<UserRepository> users;
    private final ObjectProvider<AuditService> audit;

    public ProfileController(ObjectProvider<UserRepository> users, ObjectProvider<AuditService> audit) {
        this.users = users;
        this.audit = audit;
    }

    @GetMapping({"/me", "/profile"})
    public ResponseEntity<ProfileResponse> get(CurrentUser currentUser) {
        UserRepository repository = users.getIfAvailable();
        return repository == null ? ResponseEntity.notFound().build() : repository.findById(currentUser.id()).map(user -> ResponseEntity.ok(profile(user.toAccount())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/profile")
    public ResponseEntity<ProfileResponse> update(CurrentUser currentUser,
                                                  @Valid @RequestBody ProfileRequest request) {
        UserRepository repository = users.getIfAvailable();
        return repository == null ? ResponseEntity.notFound().build() : repository.findById(currentUser.id()).map(user -> {
            user.updateProfile(request.displayName(), request.email());
            UserEntity saved = repository.saveAndFlush(user);
            audit.ifAvailable(service -> service.record(saved.getId(), ChangeOperation.UPDATED, "profile", saved.getId(),
                    null, null, 0, "{\"displayName\":\"updated\",\"email\":\"updated\"}"));
            return ResponseEntity.ok(profile(saved.toAccount()));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static ProfileResponse profile(UserAccount account) {
        return new ProfileResponse(account.id(), account.displayName(), account.email(),
                account.createdAt(), account.updatedAt());
    }

    public record ProfileRequest(@NotBlank String displayName, @NotBlank @Email String email) { }

    public record ProfileResponse(UUID id, String displayName, String email,
                                  Instant createdAt, Instant updatedAt) { }
}
