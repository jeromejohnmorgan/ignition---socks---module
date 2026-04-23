import { ComponentMeta, ComponentRegistry } from '@inductiveautomation/perspective-client';
import { GsapContainer, GsapContainerMeta } from './components/GsapContainer';

export { GsapContainer };

import '../scss/main';

// Register every component meta with the Perspective client registry.
// Each entry here must have a matching ComponentDescriptor registered on the
// Java side (GsapContainer.java) with an identical component type ID.
const components: Array<ComponentMeta> = [
    new GsapContainerMeta()
];

components.forEach((c: ComponentMeta) => ComponentRegistry.register(c));
