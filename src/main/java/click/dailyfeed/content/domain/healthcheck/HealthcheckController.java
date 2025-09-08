package click.dailyfeed.content.domain.healthcheck;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/healthcheck")
public class HealthcheckController {

    @GetMapping("/ready")
    public String ready(){
        return "OK";
    }

    @GetMapping("/live")
    public String live(){
        return "OK";
    }

    @GetMapping("/startup")
    public String startup(){
        return "OK";
    }
}
