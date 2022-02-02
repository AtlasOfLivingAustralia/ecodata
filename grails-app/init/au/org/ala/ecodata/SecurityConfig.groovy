package au.org.ala.ecodata

import au.org.ala.ws.security.AlaWebServiceAuthFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientRegistrationRepositoryConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler
import org.springframework.security.web.authentication.logout.LogoutFilter

import javax.inject.Inject

@Configuration
@EnableWebSecurity
@Import([OAuth2ClientRegistrationRepositoryConfiguration.class]) // Needed because we disabled the autoconfiguration
@Order(1)
class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value('${spring.security.logoutUrl:"http://dev.ala.org.au:8080/logout"}')
    String logoutUrl

    @Inject
    AlaWebServiceAuthFilter alaWebServiceAuthFilter

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.addFilterBefore(alaWebServiceAuthFilter, LogoutFilter)
        http.authorizeRequests()
                .antMatchers(
                        "/",
                        "/public/**",
                        "/css/**",
                        "/assets/**",
                        "/messages/**",
                        "/i18n/**",
                        "/static/**",
                        "/images/**",
                        "/js/**",
                        "/ws/**"
                ).permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .oauth2Login()
                .successHandler(new SavedRequestAwareAuthenticationSuccessHandler())
                .userInfoEndpoint()
                .and()
                .and()
                .logout()
                .logoutUrl(logoutUrl)
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID").permitAll()
                .and().csrf().disable()

    }
}