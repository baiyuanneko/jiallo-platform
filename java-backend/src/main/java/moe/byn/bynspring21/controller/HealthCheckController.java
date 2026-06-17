package moe.byn.bynspring21.controller;

import moe.byn.bynspring21.entity.base.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("health")
@RestController
public class HealthCheckController {
    @GetMapping("check")
    public R healthCheck() {
        return R.ok();
    }
}
