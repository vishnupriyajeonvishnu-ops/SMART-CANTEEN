package com.dmi.canteen.auth;

import com.dmi.canteen.common.BusinessException;
import com.dmi.canteen.common.ResourceNotFoundException;
import com.dmi.canteen.user.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 10;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final CaptchaService captchaService;

    public AuthService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       StaffRepository staffRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       CaptchaService captchaService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.captchaService = captchaService;
    }

    @Transactional
    public AuthResponse registerStudent(RegisterRequest req) {
        // 1. Verify Captcha
        if (!captchaService.validateCaptcha(req.getCaptchaId(), req.getCaptchaCode())) {
            throw new BusinessException("Invalid or expired captcha code. Please refresh and try again.");
        }

        // 2. Validate passwords match
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException("Password and confirm password do not match.");
        }

        // 3. Check unique constraints
        if (userRepository.existsByUsername(req.getUsername().trim())) {
            throw new BusinessException("Username '" + req.getUsername().trim() + "' is already in use.");
        }

        if (userRepository.existsByEmail(req.getEmail().trim().toLowerCase())) {
            throw new BusinessException("Email address '" + req.getEmail().trim() + "' is already registered.");
        }

        if (studentRepository.existsByRegisterNumber(req.getRegisterNumber().trim())) {
            throw new BusinessException("Student Register Number '" + req.getRegisterNumber().trim() + "' is already registered.");
        }

        // 4. Create and save User
        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setFullName(req.getFullName().trim());
        user.setPhone(req.getPhone() != null ? req.getPhone().trim() : null);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.ROLE_STUDENT);
        user.setActive(true);
        user.setFailedAttempts(0);
        User savedUser = userRepository.save(user);

        // 5. Create and save Student profile
        Student student = new Student();
        student.setUser(savedUser);
        student.setRegisterNumber(req.getRegisterNumber().trim());
        student.setDepartment(req.getDepartment().trim());
        student.setYearOfStudy(req.getYearOfStudy());
        studentRepository.save(student);

        // 6. Generate JWT token
        String token = tokenProvider.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(token, buildUserDto(savedUser));
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        // 1. Verify Captcha
        if (!captchaService.validateCaptcha(req.getCaptchaId(), req.getCaptchaCode())) {
            throw new BusinessException("Invalid or expired captcha code. Please refresh and try again.");
        }

        String email = req.getEmail().trim().toLowerCase();
        String username = req.getUsername().trim();

        // 2. Find user matching email
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new BadCredentialsException("Invalid email/username or password.");
        }

        User user = userOpt.get();

        // Check if username also matches
        if (!user.getUsername().equalsIgnoreCase(username)) {
            throw new BadCredentialsException("Email and username combination do not match.");
        }

        // 3. Check lockout
        if (user.getLockoutUntil() != null && LocalDateTime.now().isBefore(user.getLockoutUntil())) {
            long minutesRemaining = Duration.between(LocalDateTime.now(), user.getLockoutUntil()).toMinutes() + 1;
            throw new LockedException("Account is locked due to 5 consecutive failed attempts. Please retry after " + minutesRemaining + " minute(s).");
        }

        // 4. Verify password
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            int newAttempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(newAttempts);
            if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockoutUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                userRepository.save(user);
                throw new LockedException("Account locked for " + LOCKOUT_MINUTES + " minutes due to 5 consecutive failed login attempts.");
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid email/username or password. Attempt " + newAttempts + " of " + MAX_FAILED_ATTEMPTS + ".");
        }

        // 5. Check if active
        if (!user.isActive()) {
            throw new BusinessException("Your account is currently disabled. Please contact canteen management.");
        }

        // 6. Reset failed attempts
        user.setFailedAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);

        // 7. Generate JWT token
        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());

        return new AuthResponse(token, buildUserDto(user));
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return buildUserDto(user);
    }

    public UserDto buildUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setRoleDisplay(user.getRole().getCleanName());

        if (user.getRole() == Role.ROLE_STUDENT) {
            studentRepository.findByUserId(user.getId()).ifPresent(student -> {
                dto.setRegisterNumber(student.getRegisterNumber());
                dto.setDepartment(student.getDepartment());
                dto.setYearOfStudy(student.getYearOfStudy());
            });
        } else if (user.getRole() == Role.ROLE_STAFF || user.getRole() == Role.ROLE_MANAGER) {
            staffRepository.findByUserId(user.getId()).ifPresent(staff -> {
                dto.setEmployeeId(staff.getEmployeeId());
                dto.setDesignation(staff.getDesignation());
            });
        }

        return dto;
    }
}
