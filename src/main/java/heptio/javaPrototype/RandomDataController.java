package heptio.javaPrototype;

import heptio.database.EasyReaderWriter;
import heptio.util.RandomStringGenerator;
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
        EasyReaderWriter db;
        db = EasyReaderWriter.getInstance();
        db.synchronousWrite(RandomStringGenerator.generateString(254));
        db.asynchronousWrite(RandomStringGenerator.generateString(254));
        return "Success!";
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(RandomDataController.class, args);
    }
}