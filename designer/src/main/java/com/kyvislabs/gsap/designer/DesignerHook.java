package com.kyvislabs.gsap.designer;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.util.LoggerEx;
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook;
import com.inductiveautomation.ignition.designer.model.DesignerContext;
import com.inductiveautomation.perspective.designer.DesignerComponentRegistry;
import com.inductiveautomation.perspective.designer.api.PerspectiveDesignerInterface;
import com.kyvislabs.gsap.common.component.display.GsapContainer;

/**
 * Designer-scope module hook.  Registers the GsapContainer descriptor with the
 * Perspective Designer component palette so the component appears and can be
 * dragged onto views.
 */
public class DesignerHook extends AbstractDesignerModuleHook {

    private static final LoggerEx logger = LoggerEx.newBuilder().build("GSAP Perspective Designer");

    private DesignerContext context;
    private DesignerComponentRegistry registry;

    static {
        // Register an i18n bundle. The properties file must exist at
        // designer/src/main/resources/gsap-components.properties
        BundleUtil.get().addBundle("gsap-perspective", DesignerHook.class.getClassLoader(), "gsap-components");
    }

    public DesignerHook() {
        logger.info("GSAP Perspective Designer module initializing.");
    }

    @Override
    public void startup(DesignerContext context, LicenseState activationState) {
        this.context = context;
        init();
    }

    private void init() {
        logger.debug("Registering GSAP components in Designer palette.");
        PerspectiveDesignerInterface pdi = PerspectiveDesignerInterface.get(context);
        registry = pdi.getDesignerComponentRegistry();
        registry.registerComponent(GsapContainer.DESCRIPTOR);
    }

    @Override
    public void shutdown() {
        logger.info("GSAP Perspective Designer module shutting down.");
        if (registry != null) {
            registry.removeComponent(GsapContainer.COMPONENT_ID);
        }
    }
}
