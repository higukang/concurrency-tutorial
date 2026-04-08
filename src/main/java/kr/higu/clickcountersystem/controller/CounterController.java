package kr.higu.clickcountersystem.controller;

import kr.higu.clickcountersystem.service.CounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class CounterController {

    private final CounterService counterService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("count", counterService.getCount());
        return "index";
    }

    @PostMapping("/click")
    public String click() {
        counterService.increase();
        return "redirect:/";
    }
}
