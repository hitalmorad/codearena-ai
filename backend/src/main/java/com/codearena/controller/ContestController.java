package com.codearena.controller;

import com.codearena.dto.ContestDetailDto;
import com.codearena.dto.ContestSummaryDto;
import com.codearena.dto.RegisterRequest;
import com.codearena.dto.StandingRowDto;
import com.codearena.realtime.SseHub;
import com.codearena.service.ContestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestService contestService;
    private final SseHub sseHub;

    public ContestController(ContestService contestService, SseHub sseHub) {
        this.contestService = contestService;
        this.sseHub = sseHub;
    }

    @GetMapping
    public List<ContestSummaryDto> list() {
        return contestService.listContests();
    }

    @GetMapping("/{id}")
    public ContestDetailDto detail(@PathVariable Long id,
                                   @RequestParam(required = false) String username) {
        return contestService.getContest(id, username);
    }

    @PostMapping("/{id}/register")
    public void register(@PathVariable Long id, @Valid @RequestBody RegisterRequest request) {
        contestService.register(id, request.username());
    }

    @GetMapping("/{id}/standings")
    public List<StandingRowDto> standings(@PathVariable Long id) {
        return contestService.standings(id);
    }

    @GetMapping("/{id}/standings/stream")
    public SseEmitter standingsStream(@PathVariable Long id) {
        return sseHub.subscribe("contest-" + id);
    }
}
