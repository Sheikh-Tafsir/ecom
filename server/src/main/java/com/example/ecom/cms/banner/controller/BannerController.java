package com.example.ecom.cms.banner.controller;

import com.example.ecom.cms.banner.dto.BannerRequest;
import com.example.ecom.cms.banner.dto.BannerResponse;
import com.example.ecom.cms.banner.service.BannerService;
import com.example.ecom.cms.banner.validator.BannerValidator;
import com.example.ecom.common.dto.ApiResponse;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.common.utils.ResponseUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.ecom.common.utils.Utils.checkErrors;

@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    private final BannerValidator bannerValidator;

    private final MessageService messageService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> findAllActive() {
        List<BannerResponse> banners = bannerService.findAllActive();
        return ResponseUtils.ok(banners, messageService.get("successfully.found", "Active Banner List"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerResponse>>> findAll() {
        List<BannerResponse> banners = bannerService.findAll();
        return ResponseUtils.ok(banners, messageService.get("successfully.found", "Banner List"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BannerResponse>> create(@Valid @RequestBody BannerRequest request,
                                                              BindingResult bindingResult) {

        bannerValidator.validateCreate(request, bindingResult);
        checkErrors(bindingResult);

        BannerResponse banner = bannerService.create(request);
        return ResponseUtils.created(banner, messageService.get("successfully.created", "Banner"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> update(@PathVariable Long id,
                                                              @Valid @RequestBody BannerRequest request,
                                                              BindingResult bindingResult) {

        bannerValidator.validateUpdate(id, request, bindingResult);
        checkErrors(bindingResult);

        BannerResponse banner = bannerService.update(id, request);
        return ResponseUtils.ok(banner, messageService.get("successfully.updated", "Banner"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "Banner"));
    }
}
