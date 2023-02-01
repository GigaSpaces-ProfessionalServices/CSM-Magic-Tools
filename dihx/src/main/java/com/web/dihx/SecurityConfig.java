package com.web.dihx;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

@Configuration
@EnableWebSecurity
public class SecurityConfig  extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        /*http
                .authorizeRequests()
                    .anyRequest().permitAll()
                .and()

                    .oauth2Login();*/
        //http.authorizeRequests().antMatchers("/builder/**").authenticated().and().httpBasic().and().csrf().disable().oauth2Login();
        http.csrf()
                .disable()
                .httpBasic()
                .disable()
                .authorizeRequests()
                .antMatchers("/auth")
                .permitAll()
                .antMatchers("/customError")
                .permitAll()
                .antMatchers("/access-denied")
                .permitAll()
                .antMatchers("/secured")
                .permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .oauth2Login();
    }

}