package ru.haritonenko.telegrambot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.haritonenko.telegrambot.config.PhotoDeliveryProperties;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class BotMessageService {

    private static final int TELEGRAM_PHOTO_LIMIT_BYTES = 10 * 1024 * 1024;
    private static final int SAFE_PHOTO_LIMIT_BYTES = 9 * 1024 * 1024;
    private static final int MAX_IMAGE_SIDE = 1280;
    private static final String TELEGRAM_FILE_ID_KEY_PREFIX = "telegram-photo-file-id:";

    private final TelegramClient telegramClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final PhotoDeliveryProperties photoProperties;
    private final Map<String, CachedPhoto> photoCache;
    private final HttpClient photoHttpClient;

    public BotMessageService(
            TelegramClient telegramClient,
            StringRedisTemplate stringRedisTemplate,
            PhotoDeliveryProperties photoProperties
    ) {
        this.telegramClient = telegramClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.photoProperties = photoProperties;
        this.photoCache = newBoundedPhotoCache();
        this.photoHttpClient = HttpClient.newBuilder()
                .connectTimeout(photoProperties.connectTimeout())
                .build();
    }

    public Message sendText(Long chatId, String text) {
        return sendText(chatId, text, (ReplyKeyboard) null);
    }

    public Message sendText(Long chatId, String text, ReplyKeyboard keyboard) {
        try {
            return telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .replyMarkup(keyboard)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send text message chatId={}", chatId, e);
            return null;
        }
    }

    public Message sendText(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        try {
            return telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .replyMarkup(keyboard)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send text message chatId={}", chatId, e);
            return null;
        }
    }

    public void editText(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text)
                    .replyMarkup(keyboard)
                    .build());
        } catch (TelegramApiException e) {
            if (!isMessageNotModified(e)) {
                log.error("Failed to edit message chatId={}, messageId={}", chatId, messageId, e);
            }
        }
    }

    public Message sendPhoto(Long chatId, String photoUrl, String caption, InlineKeyboardMarkup keyboard) {
        String cachedFileId = getCachedTelegramFileId(photoUrl);
        if (cachedFileId != null) {
            Message message = sendPhotoByFileId(chatId, cachedFileId, caption, keyboard);
            if (message != null) {
                return message;
            }
            evictCachedTelegramFileId(photoUrl);
        }

        try {
            CachedPhoto cachedPhoto = getPhoto(photoUrl);
            Message message = telegramClient.execute(SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(new ByteArrayInputStream(cachedPhoto.bytes()), cachedPhoto.fileName()))
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build());
            cacheTelegramFileId(photoUrl, message);
            return message;
        } catch (Exception e) {
            logTelegramPhotoFailure("send", chatId, null, photoUrl, e);
            return sendText(chatId, caption, keyboard);
        }
    }

    public boolean editPhoto(Long chatId, Integer messageId, String photoUrl, String caption, InlineKeyboardMarkup keyboard) {
        String cachedFileId = getCachedTelegramFileId(photoUrl);
        if (cachedFileId != null && editPhotoByFileId(chatId, messageId, cachedFileId, caption, keyboard)) {
            return true;
        }

        try {
            CachedPhoto cachedPhoto = getPhoto(photoUrl);

            InputMediaPhoto media = InputMediaPhoto.builder()
                    .media(cachedPhoto.bytesInputStream(), cachedPhoto.fileName())
                    .caption(caption)
                    .build();

            telegramClient.execute(EditMessageMedia.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .media(media)
                    .replyMarkup(keyboard)
                    .build());

            return true;
        } catch (TelegramApiException e) {
            if (isMessageNotModified(e)) {
                editCaption(chatId, messageId, caption, keyboard);
                return true;
            }
            logTelegramPhotoFailure("edit", chatId, messageId, photoUrl, e);
            return false;
        } catch (Exception e) {
            logTelegramPhotoFailure("prepare edit", chatId, messageId, photoUrl, e);
            return false;
        }
    }

    public void editCaption(Long chatId, Integer messageId, String caption, InlineKeyboardMarkup keyboard) {
        try {
            telegramClient.execute(EditMessageCaption.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build());
        } catch (TelegramApiException e) {
            if (isMessageNotFound(e)) {
                log.warn("Skip caption edit because Telegram message was not found. chatId={}, messageId={}", chatId, messageId);
            } else if (!isMessageNotModified(e)) {
                log.error("Failed to edit caption chatId={}, messageId={}", chatId, messageId, e);
            }
        }
    }

    public void deleteMessage(Long chatId, Integer messageId) {
        if (chatId == null || messageId == null) {
            log.warn("Skip delete message because chatId or messageId is null. chatId={}, messageId={}", chatId, messageId);
            return;
        }

        try {
            telegramClient.execute(DeleteMessage.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .build());
            log.info("Message deleted chatId={}, messageId={}", chatId, messageId);
        } catch (TelegramApiException e) {
            if (isMessageNotFound(e)) {
                log.warn("Skip delete because Telegram message was not found. chatId={}, messageId={}", chatId, messageId);
                return;
            }
            log.error("Failed to delete message chatId={}, messageId={}", chatId, messageId, e);
        }
    }

    public void answerCallback(String callbackQueryId, String text) {
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            if (isCallbackExpired(e)) {
                log.warn("Skip callback answer because callback is expired or invalid. callbackQueryId={}", callbackQueryId);
                return;
            }
            log.error("Failed to answer callback callbackQueryId={}", callbackQueryId, e);
        }
    }

    public void sendDocument(Long chatId, String fileName, byte[] content) {
        if (content == null || content.length == 0) {
            sendText(chatId, "Не удалось загрузить PDF-файл.");
            return;
        }

        try {
            telegramClient.execute(SendDocument.builder()
                    .chatId(chatId)
                    .document(new InputFile(new ByteArrayInputStream(content), fileName))
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send document chatId={}, fileName={}", chatId, fileName, e);
        }
    }

    private CachedPhoto getPhoto(String photoUrl) throws IOException {
        synchronized (photoCache) {
            CachedPhoto cachedPhoto = photoCache.get(photoUrl);
            if (cachedPhoto != null) {
                return cachedPhoto;
            }
        }

        CachedPhoto loadedPhoto = loadPhoto(photoUrl);
        synchronized (photoCache) {
            photoCache.put(photoUrl, loadedPhoto);
        }
        return loadedPhoto;
    }

    private CachedPhoto loadPhoto(String photoUrl) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(photoUrl))
                    .timeout(photoProperties.requestTimeout())
                    .GET()
                    .build();
            HttpResponse<byte[]> response = photoHttpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Photo request failed with status=" + response.statusCode());
            }
            byte[] originalBytes = response.body();
            String fileName = resolveFileName(photoUrl);

            byte[] optimizedBytes = optimizeImageIfPossible(originalBytes);
            if (optimizedBytes.length <= SAFE_PHOTO_LIMIT_BYTES) {
                String optimizedFileName = optimizedBytes == originalBytes ? fileName : toJpgFileName(fileName);
                return new CachedPhoto(optimizedBytes, optimizedFileName);
            }

            throw new IOException("Photo is too large after optimization");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Photo loading interrupted", exception);
        }
    }

    private Message sendPhotoByFileId(Long chatId, String fileId, String caption, InlineKeyboardMarkup keyboard) {
        try {
            return telegramClient.execute(SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(fileId))
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build());
        } catch (TelegramApiException exception) {
            logTelegramPhotoFailure("file_id send", chatId, null, null, exception);
            return null;
        }
    }

    private boolean editPhotoByFileId(Long chatId, Integer messageId, String fileId, String caption, InlineKeyboardMarkup keyboard) {
        try {
            InputMediaPhoto media = InputMediaPhoto.builder()
                    .media(fileId)
                    .caption(caption)
                    .build();

            telegramClient.execute(EditMessageMedia.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .media(media)
                    .replyMarkup(keyboard)
                    .build());

            return true;
        } catch (TelegramApiException exception) {
            if (isMessageNotModified(exception)) {
                editCaption(chatId, messageId, caption, keyboard);
                return true;
            }
            if (isMessageNotFound(exception)) {
                log.warn("Skip photo edit because Telegram message was not found. chatId={}, messageId={}", chatId, messageId);
                return true;
            }
            logTelegramPhotoFailure("file_id edit", chatId, messageId, null, exception);
            return false;
        }
    }

    private String getCachedTelegramFileId(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            return null;
        }
        try {
            return stringRedisTemplate.opsForValue().get(telegramFileIdKey(photoUrl));
        } catch (RedisConnectionFailureException exception) {
            log.warn("Redis unavailable during telegram file_id read", exception);
            return null;
        }
    }

    private void cacheTelegramFileId(String photoUrl, Message message) {
        String fileId = extractLargestPhotoFileId(message);
        if (photoUrl == null || photoUrl.isBlank() || fileId == null || fileId.isBlank()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(telegramFileIdKey(photoUrl), fileId, photoProperties.telegramFileIdTtl());
        } catch (RedisConnectionFailureException exception) {
            log.warn("Redis unavailable during telegram file_id write", exception);
        }
    }

    private void evictCachedTelegramFileId(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            return;
        }
        try {
            stringRedisTemplate.delete(telegramFileIdKey(photoUrl));
        } catch (RedisConnectionFailureException exception) {
            log.warn("Redis unavailable during telegram file_id eviction", exception);
        }
    }

    private String extractLargestPhotoFileId(Message message) {
        if (message == null || message.getPhoto() == null || message.getPhoto().isEmpty()) {
            return null;
        }
        List<org.telegram.telegrambots.meta.api.objects.photo.PhotoSize> photos = message.getPhoto();
        return photos.get(photos.size() - 1).getFileId();
    }

    private String telegramFileIdKey(String photoUrl) {
        return TELEGRAM_FILE_ID_KEY_PREFIX + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(photoUrl.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, CachedPhoto> newBoundedPhotoCache() {
        return new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedPhoto> eldest) {
                return size() > photoProperties.byteCacheMaxEntries();
            }
        };
    }

    private byte[] optimizeImageIfPossible(byte[] bytes) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
        if (source == null) {
            if (bytes.length > TELEGRAM_PHOTO_LIMIT_BYTES) {
                throw new IOException("Image is too large and cannot be optimized");
            }
            return bytes;
        }

        BufferedImage resizedImage = resize(source);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("JPEG writer not found");
        }

        ImageWriter writer = writers.next();
        try (MemoryCacheImageOutputStream imageOutputStream = new MemoryCacheImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(0.82f);
            }
            writer.write(null, new IIOImage(resizedImage, null, null), writeParam);
        } finally {
            writer.dispose();
        }

        byte[] optimizedBytes = outputStream.toByteArray();
        return optimizedBytes.length < bytes.length ? optimizedBytes : bytes;
    }

    private BufferedImage resize(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int maxSide = Math.max(width, height);

        if (maxSide <= MAX_IMAGE_SIDE) {
            BufferedImage convertedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = convertedImage.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, null);
            graphics.dispose();
            return convertedImage;
        }

        double scale = (double) MAX_IMAGE_SIDE / maxSide;
        int newWidth = Math.max(1, (int) Math.round(width * scale));
        int newHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, newWidth, newHeight, null);
        graphics.dispose();

        return resizedImage;
    }

    private String resolveFileName(String photoUrl) {
        String path = URI.create(photoUrl).getPath();
        if (path == null || path.isBlank()) {
            return "room-photo.jpg";
        }

        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex < 0 || lastSlashIndex + 1 >= path.length()) {
            return "room-photo.jpg";
        }

        return path.substring(lastSlashIndex + 1);
    }

    private String toJpgFileName(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0) {
            return fileName + ".jpg";
        }

        return fileName.substring(0, extensionIndex) + ".jpg";
    }

    private boolean isMessageNotModified(TelegramApiException exception) {
        return exception.getMessage() != null
                && exception.getMessage().toLowerCase(Locale.ROOT).contains("message is not modified");
    }

    private boolean isMessageNotFound(TelegramApiException exception) {
        if (exception.getMessage() == null) {
            return false;
        }
        String message = exception.getMessage().toLowerCase(Locale.ROOT);
        return message.contains("message to edit not found")
                || message.contains("message to delete not found")
                || message.contains("message not found");
    }

    private boolean isCallbackExpired(TelegramApiException exception) {
        if (exception.getMessage() == null) {
            return false;
        }
        String message = exception.getMessage().toLowerCase(Locale.ROOT);
        return message.contains("query is too old")
                || message.contains("query id is invalid")
                || message.contains("response timeout expired");
    }

    private void logTelegramPhotoFailure(String action, Long chatId, Integer messageId, String photoUrl, Exception exception) {
        if (isTimeout(exception)) {
            log.warn("Telegram photo {} timed out. chatId={}, messageId={}, photoUrl={}", action, chatId, messageId, photoUrl);
            return;
        }
        if (exception instanceof TelegramApiException telegramException && isMessageNotFound(telegramException)) {
            log.warn("Telegram photo {} skipped because message was not found. chatId={}, messageId={}, photoUrl={}",
                    action, chatId, messageId, photoUrl);
            return;
        }
        log.error("Telegram photo {} failed. chatId={}, messageId={}, photoUrl={}", action, chatId, messageId, photoUrl, exception);
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
