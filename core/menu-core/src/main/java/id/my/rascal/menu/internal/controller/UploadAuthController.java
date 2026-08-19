package id.my.rascal.menu.internal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.image.ImageService;
import id.my.rascal.common.image.ImageUploadAuth;
import id.my.rascal.common.template.SuccessTemplate;

@RestController
@RequestMapping("/api/v1/images/auth")
public class UploadAuthController {

    private final ImageService imageService;

    public UploadAuthController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    public ResponseEntity<SuccessTemplate<ImageUploadAuth>> getUploadAuth() {
        ImageUploadAuth uploadAuth = imageService.generateUploadAuth();

        return ApiResponse.success(
            HttpStatus.OK,
            "Upload credentials successfully generated",
            uploadAuth
        );
    }
}
