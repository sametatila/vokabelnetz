package com.vokabelnetz.entity.enums;

/**
 * User roles for authorization.
 */
public enum Role {
    ROLE_USER,  // Standard user
    ROLE_ADMIN, // Admin - can manage words
    ROLE_SUPER  // Super admin - can manage users
}
