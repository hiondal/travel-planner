package com.travelplanner.common.domain;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    protected String id;

    protected BaseEntity() {
    }

    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            this.id = generateId(getPrefix());
        }
    }

    public String getId() {
        return id;
    }

    protected String getPrefix() {
        return "id";
    }

    protected static String generateId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
