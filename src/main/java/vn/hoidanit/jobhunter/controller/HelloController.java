package vn.hoidanit.jobhunter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.hoidanit.jobhunter.util.error.IdInvalidException;

@RestController
public class HelloController {

    @GetMapping("/")
    public String getHelloWorld() throws IdInvalidException {

        if (true) {
            throw new IdInvalidException("check page");

        }
        return "Hello World (Hỏi Dân IT & SOS Nu)";
    }
}
