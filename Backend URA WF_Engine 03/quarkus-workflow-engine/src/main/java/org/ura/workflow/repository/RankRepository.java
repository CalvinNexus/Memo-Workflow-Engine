package org.ura.workflow.repository;

import java.util.Optional;

import org.ura.workflow.entity.Rank;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RankRepository implements PanacheRepository<Rank> {

    public Optional<Rank> findByName(String name) {
        return find("name", name).firstResultOptional();
    }
}