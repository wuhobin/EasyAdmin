package com.nexora.monitor.domain.query;

import lombok.Data;

@Data
public class ManagedServerQuery {

    private Integer ownerId;

    private String name;

    private Integer enabled;
}
