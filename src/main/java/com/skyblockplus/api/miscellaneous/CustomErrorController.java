package com.skyblockplus.api.miscellaneous;

import com.skyblockplus.api.templates.ErrorTemplate;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomErrorController implements ErrorController {

  @RequestMapping("/error")
  public ErrorTemplate error() {
    return new ErrorTemplate("false", "Invalid request");
  }

  @Override
  public String getErrorPath() {
    return "/error";
  }
}
