package pre.assignment.rsupport.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class AccessController {

    @GetMapping("/success")
    public String success() {
        log.info("정상 요청 처리됨");
        return "OK";
    }

    @GetMapping("/error")
    public String error() {
        log.error("에러 발생! 원인=테스트 오류");
        throw new RuntimeException("테스트 예외");
    }

}
