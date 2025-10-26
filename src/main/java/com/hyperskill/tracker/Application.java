package com.hyperskill.tracker;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, unique = true)
    private String apiKey;

    @ManyToOne(optional = false)
    @JoinColumn(name = "developer_id", nullable = false)
    private Developer developer;

    protected Application() {
        // JPA requirement
    }

    public Application(String name, String description, String apiKey, Developer developer) {
        this.name = name;
        this.description = description;
        this.apiKey = apiKey;
        this.developer = developer;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getApiKey() {
        return apiKey;
    }

    public Developer getDeveloper() {
        return developer;
    }
}
