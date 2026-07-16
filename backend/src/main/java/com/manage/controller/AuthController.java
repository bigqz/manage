package com.manage.controller;

import com.manage.common.Result;
import com.manage.dto.LoginRequest;
import com.manage.service.AuthService;
import com.manage.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/info")
    public Result<LoginVO> info() {
        return Result.ok(authService.currentUserInfo());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
