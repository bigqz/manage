package com.manage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.manage.common.Result;
import com.manage.dto.RoleSaveRequest;
import com.manage.service.RoleService;
import com.manage.vo.RoleVO;
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

import java.util.List;

@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/list")
    public Result<List<RoleVO>> list() {
        return Result.ok(roleService.listAll());
    }

    @GetMapping("/page")
    public Result<Page<RoleVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String roleName) {
        return Result.ok(roleService.page(pageNum, pageSize, roleName));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody RoleSaveRequest request) {
        roleService.create(request);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody RoleSaveRequest request) {
        roleService.update(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }
}
