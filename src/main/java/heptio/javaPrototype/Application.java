package heptio.javaPrototype;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    /**
     * The main entrypoint of our monolithic application
     *
     * @param args
     */
    public static void main(String[] args) {

        /*
         * Run the noisy application controller concurrently
         */
        Runnable task = () -> {
           NoisyApplicationController app = NoisyApplicationController.getInstance();
           try {
               app.run();
           }catch (Exception e) {
               logger.error("Unable to run noisy application controller: " + e.getMessage());
               System.exit(99);
           }
           System.exit(97);
        };
        Thread thread = new Thread(task);
        thread.start();

        /*
         * Start the spring boot application
         */
        SpringApplication.run(Application.class, args);
    }
}
