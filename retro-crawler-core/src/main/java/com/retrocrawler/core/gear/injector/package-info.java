/**
 * Reflective support for injecting resolved attributes into selected Gear.
 *
 * <p>
 * These types implement the framework-owned construction stage: declared facts
 * and identifiers are injected first, followed by any configured catch-all
 * attributes. Collection models configure this behavior through Gear
 * annotations and descriptors rather than invoking individual injectors.
 */
package com.retrocrawler.core.gear.injector;
