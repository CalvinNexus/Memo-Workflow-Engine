package org.ura.workflow.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "persons")
public class Person extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String staffNumber;

    public String firstName;

    public String lastName;

    public String email;

    @ManyToOne
    @JoinColumn(name = "department_id")
    public Department department; // Fixed type to Department

    @ManyToOne
    @JoinColumn(name = "rank_id")
    public Rank rank;

    @ManyToOne
    @JoinColumn(name = "reports_to")
    public Person reportsTo;

    public Boolean active;
}