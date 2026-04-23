package com.kyvislabs.gsap.gateway;

import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.inductiveautomation.ignition.common.script.builtin.KeywordArgs;
import com.inductiveautomation.ignition.common.script.builtin.PyArgumentMap;
import com.inductiveautomation.perspective.gateway.api.Component;
import com.inductiveautomation.perspective.gateway.api.ComponentModelDelegate;
import com.inductiveautomation.perspective.gateway.messages.EventFiredMsg;
import org.python.core.PyObject;

/**
 * Gateway-side scripting delegate for the GsapContainer component.
 *
 * Methods annotated with {@link com.inductiveautomation.ignition.common.script.builtin.ScriptCallable}
 * are exposed to Ignition's Python scripting environment and can be invoked from
 * component event scripts or tag change scripts using the Perspective component API:
 *
 * <pre>
 *     # Play the animation from a tag change event script
 *     self.getSibling("GsapContainer1").play()
 *
 *     # Set a new trigger value (also causes a replay)
 *     self.getSibling("GsapContainer1").setTrigger(event.currentValue.value)
 * </pre>
 *
 * Gateway → Client messages travel over the Perspective session websocket.
 * Client → Gateway messages arrive via {@link #handleEvent(EventFiredMsg)}.
 */
public class GsapContainerModelDelegate extends ComponentModelDelegate {

    // Event names must match the MessageEvents enum in GsapContainer.tsx
    public static final String OUTBOUND_PLAY_EVENT     = "gsap-play";
    public static final String INBOUND_COMPLETE_EVENT  = "gsap-animation-complete";
    public static final String INBOUND_START_EVENT     = "gsap-animation-start";

    public GsapContainerModelDelegate(Component component) {
        super(component);
    }

    @Override
    protected void onStartup() {
        log.debugf("GsapContainerModelDelegate started for '%s'.", component.getComponentAddressPath());
    }

    @Override
    protected void onShutdown() {
        log.debugf("GsapContainerModelDelegate shutdown for '%s'.", component.getComponentAddressPath());
    }

    // ── Script-callable API ──────────────────────────────────────────────────

    /**
     * Sends a play command to the browser component, causing the configured animation
     * to restart immediately, regardless of the current 'trigger' value.
     *
     * Usage from Python:
     * <pre>self.getSibling("GsapContainer1").play()</pre>
     */
    @com.inductiveautomation.ignition.common.script.builtin.ScriptCallable
    public void play() {
        log.debugf("play() called on '%s'.", component.getComponentAddressPath());
        JsonObject payload = new JsonObject();
        payload.addProperty("triggerValue", System.currentTimeMillis());
        fireEvent(OUTBOUND_PLAY_EVENT, payload);
    }

    /**
     * Writes a new value to the component's 'trigger' property, which causes the
     * React component to detect the prop change and replay the animation.
     * This is the idiomatic way to drive animations from tag-change scripts.
     *
     * Usage from Python:
     * <pre>self.getSibling("GsapContainer1").setTrigger(event.currentValue.value)</pre>
     *
     * @param newTriggerValue any Python-compatible value (bool, int, float, str)
     */
    @com.inductiveautomation.ignition.common.script.builtin.ScriptCallable
    @KeywordArgs(names = {"newTriggerValue"}, types = {Object.class})
    public void setTrigger(PyObject[] pyArgs, String[] keywords) {
        PyArgumentMap args = PyArgumentMap.interpretPyArgs(
                pyArgs, keywords, GsapContainerModelDelegate.class, "setTrigger");
        Object val = args.get("newTriggerValue");
        log.debugf("setTrigger(%s) called on '%s'.", val, component.getComponentAddressPath());
        component.getProps().set("trigger", val);
    }

    /**
     * Updates the entire 'animation' property object, then immediately plays the
     * new animation.  Accepts a Python dict that mirrors the animation JSON schema.
     *
     * Usage from Python:
     * <pre>
     *   self.getSibling("GsapContainer1").updateAnimation({
     *       "type": "fromTo",
     *       "target": ".gsap-target",
     *       "duration": 0.5,
     *       "ease": "bounce.out",
     *       "fromVars": {"opacity": 0, "scale": 0.5},
     *       "vars":     {"opacity": 1, "scale": 1}
     *   })
     * </pre>
     */
    @com.inductiveautomation.ignition.common.script.builtin.ScriptCallable
    @KeywordArgs(names = {"animationConfig"}, types = {Object.class})
    public void updateAnimation(PyObject[] pyArgs, String[] keywords) {
        PyArgumentMap args = PyArgumentMap.interpretPyArgs(
                pyArgs, keywords, GsapContainerModelDelegate.class, "updateAnimation");
        Object config = args.get("animationConfig");
        log.debugf("updateAnimation() called on '%s'.", component.getComponentAddressPath());
        component.getProps().set("animation", config);
        // Bump trigger to force an immediate replay after the props write.
        component.getProps().set("trigger", System.currentTimeMillis());
    }

    // ── Inbound events from client ───────────────────────────────────────────

    @Override
    public void handleEvent(EventFiredMsg message) {
        String eventName = message.getEventName();
        log.debugf("Received event '%s' from '%s'.", eventName, component.getComponentAddressPath());

        if (INBOUND_COMPLETE_EVENT.equals(eventName)) {
            // The Perspective event system already propagates this to any configured
            // animationCompleteHandler scripts in the Designer.  No extra action needed
            // here unless gateway-side processing is required.
            log.debugf("Animation completed on component '%s'.", component.getComponentAddressPath());
        }

        if (INBOUND_START_EVENT.equals(eventName)) {
            log.debugf("Animation started on component '%s'.", component.getComponentAddressPath());
        }
    }
}
