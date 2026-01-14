package com.khoavdse170395.authservice.controller;

import com.khoavdse170395.authservice.model.AuthResponse;
import com.khoavdse170395.authservice.model.LoginRequest;
import com.khoavdse170395.authservice.model.LoginUrlResponse;
import com.khoavdse170395.authservice.model.OtpDtos;
import com.khoavdse170395.authservice.model.RefreshTokenRequest;
import com.khoavdse170395.authservice.model.RegisterRequest;
import com.khoavdse170395.authservice.model.TokenResponse;
import com.khoavdse170395.authservice.model.UserInfo;
import com.khoavdse170395.authservice.service.AuthService;
import com.khoavdse170395.authservice.service.EmailService;
import com.khoavdse170395.authservice.service.KeycloakAdminService;
import com.khoavdse170395.authservice.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "APIs để đăng nhập và đăng ký")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final KeycloakAdminService keycloakAdminService;
    private final EmailService emailService;

    public AuthController(AuthService authService,
                          OtpService otpService,
                          KeycloakAdminService keycloakAdminService,
                          EmailService emailService) {
        this.authService = authService;
        this.otpService = otpService;
        this.keycloakAdminService = keycloakAdminService;
        this.emailService = emailService;
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get current user", 
        description = "Lấy thông tin user hiện tại từ JWT token. Yêu cầu Bearer token hợp lệ."
    )
    @SecurityRequirement(name = "bearer-keycloak")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Token không hợp lệ hoặc đã hết hạn")
    })
    public UserInfo me(@AuthenticationPrincipal Jwt jwt) {
        return authService.getCurrentUser(jwt);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Đăng nhập và nhận access token + refresh token + user info")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
        @ApiResponse(responseCode = "400", description = "Bad Request - Dữ liệu đầu vào không hợp lệ"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Sai username hoặc password")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Đăng ký tài khoản mới với role ROLE_STUDENT mặc định")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đăng ký thành công"),
        @ApiResponse(responseCode = "400", description = "Bad Request - Dữ liệu đầu vào không hợp lệ hoặc username/email đã tồn tại")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/send-otp")
    @Operation(summary = "Send OTP for email registration confirmation")
    public ResponseEntity<Void> sendRegisterOtp(@Valid @RequestBody OtpDtos.RegisterSendOtpRequest request) {
        String otp = otpService.generateRegisterOtp(request.email());
        emailService.sendOtpEmail(
                request.email(),
                "Java Forum - Xác nhận đăng ký tài khoản",
                otp,
                "đăng ký tài khoản mới"
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/confirm")
    @Operation(summary = "Confirm registration with OTP and create user in Keycloak")
    public ResponseEntity<AuthResponse> confirmRegister(
            @Valid @RequestBody OtpDtos.RegisterConfirmOtpRequest request
    ) {
        boolean ok = otpService.verifyRegisterOtp(request.email(), request.otp());
        if (!ok) {
            return ResponseEntity.badRequest().build();
        }

        RegisterRequest registerRequest = new RegisterRequest(
                request.username(),
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName()
        );
        AuthResponse response = authService.register(registerRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/login-url")
    @Operation(
        summary = "Get login URL", 
        description = "Lấy URL để redirect đến Keycloak login page. Sau khi đăng nhập, Keycloak sẽ redirect về redirectUri với code parameter."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công")
    })
    public LoginUrlResponse getLoginUrl(
            @Parameter(description = "URL để redirect sau khi đăng nhập", example = "http://localhost:3000/auth/callback")
            @RequestParam(defaultValue = "http://localhost:8081/auth/callback") String redirectUri) {
        String loginUrl = authService.getLoginUrl(redirectUri);
        String registerUrl = authService.getRegisterUrl(redirectUri);
        return new LoginUrlResponse(
                loginUrl,
                registerUrl,
                "Sử dụng loginUrl để đăng nhập hoặc registerUrl để đăng ký. Sau đó redirect về redirectUri với code parameter."
        );
    }

    @GetMapping("/register-url")
    @Operation(
        summary = "Get register URL", 
        description = "Lấy URL để redirect đến Keycloak registration page. Sau khi đăng ký, Keycloak sẽ redirect về redirectUri với code parameter."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công")
    })
    public LoginUrlResponse getRegisterUrl(
            @Parameter(description = "URL để redirect sau khi đăng ký", example = "http://localhost:3000/auth/callback")
            @RequestParam(defaultValue = "http://localhost:8081/auth/callback") String redirectUri) {
        String registerUrl = authService.getRegisterUrl(redirectUri);
        String loginUrl = authService.getLoginUrl(redirectUri);
        return new LoginUrlResponse(
                loginUrl,
                registerUrl,
                "Sử dụng registerUrl để đăng ký tài khoản mới. Sau đó redirect về redirectUri với code parameter."
        );
    }

    @PostMapping("/token")
    @Operation(
        summary = "Exchange code for token", 
        description = "Đổi authorization code từ Keycloak thành access token và refresh token"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công"),
        @ApiResponse(responseCode = "400", description = "Bad Request - Code không hợp lệ hoặc đã hết hạn")
    })
    public TokenResponse exchangeToken(
            @Parameter(description = "Authorization code từ Keycloak", required = true)
            @RequestParam String code,
            @Parameter(description = "Redirect URI đã sử dụng trong bước trước", example = "http://localhost:3000/auth/callback")
            @RequestParam(defaultValue = "http://localhost:8081/auth/callback") String redirectUri
    ) {
        return authService.exchangeCodeForToken(code, redirectUri);
    }

    @GetMapping("/callback")
    @Operation(
        summary = "OAuth callback", 
        description = "Endpoint callback từ Keycloak sau khi user đăng nhập/đăng ký. Keycloak sẽ redirect về đây với code hoặc error."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công"),
        @ApiResponse(responseCode = "400", description = "Bad Request - Keycloak trả về lỗi")
    })
    public TokenResponse callback(
            @Parameter(description = "Authorization code từ Keycloak", required = true)
            @RequestParam String code,
            @Parameter(description = "Error từ Keycloak (nếu có)", required = false)
            @RequestParam(required = false) String error,
            @Parameter(description = "Redirect URI", example = "http://localhost:3000/auth/callback")
            @RequestParam(defaultValue = "http://localhost:8081/auth/callback") String redirectUri
    ) {
        if (error != null) {
            throw new RuntimeException("Keycloak error: " + error);
        }
        return authService.exchangeCodeForToken(code, redirectUri);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Làm mới access token bằng refresh token. Trả về access token và refresh token mới.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Thành công"),
        @ApiResponse(responseCode = "400", description = "Bad Request - Refresh token không hợp lệ hoặc đã hết hạn")
    })
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send OTP for forgot password")
    public ResponseEntity<Void> sendForgotPasswordOtp(
            @Valid @RequestBody OtpDtos.ForgotPasswordRequest request
    ) {
        String otp = otpService.generateForgotPasswordOtp(request.email());
        emailService.sendOtpEmail(
                request.email(),
                "Java Forum - Xác nhận đặt lại mật khẩu",
                otp,
                "đặt lại mật khẩu"
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password/confirm")
    @Operation(summary = "Reset password with OTP")
    public ResponseEntity<Void> resetPasswordWithOtp(
            @Valid @RequestBody OtpDtos.ResetPasswordWithOtpRequest request
    ) {
        boolean ok = otpService.verifyForgotPasswordOtp(request.email(), request.otp());
        if (!ok) {
            return ResponseEntity.badRequest().build();
        }
        String userId = keycloakAdminService.findUserIdByEmail(request.email());
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        keycloakAdminService.updateUserPassword(userId, request.newPassword());
        return ResponseEntity.ok().build();
    }
}


