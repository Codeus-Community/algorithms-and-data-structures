package org.codeus.consistent_hashing.controller;
import lombok.RequiredArgsConstructor;
import org.codeus.consistent_hashing.dto.AddNodeResponse;
import org.codeus.consistent_hashing.dto.InitResponse;
import org.codeus.consistent_hashing.dto.RemoveNodeResponse;
import org.codeus.consistent_hashing.service.SimpleHashingManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v0/")
@RequiredArgsConstructor
public class SimpleHashingController {

    private final SimpleHashingManager simpleHashingManager;

    @PostMapping("/init")
    public ResponseEntity<InitResponse> initialize(@RequestParam int nodes) {
        if (nodes <= 0) {
            throw new IllegalArgumentException("Number of nodes must be positive");
        }

        InitResponse response = simpleHashingManager.initializeHashing(nodes);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/nodes")
    public ResponseEntity<AddNodeResponse> addNode() {
        AddNodeResponse response = simpleHashingManager.addNode();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/nodes/{index}")
    public ResponseEntity<RemoveNodeResponse> removeNode(@PathVariable int index) {
        RemoveNodeResponse response = simpleHashingManager.removeNode(index);
        return ResponseEntity.ok(response);
    }
}