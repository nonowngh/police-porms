package mb.fw.atb.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.security.enabled", havingValue = "true", matchIfMissing = false)
    public SecurityFilterChain securityFilterChainBasicAuth(HttpSecurity http) throws Exception {
        log.info("SecurityConfig.securityFilterChainBasicAuth");
        http.csrf().disable()  // CSRF 보호 비활성화
                .cors()  // CORS 활성화
                .and()
                .httpBasic()  // HTTP Basic 인증 활성화
                .and()
                .authorizeRequests()
                .antMatchers("/atb-api/**", "/na-api/**").authenticated()  // 해당 경로만 인증 필요
                .anyRequest().denyAll();  // 다른 모든 요청은 차단

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.security.enabled", havingValue = "false", matchIfMissing = false)
    public SecurityFilterChain securityFilterChainOpen(HttpSecurity http) throws Exception {
        log.info("SecurityConfig.securityFilterChainOpen");
        http.csrf().disable()
                .cors()
                .and()
                .authorizeRequests()
                .antMatchers("/atb-api/**", "/na-api/**").permitAll() // 해당 경로만 접근 허용
                .anyRequest().denyAll(); //다른 모든 요청은 차단

        return http.build();
    }


    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");  // 모든 Origin 허용
        config.addAllowedHeader("*");  // 모든 헤더 허용
        config.addAllowedMethod("*");  // 모든 HTTP 메서드 허용
        source.registerCorsConfiguration("/**", config);  // 모든 경로에 CORS 설정 적용
        return new CorsFilter(source);
    }

}