package org.ura.workflow.engine.exception;

import jakarta.ws.rs.core.Response;

/** A referenced person (actor, delegate, initiator) does not exist. */
public class PersonNotFoundException extends WorkflowException {

    public PersonNotFoundException(Long personId) {
        super("PERSON_NOT_FOUND", Response.Status.NOT_FOUND,
                "No person found with id " + personId);
    }
}
