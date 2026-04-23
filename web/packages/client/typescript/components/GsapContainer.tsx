import * as React from 'react';
import { gsap } from 'gsap';
import {
    AbstractUIElementStore,
    ComponentMeta,
    ComponentProps,
    ComponentStoreDelegate,
    makeLogger,
    PComponent,
    PropertyTree,
    SizeObject
} from '@inductiveautomation/perspective-client';

export const COMPONENT_TYPE = 'kyvislabs.display.gsapcontainer';

const logger = makeLogger(COMPONENT_TYPE);

// ── Types mirroring the JSON property schema ──────────────────────────────────

export interface GsapTimelineStep {
    target?:    string;
    type:       'to' | 'from' | 'fromTo';
    duration:   number;
    ease:       string;
    delay?:     number;
    vars:       Record<string, unknown>;
    fromVars?:  Record<string, unknown>;
    position?:  gsap.Position;
}

export interface GsapAnimation {
    type:       'to' | 'from' | 'fromTo' | 'timeline';
    target:     string;
    duration:   number;
    ease:       string;
    delay:      number;
    repeat:     number;
    yoyo:       boolean;
    vars:       Record<string, unknown>;
    fromVars:   Record<string, unknown>;
    steps:      GsapTimelineStep[];
}

export interface GsapContainerProps {
    trigger:    boolean | number | string;
    animation:  GsapAnimation;
    content:    string;
    autoplay:   boolean;
    style?:     React.CSSProperties;
}

// ── Gateway event names (must match GsapContainerModelDelegate.java) ──────────

enum MessageEvents {
    PLAY_EVENT     = 'gsap-play',
    COMPLETE_EVENT = 'gsap-animation-complete',
    START_EVENT    = 'gsap-animation-start'
}

// ── Gateway delegate ──────────────────────────────────────────────────────────

/**
 * Handles messages arriving from the Gateway-scope GsapContainerModelDelegate.
 * The delegate's play() Python method sends a 'gsap-play' event which is received
 * here and forwarded to the live component instance.
 */
export class GsapContainerGatewayDelegate extends ComponentStoreDelegate {
    private componentInstance: GsapContainer | null = null;

    constructor(componentStore: AbstractUIElementStore) {
        super(componentStore);
    }

    /** Called by GsapContainer to register itself so events can be forwarded. */
    registerComponent(instance: GsapContainer): void {
        this.componentInstance = instance;
    }

    /** Receives events pushed from the Java ModelDelegate (e.g. play()). */
    handleEvent(eventName: string, eventObject: { [key: string]: unknown }): void {
        logger.debug(`handleEvent: ${eventName}`);

        if (eventName === MessageEvents.PLAY_EVENT) {
            if (this.componentInstance) {
                this.componentInstance.buildAndPlay();
            }
        }
    }

    /** Fires a Perspective component event up to the Gateway / script handlers. */
    notifyAnimationComplete(triggerValue: unknown): void {
        this.fireEvent(MessageEvents.COMPLETE_EVENT, {
            timestamp:    Date.now(),
            triggerValue: triggerValue
        });
    }

    notifyAnimationStart(): void {
        this.fireEvent(MessageEvents.START_EVENT, {});
    }
}

// ── React component ───────────────────────────────────────────────────────────

interface GsapContainerState {
    // intentionally empty — all animation state lives in GSAP, not React
}

/**
 * GsapContainer — a Perspective component that renders arbitrary HTML content
 * and animates it using GSAP (GreenSock Animation Platform).
 *
 * Reactivity bridge:
 *   Ignition tags → Perspective writes tag values to the 'trigger' prop
 *   → componentDidUpdate detects the change
 *   → buildAndPlay() constructs a new GSAP Timeline from 'animation' props
 *   → Timeline runs imperatively against real DOM nodes inside stageRef
 *   → onComplete fires a Perspective component event (animationCompleteHandler)
 *
 * The 'content' prop holds raw HTML which is injected via dangerouslySetInnerHTML.
 * This gives GSAP unrestricted access to the DOM subtree via querySelectorAll,
 * which is necessary because GSAP operates imperatively on real DOM nodes —
 * something React's virtual DOM intentionally abstracts away.
 *
 * IMPORTANT: 'content' HTML is authored by the Perspective designer (trusted
 * environment), not by end-users.  Sanitise it if that assumption ever changes.
 */
export class GsapContainer extends PComponent<ComponentProps<GsapContainerProps>, GsapContainerState> {

    private readonly stageRef = React.createRef<HTMLDivElement>();
    private activeTimeline: gsap.core.Timeline | null = null;

    constructor(props: ComponentProps<GsapContainerProps>) {
        super(props);
        // Register this instance with the delegate so gateway play() calls
        // can reach the component's buildAndPlay() method.
        const delegate = props.store.delegate as GsapContainerGatewayDelegate;
        if (delegate && typeof delegate.registerComponent === 'function') {
            delegate.registerComponent(this);
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    componentDidMount(): void {
        if (this.props.props.autoplay) {
            this.buildAndPlay();
        }
    }

    componentDidUpdate(prevProps: ComponentProps<GsapContainerProps>): void {
        const next = this.props.props;
        const prev = prevProps.props;

        // Core reactivity: any change to trigger replays the animation.
        if (next.trigger !== prev.trigger) {
            this.buildAndPlay();
            return;
        }

        // If the animation config or HTML content changes, kill the stale
        // timeline so the next trigger fires with the updated configuration.
        const animChanged    = JSON.stringify(next.animation) !== JSON.stringify(prev.animation);
        const contentChanged = next.content !== prev.content;
        if (animChanged || contentChanged) {
            this.killTimeline();
        }
    }

    componentWillUnmount(): void {
        this.killTimeline();
    }

    // ── GSAP helpers ──────────────────────────────────────────────────────────

    private killTimeline(): void {
        if (this.activeTimeline) {
            this.activeTimeline.kill();
            this.activeTimeline = null;
        }
    }

    /**
     * Constructs a paused GSAP Timeline from the current 'animation' prop.
     * Returns null if the stage ref is not yet attached or no animation config
     * is present.
     */
    private buildTimeline(): gsap.core.Timeline | null {
        const stage = this.stageRef.current;
        if (!stage) {
            logger.warn('buildTimeline: stage ref not yet attached.');
            return null;
        }

        const { animation } = this.props.props;
        if (!animation) {
            logger.warn('buildTimeline: no animation configuration provided.');
            return null;
        }

        this.killTimeline();

        const defaultSelector = animation.target || '.gsap-target';

        const tl = gsap.timeline({
            paused:     true,
            onStart:    () => this.onAnimationStart(),
            onComplete: () => this.onAnimationComplete()
        });

        if (animation.type === 'timeline' && Array.isArray(animation.steps) && animation.steps.length > 0) {
            // ── Multi-step timeline ──────────────────────────────────────────
            for (const step of animation.steps) {
                const selector = step.target || defaultSelector;
                const targets  = stage.querySelectorAll(selector);

                if (!targets.length) {
                    logger.warn(`buildTimeline: step target "${selector}" matched no elements.`);
                    continue;
                }

                const stepVars: gsap.TweenVars = {
                    duration: step.duration ?? 0.5,
                    ease:     step.ease     || 'power2.out',
                    ...(step.vars || {})
                };

                if (step.type === 'fromTo') {
                    tl.fromTo(targets, step.fromVars || {}, stepVars, step.position);
                } else if (step.type === 'from') {
                    tl.from(targets, stepVars, step.position);
                } else {
                    tl.to(targets, stepVars, step.position);
                }
            }
        } else {
            // ── Single tween ─────────────────────────────────────────────────
            const targets = stage.querySelectorAll(defaultSelector);

            if (!targets.length) {
                logger.warn(`buildTimeline: target "${defaultSelector}" matched no elements.`);
                return null;
            }

            const tweenVars: gsap.TweenVars = {
                duration: animation.duration ?? 1,
                ease:     animation.ease     || 'power2.out',
                delay:    animation.delay    ?? 0,
                repeat:   animation.repeat   ?? 0,
                yoyo:     animation.yoyo     ?? false,
                ...(animation.vars || {})
            };

            if (animation.type === 'fromTo') {
                tl.fromTo(targets, animation.fromVars || {}, tweenVars);
            } else if (animation.type === 'from') {
                tl.from(targets, tweenVars);
            } else {
                // default: 'to'
                tl.to(targets, tweenVars);
            }
        }

        this.activeTimeline = tl;
        return tl;
    }

    // ── Animation event callbacks ─────────────────────────────────────────────

    private onAnimationStart(): void {
        const delegate = this.props.store.delegate as GsapContainerGatewayDelegate;
        if (delegate && typeof delegate.notifyAnimationStart === 'function') {
            delegate.notifyAnimationStart();
        }
    }

    private onAnimationComplete(): void {
        const delegate = this.props.store.delegate as GsapContainerGatewayDelegate;
        if (delegate && typeof delegate.notifyAnimationComplete === 'function') {
            delegate.notifyAnimationComplete(this.props.props.trigger);
        }
    }

    // ── Public API (called by delegate and lifecycle) ────────────────────────

    buildAndPlay(): void {
        const tl = this.buildTimeline();
        if (tl) {
            tl.restart();
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    render() {
        const { props, emit } = this.props;

        return (
            // emit() injects Perspective-required attributes (data-component, style
            // from the 'style' prop, event handlers for Designer selection, etc.)
            <div {...emit({ classes: ['gsap-container'] })} style={props.style}>
                {/*
                  * The stage div owns the animatable DOM subtree.
                  * dangerouslySetInnerHTML is intentional: GSAP must operate on real
                  * DOM nodes, which only exist after the raw HTML is parsed by the
                  * browser.  React-rendered children would force us to use refs for
                  * every animatable element, defeating the purpose of the flexible
                  * 'target' selector pattern.
                  */}
                <div
                    ref={this.stageRef}
                    className="gsap-stage"
                    style={{ width: '100%', height: '100%', position: 'relative' }}
                    dangerouslySetInnerHTML={{ __html: props.content || '' }}
                />
            </div>
        );
    }
}

// ── ComponentMeta ─────────────────────────────────────────────────────────────

/**
 * Provides metadata Perspective needs to instantiate and manage the component:
 * the type ID, default size, props reducer, view component class, and delegate
 * factory.
 */
export class GsapContainerMeta implements ComponentMeta {
    getComponentType(): string {
        return COMPONENT_TYPE;
    }

    getDefaultSize(): SizeObject {
        return { width: 400, height: 300 };
    }

    createDelegate(component: AbstractUIElementStore): ComponentStoreDelegate {
        return new GsapContainerGatewayDelegate(component);
    }

    /**
     * Maps the raw Perspective PropertyTree (backed by the component's JSON props)
     * to the typed GsapContainerProps object the React component receives.
     */
    getPropsReducer(tree: PropertyTree): GsapContainerProps {
        return {
            trigger:   tree.read('trigger', false),
            animation: tree.read('animation', {
                type:     'to',
                target:   '.gsap-target',
                duration: 1,
                ease:     'power2.out',
                delay:    0,
                repeat:   0,
                yoyo:     false,
                vars:     { opacity: 1, y: 0 },
                fromVars: {},
                steps:    []
            }),
            content:   tree.readString('content', '<div class="gsap-target"></div>'),
            autoplay:  tree.readBoolean('autoplay', false),
            style:     tree.read('style', {})
        };
    }

    getViewComponent(): PComponent<any> {
        return GsapContainer as any;
    }
}
