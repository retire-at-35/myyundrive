package com.pan.pojo.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class AdminProperties {
    @Value("${admin.email}")
    private String email;
}
