package com.wse.qanaryexplanationservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Lazy
public class ClientService {

    private final Logger logger = LoggerFactory.getLogger(ClientService.class);

    public ClientService() {
    }
}
