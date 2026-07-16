package com.manage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.manage.common.Result;
import com.manage.dto.AssignRolesRequest;
import com.manage.dto.UserSaveRequest;
import com.manage.service.UserService;
import com.manage.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/page")
    public Result<Page<UserVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username) {
        return Result.ok(userService.page(pageNum, pageSize, username));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserSaveRequest request) {
        userService.create(request);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody UserSaveRequest request) {
        userService.update(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest request) {
        userService.assignRoles(id, request);
        return Result.ok();
    }
}
