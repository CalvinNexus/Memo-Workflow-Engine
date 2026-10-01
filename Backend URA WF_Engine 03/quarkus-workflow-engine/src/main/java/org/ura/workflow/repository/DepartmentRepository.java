package org.ura.workflow.repository;

import org.ura.workflow.entity.Department;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DepartmentRepository implements PanacheRepository<Department> {
}