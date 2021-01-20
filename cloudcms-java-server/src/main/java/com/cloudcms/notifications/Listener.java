/**
 * Copyright (C) 2020 Gitana Software, Inc.
 */
package com.cloudcms.notifications;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import com.cloudcms.cache.CacheClearService;
import com.cloudcms.server.CloudcmsDriver;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

@Service
public class Listener {

    private final Logger log = LoggerFactory.getLogger(Listener.class);

    @Autowired
    private CloudcmsDriver driver;

    @Autowired
    CacheClearService cacheClearService;

    private String applicationId;

    @PostConstruct
    private synchronized void init() {
        this.applicationId = driver.getApplicationId();
    }

    @Autowired
    private String invalidationQueueName;

    // @JmsListener(destination = "cloudcms-dev-integration-test1")
    @JmsListener(destination = "#{@invalidationQueueName}")
    public void receive(String message) {
        log.trace("Received message {}", message);

        JsonObject invalidationMessage = JsonParser.parseString(message).getAsJsonObject();
        final String type = invalidationMessage.get("Type").getAsString();
        final String subject = invalidationMessage.get("Subject").getAsString();

        if (type != null 
            && !type.isEmpty() 
            && type.equalsIgnoreCase("Notification")
            && subject != null
            && !subject.isEmpty()
            && subject.toLowerCase().startsWith("invalidate_objects:")) {
                log.trace(invalidationMessage.get("Message").getAsString());
                final JsonObject messageBody = JsonParser.parseString(invalidationMessage.get("Message").getAsString()).getAsJsonObject();

                List<String> nodeRefs = new ArrayList<>();
                final JsonArray invalidationsArray = messageBody.get("invalidations").getAsJsonArray();
                invalidationsArray.forEach( e -> {
                    JsonObject eObject = e.getAsJsonObject();
                    
                    // filter out invalidation messages meant for other application objects
                    if (applicationId.equals(eObject.get("applicationId").getAsString())) {
                        nodeRefs.add(eObject.get("ref").getAsString());                        
                    }
                });

                if (!nodeRefs.isEmpty()) {
                    log.debug(String.format("Received invalidation message for node(s) %s", nodeRefs.toString()));
                    cacheClearService.clearCacheKeysFromNodeRefs(nodeRefs);
                }
        }
    }
}