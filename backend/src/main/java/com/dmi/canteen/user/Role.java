package com.dmi.canteen.user;

public enum Role {
    ROLE_STUDENT,
    ROLE_STAFF,
    ROLE_MANAGER;

    public String getCleanName() {
        switch (this) {
            case ROLE_STUDENT:
                return "Student";
            case ROLE_STAFF:
                return "Staff";
            case ROLE_MANAGER:
                return "Manager";
            default:
                return name();
        }
    }
}
