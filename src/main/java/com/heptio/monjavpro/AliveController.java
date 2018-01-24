package com.heptio.monjavpro;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Controller
@EnableAutoConfiguration
public class AliveController {

    @RequestMapping("/alive")
    @ResponseBody
    String home() {
        return "The service is healthy, and responding.";
    }

}