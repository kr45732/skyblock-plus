package com.SkyblockBot.API.Service;

import com.SkyblockBot.API.Models.ErrorTemplate;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ErrorTemplate error() {
        return new ErrorTemplate("false", "invalid path");
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }
}