package com.web.dihx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class TestController {

    @RequestMapping("/test1")
    public ModelAndView test1(String status) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("test1");

//        System.out.println("Status : " + status);
        return modelAndView;
    }
    @RequestMapping("/test2")
    public ModelAndView test2(String status) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("test2");
        System.out.println("Status : " + status);
        return modelAndView;
    }
}