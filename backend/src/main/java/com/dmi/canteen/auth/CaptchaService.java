package com.dmi.canteen.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    // 100% Unambiguous, clear uppercase characters and digits (no confusing 0/O, 1/I/L, 2/Z, 5/S, 6/G, 8/B, U/V)
    private static final String CHARS = "ACDEFHJKMNPRTWXY3479";
    private static final int WIDTH = 180;
    private static final int HEIGHT = 52;
    private static final int EXPIRY_MINUTES = 5;

    private final Map<String, CaptchaRecord> captchaStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public static class CaptchaRecord {
        private final String code;
        private final LocalDateTime expiresAt;

        public CaptchaRecord(String code, LocalDateTime expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        public String getCode() {
            return code;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }
    }

    public static class CaptchaResponse {
        private String captchaId;
        private String imageBase64;

        public CaptchaResponse(String captchaId, String imageBase64) {
            this.captchaId = captchaId;
            this.imageBase64 = imageBase64;
        }

        public String getCaptchaId() {
            return captchaId;
        }

        public String getImageBase64() {
            return imageBase64;
        }

        public String getCaptchaImage() {
            return imageBase64;
        }
    }

    public CaptchaResponse generateCaptcha() {
        return generateCaptcha(null);
    }

    public CaptchaResponse generateCaptcha(String oldCaptchaId) {
        cleanExpiredCaptchas();

        // Invalidate old captcha on refresh so it cannot be reused
        if (oldCaptchaId != null && !oldCaptchaId.trim().isEmpty()) {
            captchaStore.remove(oldCaptchaId.trim());
            log.debug("Invalidated previous Captcha id={} on refresh", oldCaptchaId.trim());
        }

        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        String captchaCode = sb.toString();
        String captchaId = UUID.randomUUID().toString();

        captchaStore.put(captchaId, new CaptchaRecord(captchaCode, LocalDateTime.now().plusMinutes(EXPIRY_MINUTES)));
        log.debug("Generated new Captcha id={}", captchaId);

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Rendering hints for ultra-crisp, anti-aliased, razor-sharp text
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        // Crisp, clean light background
        g.setColor(new Color(248, 250, 252));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Neat subtle border
        g.setColor(new Color(226, 232, 240));
        g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

        // Standard, crystal-clear bold font
        Font font = new Font("Arial", Font.BOLD, 32);
        g.setFont(font);

        FontMetrics fm = g.getFontMetrics();
        int totalWidth = 0;
        int spacing = 10;
        for (char c : captchaCode.toCharArray()) {
            totalWidth += fm.charWidth(c) + spacing;
        }
        totalWidth -= spacing;
        int curX = Math.max(10, (WIDTH - totalWidth) / 2);

        Color[] colors = new Color[]{
                new Color(15, 23, 42),   // Slate 900
                new Color(30, 58, 138),  // Blue 900
                new Color(23, 37, 84),   // Blue 950
                new Color(51, 65, 85)    // Slate 700
        };

        // Draw each character straight, bold, high-contrast with generous spacing
        for (int i = 0; i < captchaCode.length(); i++) {
            char c = captchaCode.charAt(i);
            g.setColor(colors[i % colors.length]);
            g.drawString(String.valueOf(c), curX, 38);
            curX += fm.charWidth(c) + spacing;
        }

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            throw new RuntimeException("Error rendering captcha image", e);
        }

        String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        return new CaptchaResponse(captchaId, base64);
    }

    public boolean validateCaptcha(String captchaId, String inputCode) {
        if (captchaId == null || inputCode == null || inputCode.trim().isEmpty()) {
            log.warn("Captcha rejected: missing id or code (id={})", captchaId);
            return false;
        }

        CaptchaRecord record = captchaStore.get(captchaId.trim());
        if (record == null) {
            log.warn("Captcha rejected: session not found or already invalidated for id={}", captchaId);
            return false;
        }

        if (LocalDateTime.now().isAfter(record.getExpiresAt())) {
            captchaStore.remove(captchaId.trim());
            log.warn("Captcha rejected: expired for id={}", captchaId);
            return false;
        }

        boolean matches = isCodeMatch(record.getCode(), inputCode);
        log.info("Captcha verification for id={}: result={}", captchaId, matches ? "SUCCESS" : "FAILURE");

        if (matches) {
            // Invalidate on successful use to prevent replay attacks
            captchaStore.remove(captchaId.trim());
        }

        return matches;
    }

    public static boolean isCodeMatch(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        // Normalize both values: remove all whitespace and perform case-insensitive comparison
        String exp = expected.replaceAll("\\s+", "").trim();
        String act = actual.replaceAll("\\s+", "").trim();
        if (exp.isEmpty() || act.isEmpty()) {
            return false;
        }
        return exp.equalsIgnoreCase(act);
    }

    public String getStoredCode(String captchaId) {
        if (captchaId == null) return null;
        CaptchaRecord record = captchaStore.get(captchaId.trim());
        return record != null ? record.getCode() : null;
    }

    private void cleanExpiredCaptchas() {
        LocalDateTime now = LocalDateTime.now();
        captchaStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue().getExpiresAt()));
    }
}
