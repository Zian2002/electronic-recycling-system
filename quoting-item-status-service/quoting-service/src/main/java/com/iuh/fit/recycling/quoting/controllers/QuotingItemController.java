package com.iuh.fit.recycling.quoting.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iuh.fit.recycling.quoting.entities.QuotingItem;
import com.iuh.fit.recycling.quoting.models.GetQuotingPriceRequest;
import com.iuh.fit.recycling.quoting.models.QuotingItemRequest;
import com.iuh.fit.recycling.quoting.services.QuotingItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/quote")
public class QuotingItemController {
    private final QuotingItemService quotingItemService;

    @PostMapping("/get-price")
    public ResponseEntity<QuotingItem> getQuotingPrice(@RequestBody GetQuotingPriceRequest request){
        return ResponseEntity.ok(quotingItemService.getQuotingPrice(request));
    }

    @PostMapping
    public ResponseEntity<QuotingItem> add(@RequestBody QuotingItemRequest request) throws JsonProcessingException {
        return ResponseEntity.ok(quotingItemService.add(request));
    }

    @GetMapping("/item/{quotingItemId}")
    public ResponseEntity<QuotingItem> findById(@PathVariable String quotingItemId){
        return ResponseEntity.ok(quotingItemService.findById(quotingItemId));
    }

    @GetMapping("/items")
    public ResponseEntity<List<QuotingItem>> findAll(){
        return ResponseEntity.ok(quotingItemService.findAll());
    }

    @GetMapping("/exist/{quotingItemId}")
    public ResponseEntity<Boolean> existByQuotingItemId(@PathVariable String quotingItemId){
        return ResponseEntity.ok(quotingItemService.existByQuotingItemId(quotingItemId));
    }

}
