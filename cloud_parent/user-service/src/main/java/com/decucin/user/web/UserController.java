package com.decucin.user.web;


import com.decucin.user.config.PatternProperties;
import com.decucin.user.pojo.User;
import com.decucin.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/user")
//@RefreshScope
public class UserController {

    @Autowired
    private UserService userService;

//     @Value("${pattern.dateformat}")
//     private String dateformat;

    @Autowired
    private PatternProperties patternProperties;

     @GetMapping("now")
     public String now(){
         return LocalDateTime.now().format(DateTimeFormatter.ofPattern(patternProperties.getDateformat()));
     }
    /**
     * 路径： /user/110
     *
     * @param id 用户id
     * @return 用户
     */
    @GetMapping("/{id}")
    public User queryById(@PathVariable("id") Long id) {

        return userService.queryById(id);
    }
}
