package com.sap.cloud.security.autoconfig;

import com.sap.cloud.security.config.IdentityServiceConfiguration;
import com.sap.cloud.security.config.XsuaaServiceConfiguration;
import com.sap.cloud.security.config.XsuaaServiceConfigurations;
import com.sap.cloud.security.token.authentication.JwtDecoderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET;


/**
 * {@link EnableAutoConfiguration} exposes a
 * {@link JwtDecoder}, which has the standard Spring Security Jwt validators as
 * well as the SCP identity provider-specific validators.
 *
 * Activates when there is a bean of type {@link Jwt} configured in the context.
 *
 * <p>
 * Can be disabled
 * with {@code @EnableAutoConfiguration(exclude={HybridIdentityServicesAutoConfiguration.class})}
 * or with property {@code sap.spring.security.hybrid.auto = false}.
 */
@Configuration
@ConditionalOnClass(Jwt.class)
@ConditionalOnProperty(name = "sap.spring.security.hybrid.auto", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({XsuaaServiceConfiguration.class, IdentityServiceConfiguration.class, XsuaaServiceConfigurations.class})
@AutoConfigureBefore(OAuth2ResourceServerAutoConfiguration.class) // imports OAuth2ResourceServerJwtConfiguration which specifies JwtDecoder
class HybridIdentityServicesAutoConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(HybridIdentityServicesAutoConfiguration.class);

    @Configuration
    @ConditionalOnMissingBean({JwtDecoder.class})
    @ConditionalOnWebApplication(type = SERVLET)
    static class JwtDecoderConfigurations {
        JwtDecoderConfigurations() {
        }

        @Bean
        @ConditionalOnProperty("sap.security.services.xsuaa.uaadomain")
        public JwtDecoder hybridJwtDecoder(XsuaaServiceConfiguration xsuaaConfig, IdentityServiceConfiguration identityConfig) {
            LOGGER.debug("auto-configures HybridJwtDecoder.");
            return new JwtDecoderBuilder()
                    .withIasServiceConfiguration(identityConfig)
                    .withXsuaaServiceConfiguration(xsuaaConfig)
                    .buildHybrid();
        }

        @Bean
        @ConditionalOnProperty("sap.security.services.xsuaa[0].uaadomain")
        public JwtDecoder hybridJwtDecoderMultiXsuaaServices(XsuaaServiceConfigurations xsuaaConfigs, IdentityServiceConfiguration identityConfig) {
            LOGGER.debug("auto-configures HybridJwtDecoder when bound to multiple xsuaa service instances.");
            return new JwtDecoderBuilder()
                    .withIasServiceConfiguration(identityConfig)
                    .withXsuaaServiceConfigurations(xsuaaConfigs.getConfigurations())
                    .buildHybrid();
        }
    }


}
