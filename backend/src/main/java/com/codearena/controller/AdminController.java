package com.codearena.controller;

import com.codearena.dto.AdminContestRequest;
import com.codearena.dto.AdminProblemRequest;
import com.codearena.service.AdminService;
import com.codearena.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
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
 * Admin CRUD for problems and contests. Every endpoint requires EITHER the
 * {@code X-Admin-Key} header, OR an {@code X-Auth-Token} belonging to a user
 * with the ADMIN role.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;
    private final String adminKey;

    public AdminController(AdminService adminService,
                           AuthService authService,
                           @Value("${codearena.admin.key}") String adminKey) {
        this.adminService = adminService;
        this.authService = authService;
        this.adminKey = adminKey;
    }

    @GetMapping("/verify")
    public Map<String, Object> verify(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                      @RequestHeader(value = "X-Auth-Token", required = false) String token) {
        requireAdmin(key, token);
        return Map.of("ok", true);
    }

    @PostMapping("/problems")
    public Map<String, Object> createProblem(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                             @RequestHeader(value = "X-Auth-Token", required = false) String token,
                                             @Valid @RequestBody AdminProblemRequest request) {
        requireAdmin(key, token);
        return Map.of("id", adminService.createProblem(request), "slug", request.slug());
    }

    @PutMapping("/problems/{slug}")
    public Map<String, Object> updateProblem(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                             @RequestHeader(value = "X-Auth-Token", required = false) String token,
                                             @PathVariable String slug,
                                             @Valid @RequestBody AdminProblemRequest request) {
        requireAdmin(key, token);
        adminService.updateProblem(slug, request);
        return Map.of("ok", true);
    }

    @DeleteMapping("/problems/{slug}")
    public Map<String, Object> deleteProblem(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                             @RequestHeader(value = "X-Auth-Token", required = false) String token,
                                             @PathVariable String slug) {
        requireAdmin(key, token);
        adminService.deleteProblem(slug);
        return Map.of("ok", true);
    }

    @PostMapping("/contests")
    public Map<String, Object> createContest(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                             @RequestHeader(value = "X-Auth-Token", required = false) String token,
                                             @Valid @RequestBody AdminContestRequest request) {
        requireAdmin(key, token);
        return Map.of("id", adminService.createContest(request));
    }

    @DeleteMapping("/contests/{id}")
    public Map<String, Object> deleteContest(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                             @RequestHeader(value = "X-Auth-Token", required = false) String token,
                                             @PathVariable Long id) {
        requireAdmin(key, token);
        adminService.deleteContest(id);
        return Map.of("ok", true);
    }

    private void requireAdmin(String key, String token) {
        if (key != null && adminKey.equals(key)) {
            return;
        }
        if (authService.isAdminToken(token)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }
}
