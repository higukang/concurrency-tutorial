package kr.higu.clickcountersystem.controller;

import kr.higu.clickcountersystem.service.Step1CounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RequiredArgsConstructor
class Step1PageController {

    private final Step1CounterService step1CounterService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("count", step1CounterService.getCount());
        return "index";
    }

    @PostMapping("/click")
    public String click() {
        step1CounterService.increase();
        return "redirect:/";
    }
}

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class Step1CounterController {

    private final Step1CounterService step1CounterService;

    @PostMapping("/click")
    public void click() {
        step1CounterService.increase();
    }

    @GetMapping("/count")
    public Long getCount() {
        return step1CounterService.getCount();
    }
}
