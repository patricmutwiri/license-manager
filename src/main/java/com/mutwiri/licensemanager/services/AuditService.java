/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.services;

import com.mutwiri.licensemanager.entities.AuditEvent;
import com.mutwiri.licensemanager.repository.AuditEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuditService {
    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public AuditEvent record(String eventType, String actor, String resourceType, String resourceId,
            String description, Map<String, String> metadata) {
        AuditEvent event = new AuditEvent();
        event.setEventType(eventType);
        event.setActor(actor == null || actor.isBlank() ? "system" : actor);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setDescription(description);
        event.setMetadata(metadata == null ? Map.of() : metadata);
        return auditEventRepository.save(event);
    }

    public List<AuditEvent> recent() {
        return auditEventRepository.findTop100ByOrderByCreatedAtDesc();
    }
}

