package com.school.teaching.config;

import com.school.teaching.mapper.UserMapper;
import com.school.teaching.security.JwtAuthenticationFilter;
import com.school.teaching.security.PasswordChangeEnforcementFilter;
import com.school.teaching.sse.SseTicketStore;
import com.school.teaching.utils.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final CorsConfigurationSource corsConfigurationSource;
    private final UserMapper userMapper;
    private final SseTicketStore ticketStore;

    public SecurityConfig(JwtUtils jwtUtils, CorsConfigurationSource corsConfigurationSource,
                          UserMapper userMapper, SseTicketStore ticketStore) {
        this.jwtUtils = jwtUtils;
        this.corsConfigurationSource = corsConfigurationSource;
        this.userMapper = userMapper;
        this.ticketStore = ticketStore;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtils, ticketStore);
    }

    @Bean
    public PasswordChangeEnforcementFilter passwordChangeEnforcementFilter() {
        return new PasswordChangeEnforcementFilter(userMapper);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(passwordChangeEnforcementFilter(), JwtAuthenticationFilter.class)

            // 安全响应头（纵深防御，nginx 层已有但应用层也加）
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(frame -> frame.deny())
            )

            .authorizeHttpRequests(auth -> auth
                // 放行 ASYNC 派发 — SSE 流结束后容器内部重派发不触发安全拦截
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ASYNC).permitAll()

                // ============================================================
                // [公开] 无需认证 — Swagger + 健康检查 + OPTIONS
                // ============================================================
                .requestMatchers(
                    "/doc.html", "/swagger-resources/**", "/webjars/**",
                    "/v3/**", "/swagger-ui/**", "/swagger-ui.html"
                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers("/auth/actions/login").permitAll()
                .requestMatchers("/auth/actions/external-login").permitAll()
                .requestMatchers("/access/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/agent/ppt/download").permitAll()

                // ============================================================
                // [公开·只读] 公开GET端点 — 题库/知识节点/积分排行/字典/Logo
                // ============================================================
                .requestMatchers(HttpMethod.GET, "/question-bank/list").permitAll()
                .requestMatchers(HttpMethod.GET, "/question-bank/{id:[0-9]+}").permitAll()
                .requestMatchers(HttpMethod.GET, "/question-bank/actions/subjects").permitAll()
                .requestMatchers(HttpMethod.GET, "/question-bank/actions/template/download").permitAll()
                .requestMatchers(HttpMethod.GET, "/question-bank/actions/excel-template/download").permitAll()
                .requestMatchers(HttpMethod.GET, "/knowledge-node/actions/template/download").permitAll()
                .requestMatchers(HttpMethod.GET, "/knowledge-node/actions/zip-template/download").permitAll()
                .requestMatchers(HttpMethod.GET, "/knowledge-node/actions/txt-template/download").permitAll()
                .requestMatchers(HttpMethod.GET, "/knowledge-node/actions/docx-template/download").permitAll()
                .requestMatchers(HttpMethod.GET, "/credit/actions/ranking").permitAll()
                .requestMatchers(HttpMethod.GET, "/credit/actions/moral-ranking").permitAll()
                .requestMatchers(HttpMethod.GET, "/credit/shop").permitAll()
                .requestMatchers(HttpMethod.GET, "/credit/titles").permitAll()
                .requestMatchers(HttpMethod.GET, "/bbs/categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/settings/grades").permitAll()
                .requestMatchers(HttpMethod.GET, "/settings/subjects").permitAll()
                .requestMatchers(HttpMethod.GET, "/settings/features").permitAll()
                .requestMatchers(HttpMethod.GET, "/student/actions/template/download").permitAll()
                .requestMatchers(HttpMethod.GET, "/knowledge-node/list").permitAll()
                .requestMatchers(HttpMethod.GET, "/knowledge-node/tree").permitAll()
                .requestMatchers(HttpMethod.GET, "/showcase/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/dictionary/subjects").permitAll()
                .requestMatchers(HttpMethod.GET, "/dictionary/grades").permitAll()
                .requestMatchers(HttpMethod.GET, "/dictionary/question-types").permitAll()
                .requestMatchers(HttpMethod.GET, "/dictionary/terms").permitAll()
                .requestMatchers(HttpMethod.GET, "/dictionary/wuyu-tags").permitAll()
                .requestMatchers(HttpMethod.GET, "/system/logo").permitAll()

                // ============================================================
                // [管理员专用] Actuator非健康端点
                // ============================================================
                .requestMatchers("/actuator/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                // ============================================================
                // [家长] 家长端点
                // ============================================================
                .requestMatchers("/parent/**")
                    .hasAnyRole("PARENT", "ADMIN", "SUPER_ADMIN")

                // ============================================================
                // [教师专属GET] 必须在学生块之前! — 字典我的学科
                // ============================================================
                .requestMatchers(HttpMethod.GET, "/dictionary/actions/my-subjects")
                    .hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN", "INSPECTOR", "REGION_ADMIN")

                // ============================================================
                // [教师+] 学业预警 · 分组管理 · 闯关管理端 · 教师端巡视/教研/课题/备课/质量分析 · 快捷评语
                // ============================================================
                .requestMatchers("/alert/**")
                    .hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN", "INSPECTOR", "REGION_ADMIN")
                .requestMatchers(HttpMethod.GET, "/groups/**").hasAnyRole("STUDENT", "TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/groups/**").hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/groups/**").hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/checkpoint/admin/**")
                    .hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/teacher/inspection/**")
                    .hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/teacher/research/**")
                    .hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/research/**")
                    .hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/teacher/lesson-prep/**")
                    .hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/teacher/comparison/**")
                    .hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.GET, "/teacher/quick-comments").hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/teacher/quick-comments").hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/teacher/quick-comments/**").hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/teaching-group/actions/my-groups")
                    .hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")

                // ============================================================
                // [学生+] 全角色可访问 — 核心教学功能
                // ============================================================
                .requestMatchers("/checkpoint/**")
                    .hasAnyRole("STUDENT", "TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")
                .requestMatchers(
                    "/auth/**",
                    "/task/*",
                    "/task/*/questions",
                    "/student/actions/my-class",
                    "/student/tasks/**",
                    "/student/timeline",
                    "/student/typing-history",
                    "/credit/**",
                    "/bbs/**",
                    "/notification/**",
                    "/profile/**",
                    "/wrong/**",
                    "/precision/**",
                    "/class-album/**",
                    "/upload/**",
                    "/showcase/**",
                    "/practice/**",
                    "/classroom/**",
                    "/peer-reviews/pending",
                    "/peer-reviews/*/actions/submit",
                    "/peer-reviews/submissions/*",
                    "/re-reviews/actions/request",
                    "/re-reviews/actions/my-requests",
                    "/class/actions/my",
                    "/dictionary/**",
                    "/knowledge-base/**"
                ).hasAnyRole("STUDENT", "TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN", "INSPECTOR", "PARENT", "REGION_ADMIN")

                // ============================================================
                // [教师+] 教师及以上 — 教学管理功能
                // ============================================================
                .requestMatchers(
                    "/class/**",
                    "/exam-share/**",
                    "/paper-import/**",
                    "/question-bank/**",
                    "/knowledge-node/**",
                    "/ai-output/**",
                    "/task/**",
                    "/student-tasks/**",
                    "/peer-reviews/**",
                    "/re-reviews/**",
                    "/external-reviews/**",
                    "/templates/**",
                    "/task-templates/**",
                    "/ai/questions/**",
                    "/ai/grading/**",
                    "/credit/admin/**",
                    "/student/**",
                    "/dashboard/**",
                    "/report/**",
                    "/class-type-config/list"
                ).hasAnyRole("TEACHER", "HEAD_TEACHER", "ADMIN", "SUPER_ADMIN")

                // ============================================================
                // [特殊] 家校消息 — 教师/家长/管理员
                // ============================================================
                .requestMatchers("/messages/**")
                    .hasAnyRole("TEACHER", "HEAD_TEACHER", "PARENT", "ADMIN", "SUPER_ADMIN")

                // ============================================================
                // [巡视员+] 巡视管理 · 教师行为日志 · 巡视面板
                // ============================================================
                .requestMatchers("/inspector/manage/**")
                    .hasAnyRole("INSPECTOR", "ADMIN", "SUPER_ADMIN")
                .requestMatchers("/teacher/activity/**")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN", "INSPECTOR")
                .requestMatchers("/inspector/**")
                    .hasAnyRole("INSPECTOR", "ADMIN", "SUPER_ADMIN", "REGION_ADMIN")

                // ============================================================
                // [区域管理员+]
                // ============================================================
                .requestMatchers("/region/**")
                    .hasAnyRole("REGION_ADMIN", "ADMIN", "SUPER_ADMIN")

                // ============================================================
                // [管理员+] 系统设置 · 教师管理 · 分组管理 · 系统监控
                // ============================================================
                .requestMatchers("/settings/**", "/teacher/**", "/stage-config/**", "/class-type-config/**",
                    "/teaching-group/**", "/lesson-prep-group/**")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/system/**")
                    .hasAnyRole("ADMIN", "SUPER_ADMIN")

                // ============================================================
                // [超级管理员] 系统维护
                // ============================================================
                .requestMatchers("/system-maintenance/**")
                    .hasRole("SUPER_ADMIN")

                .anyRequest().authenticated()
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write(
                        "{\"code\":401,\"message\":\"未登录或Token无效\",\"data\":null,\"timestamp\":" +
                        System.currentTimeMillis() + "}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=utf-8");
                    response.getWriter().write(
                        "{\"code\":403,\"message\":\"权限不足\",\"data\":null,\"timestamp\":" +
                        System.currentTimeMillis() + "}");
                })
            );

        return http.build();
    }
}
