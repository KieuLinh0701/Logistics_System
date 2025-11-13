package com.logistics.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.service.ServiceTypeService;

@RestController
@RequestMapping("/api/service-types")
public class ServiceTypeController {

    @Autowired
    private ServiceTypeService service;
}