package com.fesi.deadlinemate.domain.gatheringDraft.controller;

import com.fesi.deadlinemate.domain.gatheringDraft.dto.request.CreateGatheringDraftRequest;
import com.fesi.deadlinemate.domain.gatheringDraft.dto.request.UpdateGatheringDraftRequest;
import com.fesi.deadlinemate.domain.gatheringDraft.dto.response.GatheringDraftDetailResponse;
import com.fesi.deadlinemate.domain.gatheringDraft.dto.response.GatheringDraftListResponse;
import com.fesi.deadlinemate.domain.gatheringDraft.dto.response.GatheringDraftSaveResponse;
import com.fesi.deadlinemate.domain.gatheringDraft.service.GatheringDraftQueryService;
import com.fesi.deadlinemate.domain.gatheringDraft.service.GatheringDraftService;
import com.fesi.deadlinemate.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gatherings/drafts")
@RequiredArgsConstructor
public class GatheringDraftController {

    private final GatheringDraftService gatheringDraftService;
    private final GatheringDraftQueryService gatheringDraftQueryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GatheringDraftSaveResponse> create(
            @RequestBody @Valid CreateGatheringDraftRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        GatheringDraftSaveResponse response = gatheringDraftService.create(request.toCommand(userId));
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<GatheringDraftListResponse> getMyDrafts(
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(gatheringDraftQueryService.getMyDrafts(userId));
    }

    @GetMapping("/{draftId}")
    public ApiResponse<GatheringDraftDetailResponse> getDraftDetail(
            @PathVariable Long draftId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(gatheringDraftQueryService.getDraftDetail(draftId, userId));
    }

    @PutMapping("/{draftId}")
    public ApiResponse<GatheringDraftSaveResponse> update(
            @PathVariable Long draftId,
            @RequestBody @Valid UpdateGatheringDraftRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        GatheringDraftSaveResponse response = gatheringDraftService.update(draftId, request.toCommand(userId));
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{draftId}")
    public ApiResponse<Void> delete(
            @PathVariable Long draftId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        gatheringDraftService.delete(draftId, userId);
        return ApiResponse.success();
    }
}