package id.my.rascal.image.internal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.image.api.ImageApi;
import id.my.rascal.image.api.ImageUploadAuthApiResponse;

@RestController
@RequestMapping("/api/v1/images/auth")
public class ImageUploadAuthController {

    private final ImageApi imageApi;

    public ImageUploadAuthController(ImageApi imageApi) {
        this.imageApi = imageApi;
    }

    @GetMapping
    public ResponseEntity<SuccessTemplate<ImageUploadAuthApiResponse>> getUploadAuth() {
        ImageUploadAuthApiResponse uploadAuth = imageApi.getAuthenticationParameters();

        return ApiResponse.success(
            HttpStatus.OK,
            "Upload credentials successfully generated",
            uploadAuth
        );
    }

}
