package com.atguigu.gulimall.order.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping("/{page}.html")
    public String page(@PathVariable String page) {
        return page;
    }
}
