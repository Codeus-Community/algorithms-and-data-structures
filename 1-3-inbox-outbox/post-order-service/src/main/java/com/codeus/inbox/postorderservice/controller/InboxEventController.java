package com.codeus.inbox.postorderservice.controller;

import com.codeus.inbox.postorderservice.service.PostOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class InboxEventController {

    private final PostOrderService postOrderService;

    @GetMapping("/count")
    public long count() {
        return postOrderService.count();
    }
}
