package com.codearena.controller;

import com.codearena.dto.AdminContestRequest;
import com.codearena.dto.AdminProblemDetailDto;
import com.codearena.dto.AdminProblemRequest;
import com.codearena.service.AdminService;
import com.codearena.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin CRUD for problems and contests. Every endpoint requires an
 * {@code X-Auth-Token} belonging to a user with the ADMIN role. Admin status is
 * granted automatically to accounts whose email is on the configured allowlist.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;

    public AdminController(AdminService adminService, AuthService authService) {
        this.adminService = adminService;
        this.authService = authService;
    }

    @GetMapping("/verify")
    public Map<String, Object> verify(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        requireAdmin(token);
        return Map.of("ok", true);
    }

    @PostMapping("/problems")
    public Map<String, Object> createProblem(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                             @Valid @RequestBody AdminProblemRequest request) {
        requireAdmin(token);
        return Map.of("id", adminService.createProblem(request), "slug", request.slug());
    }

    @GetMapping("/problems/{slug}")
    public AdminProblemDetailDto getProblem(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                            @PathVariable String slug) {
        requireAdmin(token);
        return adminService.getProblem(slug);
    }

    @PutMapping("/problems/{slug}")
    public Map<String, Object> updateProblem(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                             @PathVariable String slug,
                                             @Valid @RequestBody AdminProblemRequest request) {
        requireAdmin(token);
        adminService.updateProblem(slug, request);
        return Map.of("ok", true);
    }

    @DeleteMapping("/problems/{slug}")
    public Map<String, Object> deleteProblem(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                             @PathVariable String slug) {
        requireAdmin(token);
        adminService.deleteProblem(slug);
        return Map.of("ok", true);
    }

    @PostMapping("/contests")
    public Map<String, Object> createContest(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                             @Valid @RequestBody AdminContestRequest request) {
        requireAdmin(token);
        return Map.of("id", adminService.createContest(request));
    }

    @DeleteMapping("/contests/{id}")
    public Map<String, Object> deleteContest(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                             @PathVariable Long id) {
        requireAdmin(token);
        adminService.deleteContest(id);
        return Map.of("ok", true);
    }

    private void requireAdmin(String token) {
        if (!authService.isAdminToken(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
