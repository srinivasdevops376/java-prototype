package heptio.javaPrototype;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Controller
@EnableAutoConfiguration
public class RandomDataController {

    @RequestMapping("/")
    @ResponseBody
    String home() {
        return "This request has been stored, along with a random string";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(RandomDataController.class, args);
    }
}