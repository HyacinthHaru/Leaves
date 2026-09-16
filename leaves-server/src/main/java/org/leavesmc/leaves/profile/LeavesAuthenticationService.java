package org.leavesmc.leaves.profile;

import com.destroystokyo.paper.profile.PaperServicesDiscoveryService;
import com.mojang.authlib.Environment;
import com.mojang.authlib.minecraft.SessionService;
import com.mojang.authlib.services.response.discovery.DiscoveryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.util.function.Supplier;

public class LeavesAuthenticationService extends PaperServicesDiscoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeavesAuthenticationService.class);

    public LeavesAuthenticationService(Proxy proxy, boolean servicesKeySetEnabled, Supplier<DiscoveryResponse> discoverySupplier) {
        super(proxy, servicesKeySetEnabled, discoverySupplier);
    }

    public static LeavesAuthenticationService create(Proxy proxy) {
        final Environment environment = determineEnvironment();
        LOGGER.info("Environment: {}", environment);
        return new LeavesAuthenticationService(proxy, true, createDiscoverySupplier(proxy, environment));
    }

    @Override
    public SessionService createMinecraftSessionService() {
        return new LeavesMinecraftSessionService(this.getServicesKeySet(), this.getProxy(), this);
    }
}
