package mb.fw.policeporms.annotaion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RestController    // @Controller + @ResponseBody
@Profile("receiver") // receiver 프로필일 때만 API 엔드포인트 활성화
public @interface ReceiverController {
    String value() default "";
}