package ru.haritonenko.telegrambot.service;

import java.io.ByteArrayInputStream;

record CachedPhoto(byte[] bytes, String fileName) {
    ByteArrayInputStream bytesInputStream() {
        return new ByteArrayInputStream(bytes);
    }
}
