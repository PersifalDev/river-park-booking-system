package ru.haritonenko.commonlibs.dto.kafka.event.type;

public enum UserEventType {

    USER_PROFILE_REQUESTED,
    USER_PROFILE_RESPONSE,

    USER_REGISTRATION_STATUS_REQUESTED,
    USER_REGISTRATION_STATUS_RESPONSE,

    USER_AUTHORIZED,
    USER_UNAUTHORIZED
}