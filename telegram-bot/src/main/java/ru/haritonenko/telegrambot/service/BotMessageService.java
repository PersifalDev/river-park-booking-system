package ru.haritonenko.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotMessageService {

    private static final int TELEGRAM_PHOTO_LIMIT_BYTES = 10 * 1024 * 1024;
    private static final int SAFE_PHOTO_LIMIT_BYTES = 9 * 1024 * 1024;
    private static final int MAX_IMAGE_SIDE = 1800;

    private final TelegramClient telegramClient;
    private final Map<String, CachedPhoto> photoCache = new ConcurrentHashMap<>();

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
        try {
            CachedPhoto cachedPhoto = getPhoto(photoUrl);
            return telegramClient.execute(SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(new ByteArrayInputStream(cachedPhoto.bytes()), cachedPhoto.fileName()))
                    .caption(caption)
                    .replyMarkup(keyboard)
                    .build());
        } catch (Exception e) {
            log.error("Failed to send photo chatId={}, photoUrl={}", chatId, photoUrl, e);
            return sendText(chatId, caption, keyboard);
        }
    }

    public boolean editPhoto(Long chatId, Integer messageId, String photoUrl, String caption, InlineKeyboardMarkup keyboard) {
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
            log.error("Failed to edit photo chatId={}, messageId={}, photoUrl={}", chatId, messageId, photoUrl, e);
            return false;
        } catch (Exception e) {
            log.error("Failed to prepare photo edit chatId={}, messageId={}, photoUrl={}", chatId, messageId, photoUrl, e);
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
            if (!isMessageNotModified(e)) {
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
        CachedPhoto cachedPhoto = photoCache.get(photoUrl);
        if (cachedPhoto != null) {
            return cachedPhoto;
        }

        CachedPhoto loadedPhoto = loadPhoto(photoUrl);
        photoCache.put(photoUrl, loadedPhoto);
        return loadedPhoto;
    }

    private CachedPhoto loadPhoto(String photoUrl) throws IOException {
        try (InputStream inputStream = URI.create(photoUrl).toURL().openStream()) {
            byte[] originalBytes = inputStream.readAllBytes();
            String fileName = resolveFileName(photoUrl);

            if (originalBytes.length <= SAFE_PHOTO_LIMIT_BYTES) {
                return new CachedPhoto(originalBytes, fileName);
            }

            byte[] optimizedBytes = optimizeImage(originalBytes);
            return new CachedPhoto(optimizedBytes, toJpgFileName(fileName));
        }
    }

    private byte[] optimizeImage(byte[] bytes) throws IOException {
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
        if (optimizedBytes.length > TELEGRAM_PHOTO_LIMIT_BYTES) {
            throw new IOException("Optimized image is still too large");
        }

        return optimizedBytes;
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

    private record CachedPhoto(byte[] bytes, String fileName) {
        private ByteArrayInputStream bytesInputStream() {
            return new ByteArrayInputStream(bytes);
        }
    }
}