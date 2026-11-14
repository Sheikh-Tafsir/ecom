package com.example.ecom.common.utils;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class UuidV7Generator implements IdentifierGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        if (object != null && session != null) {
            try {
                Object existingId = session.getEntityPersister(null, object).getIdentifier(object, session);
                if (existingId != null) {
                    return existingId;
                }
            } catch (Exception ignored) {
                // Ignore and proceed to generate
            }
        }
        return UuidUtils.generateUuidV7();
    }
}
