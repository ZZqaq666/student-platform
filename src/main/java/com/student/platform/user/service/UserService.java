package com.student.platform.user.service;

import com.student.platform.common.dto.AuthResponse;
import com.student.platform.common.dto.LoginRequest;
import com.student.platform.common.dto.RegisterRequest;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);
}
