package com.kyvislabs.gsap.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.perspective.common.api.ComponentRegistry;
import com.inductiveautomation.perspective.gateway.api.ComponentModelDelegateRegistry;
import com.inductiveautomation.perspective.gateway.api.PerspectiveContext;
import com.kyvislabs.gsap.common.Components;
import com.kyvislabs.gsap.common.component.display.GsapContainer;

import java.util.Optional;

/**
 * Gateway-scope module hook.  Registered in the root build.gradle.kts under
 * ignitionModule { hooks { ... } }.
 *
 * On startup it registers the GsapContainer component descriptor with the
 * Perspective component registry and attaches a model delegate factory so
 * Python scripts can call play() / setTrigger() on component instances.
 */
public class GatewayHook extends AbstractGatewayModuleHook {

    private static final LoggerEx log = LoggerEx.newBuilder().build("GSAP Perspective");

    private GatewayContext gatewayContext;
    private PerspectiveContext perspectiveContext;
    private ComponentRegistry componentRegistry;
    private ComponentModelDelegateRegistry modelDelegateRegistry;

    @Override
    public void setup(GatewayContext context) {
        this.gatewayContext = context;
        log.info("GSAP Perspective module: setting up.");
    }

    @Override
    public void startup(LicenseState activationState) {
        log.info("GSAP Perspective module: starting up.");

        this.perspectiveContext   = PerspectiveContext.get(this.gatewayContext);
        this.componentRegistry    = this.perspectiveContext.getComponentRegistry();
        this.modelDelegateRegistry = this.perspectiveContext.getComponentModelDelegateRegistry();

        if (this.componentRegistry != null) {
            log.info("Registering GsapContainer component.");
            this.componentRegistry.registerComponent(GsapContainer.DESCRIPTOR);
        } else {
            log.error("Component registry unavailable — GsapContainer will not function!");
        }

        if (this.modelDelegateRegistry != null) {
            log.info("Registering GsapContainer model delegate.");
            this.modelDelegateRegistry.register(GsapContainer.COMPONENT_ID, GsapContainerModelDelegate::new);
        }
    }

    @Override
    public void shutdown() {
        log.info("GSAP Perspective module: shutting down.");

        if (this.componentRegistry != null) {
            this.componentRegistry.removeComponent(GsapContainer.COMPONENT_ID);
        }

        if (this.modelDelegateRegistry != null) {
            this.modelDelegateRegistry.remove(GsapContainer.COMPONENT_ID);
        }
    }

    // ── Resource mounting ────────────────────────────────────────────────────

    /**
     * The folder name inside the web jar that contains the compiled JS/CSS bundles.
     * These are copied there by the Webpack AfterBuildPlugin.
     */
    @Override
    public Optional<String> getMountedResourceFolder() {
        return Optional.of("mounted");
    }

    /**
     * URL path segment under which the resources are served:
     * /res/kyvislabs-gsap/Components.js
     */
    @Override
    public Optional<String> getMountPathAlias() {
        return Optional.of(Components.URL_ALIAS);
    }

    @Override
    public boolean isFreeModule() {
        return true;
    }

    @Override
    public boolean isMakerEditionCompatible() {
        return true;
    }
}
