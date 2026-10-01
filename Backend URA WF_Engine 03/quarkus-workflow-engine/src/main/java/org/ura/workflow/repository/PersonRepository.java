package org.ura.workflow.repository;

import java.util.Optional;

import org.ura.workflow.entity.Person;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PersonRepository implements PanacheRepository<Person> {

    public Optional<Person> findByStaffNumber(String staffNumber) {
        return find("staffNumber", staffNumber).firstResultOptional();
    }
}