package com.gigaspaces.connector.utils;

import com.gigaspaces.connector.config.Consts;
import org.openspaces.config.DefaultServiceConfig;
import org.openspaces.core.config.annotation.SpaceProxyBeansConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
//@Profile(Consts.CONNECTOR_MODE)
@Import({DefaultServiceConfig.class, SpaceProxyBeansConfig.class})
public class Beans {
}
