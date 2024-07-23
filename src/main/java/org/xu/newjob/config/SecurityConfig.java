package org.xu.newjob.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.xu.newjob.entity.LoginTicket;
import org.xu.newjob.entity.User;
import org.xu.newjob.service.UserService;
import org.xu.newjob.util.CookieUtil;
import org.xu.newjob.util.HostHolder;
import org.xu.newjob.util.NewJobConstant;
import org.xu.newjob.util.NewJobUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

@Configuration
public class SecurityConfig implements NewJobConstant {

    @Autowired
    private UserService userService;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/resources/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                // 从cookie中获取凭证
                String ticket = CookieUtil.getValue(request, "ticket");

                if (ticket != null) {
                    LoginTicket loginTicket = userService.findLoginTicket(ticket);
                    // 检查凭证
                    if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                        // 根据凭证查询用户
                        User user = userService.findUserById(loginTicket.getUserId());

                        // 这个不能在这个过滤器中做，（我认为这个过滤器在请求返回的时候也会执行，所以会导致退出时用户信息无法删除）
//                        hostHolder.setUser(user);

                        // 构建用户认证的结果，存入 SpringSecurityContext 中，便于 Security 进行授权
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                user, user.getPassword(), userService.getAuthorities(user.getId()));
                        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
                    }
                }
                filterChain.doFilter(request, response);
            }
        }, UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests((auth) -> auth.requestMatchers(
                "/user/setting",
                "/user/upload",
                "/discuss/add",
                "/comment/add/**",
                "/letter/**",
                "/notice/**",
                "/like",
                "/follow",
                "/unfollow").hasAnyAuthority(
                AUTHORITY_USER,
                AUTHORITY_ADMIN,
                AUTHORITY_MODERATOR).anyRequest().permitAll());
//        http.csrf(AbstractHttpConfigurer::disable);
        // 权限不够时的处理
        http.exceptionHandling((e) -> e.authenticationEntryPoint(new AuthenticationEntryPoint() {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                String xRequestedWith = request.getHeader("x-requested-with");
                if ("XMLHttpRequest".equals(xRequestedWith)) {
                    response.setContentType("application/plain;charset=utf-8");
                    PrintWriter writer = response.getWriter();
                    writer.write(NewJobUtil.getJSONString(403, "您还没有登录！"));
                } else {
                    response.sendRedirect(request.getContextPath() + "/login");
                }
            }
        }).accessDeniedHandler(new AccessDeniedHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                String xRequestedWith = request.getHeader("x-requested-with");
                if ("XMLHttpRequest".equals(xRequestedWith)) {
                    response.setContentType("application/plain;charset=utf-8");
                    PrintWriter writer = response.getWriter();
                    writer.write(NewJobUtil.getJSONString(403, "您没有访问此功能的权限！"));
                } else {
                    response.sendRedirect(request.getContextPath() + "/denied");
                }
            }
        }));

        // Security 默认会拦截 /logout 请求，进行退出处理，
        // 覆盖默认的拦截，使用原来的退出逻辑
        http.logout(l -> l.logoutUrl("/securityLogout"));

        // 不配置，就会走自己的认证逻辑

        return http.build();
    }


}
