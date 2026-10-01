/**
 * Domain rules: the runtime engine itself. Resolves the next assignee
 * from the organisation model and the active workflow definition,
 * advances/returns/completes instances, and emits audit events.
 * Framework-agnostic — no JAX-RS or persistence types here.
 */
package org.ura.workflow.engine;
