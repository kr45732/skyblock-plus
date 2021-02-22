package com.skyblockplus.api.miscellaneous;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<?> error() {
        return new ResponseEntity<>("Invalid Path", HttpStatus.BAD_REQUEST);
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }
}