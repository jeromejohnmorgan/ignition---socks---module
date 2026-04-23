package com.kyvislabs.gsap.common;

import com.inductiveautomation.perspective.common.api.BrowserResource;

import java.util.Set;

/**
 * Module-wide constants and the browser resources (JS + CSS bundles) that Perspective
 * loads into every session that uses a component from this module.
 */
public class Components {

    public static final String MODULE_ID         = "com.kyvislabs.gsap";
    public static final String URL_ALIAS         = "kyvislabs-gsap";
    public static final String COMPONENT_CATEGORY = "Kyvis Labs";

    public static final Set<BrowserResource> BROWSER_RESOURCES = Set.of(
            new BrowserResource(
                    "gsap-perspective-js",
                    String.format("/res/%s/Components.js", URL_ALIAS),
                    BrowserResource.ResourceType.JS
            ),
            new BrowserResource(
                    "gsap-perspective-css",
                    String.format("/res/%s/Components.css", URL_ALIAS),
                    BrowserResource.ResourceType.CSS
            )
    );
}
