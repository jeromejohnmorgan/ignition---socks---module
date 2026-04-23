package com.kyvislabs.gsap.common.component.display;

import com.inductiveautomation.ignition.common.gson.JsonParser;
import com.inductiveautomation.ignition.common.jsonschema.JsonSchema;
import com.inductiveautomation.perspective.common.api.ComponentDescriptor;
import com.inductiveautomation.perspective.common.api.ComponentDescriptorImpl;
import com.inductiveautomation.perspective.common.api.ComponentEventDescriptor;
import com.kyvislabs.gsap.common.Components;

import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Describes the GsapContainer component to the Ignition component registry so the
 * Gateway and Designer both know about the corresponding React front-end component.
 *
 * Component type ID must exactly match the COMPONENT_TYPE constant in GsapContainer.tsx.
 */
public class GsapContainer {

    public static final String COMPONENT_ID = "kyvislabs.display.gsapcontainer";

    // ── Property schema ──────────────────────────────────────────────────────
    public static final JsonSchema SCHEMA =
            JsonSchema.parse(Components.class.getResourceAsStream("/gsapcontainer.props.json"));

    // ── Component events surfaced to Perspective scripting ───────────────────

    /** Fires when the GSAP animation completes (including all repeats). */
    public static final ComponentEventDescriptor ANIMATION_COMPLETE_HANDLER =
            new ComponentEventDescriptor(
                    "animationCompleteHandler",
                    "Fires when the GSAP animation finishes playing.",
                    JsonSchema.parse(Components.class.getResourceAsStream("/gsap.event.complete.props.json"))
            );

    /** Fires when the GSAP animation starts playing. */
    public static final ComponentEventDescriptor ANIMATION_START_HANDLER =
            new ComponentEventDescriptor(
                    "animationStartHandler",
                    "Fires when the GSAP animation begins playing.",
                    JsonSchema.parse(Components.class.getResourceAsStream("/gsap.event.empty.props.json"))
            );

    // ── ComponentDescriptor ──────────────────────────────────────────────────

    public static final ComponentDescriptor DESCRIPTOR =
            ComponentDescriptorImpl.ComponentBuilder.newBuilder()
                    .setPaletteCategory(Components.COMPONENT_CATEGORY)
                    .setId(COMPONENT_ID)
                    .setModuleId(Components.MODULE_ID)
                    .setSchema(SCHEMA)
                    .setEvents(Arrays.asList(ANIMATION_COMPLETE_HANDLER, ANIMATION_START_HANDLER))
                    .setName("GSAP Container")
                    .addPaletteEntry(
                            "",
                            "GSAP Container",
                            "Animate any HTML content using GSAP, driven by Ignition tag values.",
                            null,
                            null
                    )
                    .addPaletteEntry(
                            "gsap-fade-in",
                            "GSAP Fade In",
                            "A pre-configured fade-in animation triggered by a tag change.",
                            null,
                            new JsonParser().parse(
                                    new InputStreamReader(
                                            Components.class.getResourceAsStream("/variants/gsapcontainer.fadein.props.json")
                                    )
                            ).getAsJsonObject()
                    )
                    .addPaletteEntry(
                            "gsap-slide-in",
                            "GSAP Slide In",
                            "A pre-configured slide-in-from-left animation.",
                            null,
                            new JsonParser().parse(
                                    new InputStreamReader(
                                            Components.class.getResourceAsStream("/variants/gsapcontainer.slidein.props.json")
                                    )
                            ).getAsJsonObject()
                    )
                    .addPaletteEntry(
                            "gsap-timeline",
                            "GSAP Timeline",
                            "A multi-step timeline animation example.",
                            null,
                            new JsonParser().parse(
                                    new InputStreamReader(
                                            Components.class.getResourceAsStream("/variants/gsapcontainer.timeline.props.json")
                                    )
                            ).getAsJsonObject()
                    )
                    .setDefaultMetaName("gsapContainer")
                    .setResources(Components.BROWSER_RESOURCES)
                    .build();
}
