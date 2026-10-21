package com.archmaxim.service;

import com.archmaxim.dto.ChangePasswordRequest;
import com.archmaxim.dto.LoginRequest;
import com.archmaxim.dto.LoginResponse;
import com.archmaxim.dto.MeResponse;
import com.archmaxim.entity.Account;
import com.archmaxim.exception.BadRequestException;
import com.archmaxim.exception.InvalidCredentialsException;
import com.archmaxim.exception.NotFoundException;
import com.archmaxim.security.AuthenticatedUser;
import com.archmaxim.security.IssuedToken;
import com.archmaxim.security.JwtService;
import com.archmaxim.security.LoginAttemptLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final AccountDirectory accountDirectory;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptLimiter attemptLimiter;

    // Checked when the email is unknown, so a miss takes as long as a wrong password.
    private final String placeholderHash;

    public AuthService(AccountDirectory accountDirectory,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LoginAttemptLimiter attemptLimiter) {
        this.accountDirectory = accountDirectory;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.attemptLimiter = attemptLimiter;
        this.placeholderHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public LoginResponse login(LoginRequest request, String clientAddress) {
        String email = AccountDirectory.normalizeEmail(request.email());
        String attemptKey = LoginAttemptLimiter.key(email, clientAddress);
        attemptLimiter.checkAllowed(attemptKey);

        List<Account> accounts = accountDirectory.findByEmail(email);
        if (accounts.isEmpty()) {
            passwordEncoder.matches(request.password(), placeholderHash);
        }
        for (Account account : accounts) {
            if (passwordEncoder.matches(request.password(), account.getPasswordHash())) {
                attemptLimiter.recordSuccess(attemptKey);
                IssuedToken token = jwtService.issue(account);
                return new LoginResponse(token.value(), account.getId(), account.getFullName(),
                        account.getRole(), token.expiresAt());
            }
        }

        attemptLimiter.recordFailure(attemptKey);
        throw new InvalidCredentialsException();
    }

    public MeResponse me(AuthenticatedUser user) {
        return MeResponse.from(requireAccount(user));
    }

    @Transactional
    public void changePassword(AuthenticatedUser user, ChangePasswordRequest request) {
        Account account = requireAccount(user);
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new BadRequestException("Your current password is not correct.");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BadRequestException("The new password must be different from the current one.");
        }
        account.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private Account requireAccount(AuthenticatedUser user) {
        return accountDirectory.find(user)
                .orElseThrow(() -> new NotFoundException("Your account no longer exists."));
    }
}
