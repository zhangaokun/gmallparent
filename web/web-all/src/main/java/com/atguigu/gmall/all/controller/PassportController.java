package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
@Controller
public class PassportController {
    @GetMapping("login.html")
    public String login(HttpServletRequest request){
        //从哪里点击登陆应该跳转到哪里
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl",originUrl);
        return "login";
    }
}
