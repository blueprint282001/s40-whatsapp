package tools;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateAssets {

    public static void main(String[] args) throws Exception {
        File resDir = new File("client-j2me/res");
        if (!resDir.exists()) resDir.mkdirs();

        generateDoodleBg(new File(resDir, "doodle_bg.png"));
        generateDefaultAvatar(new File(resDir, "avatar_default.png"));
        generateTickSingle(new File(resDir, "tick_single.png"));
        generateTickDouble(new File(resDir, "tick_double.png"));
        generateTickBlue(new File(resDir, "tick_blue.png"));
        generateHeaderIcon(new File(resDir, "icon_header.png"));
        generateCameraIcon(new File(resDir, "icon_camera.png"));
        generateMicIcon(new File(resDir, "icon_mic.png"));
        generateGroupAvatar(new File(resDir, "avatar_group.png"));
        generateAppIcon(new File(resDir, "icon.png"), 48);

        System.out.println("All authentic WhatsApp assets generated successfully!");
    }

    // Official WhatsApp Application Icon (48x48 for Nokia S40 Grid)
    private static void generateAppIcon(File file, int size) throws Exception {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Subtle shadow / glow
        g.setColor(new Color(0, 0, 0, 40));
        g.fillOval(3, 4, size - 6, size - 6);

        // WhatsApp Gradient Green Bubble
        GradientPaint gp = new GradientPaint(
            size / 2, 2, new Color(0x25, 0xD3, 0x66),
            size / 2, size - 2, new Color(0x12, 0x8C, 0x7E)
        );
        g.setPaint(gp);
        g.fillOval(2, 2, size - 5, size - 5);

        // Speech bubble tail pointing bottom-left
        int[] tx = { 5, 18, 12 };
        int[] ty = { size - 4, size - 12, size - 4 };
        g.fillPolygon(tx, ty, 3);

        // White border ring around bubble
        g.setColor(new Color(255, 255, 255, 80));
        g.setStroke(new BasicStroke(1.2f));
        g.drawOval(2, 2, size - 5, size - 5);

        // Pure White WhatsApp Telephone Receiver Handset
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Curved handset handle
        g.drawArc(14, 14, 20, 20, -45, 115);

        // Earpiece (top right)
        g.fillOval(25, 12, 8, 8);
        // Mouthpiece (bottom left)
        g.fillOval(12, 25, 8, 8);

        // Subtle gloss reflection at top
        g.setColor(new Color(255, 255, 255, 90));
        g.fillArc(6, 4, size - 12, 14, 0, 180);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // Authentic WhatsApp Group Avatar (32x32)
    private static void generateGroupAvatar(File file) throws Exception {
        int size = 32;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Circular background in #00A884 (WhatsApp group teal background)
        g.setColor(new Color(0x12, 0x8C, 0x7E));
        g.fillOval(0, 0, size, size);

        // Silhouette color #FFFFFF
        g.setColor(Color.WHITE);

        // Center figure
        g.fillOval(12, 5, 8, 8);
        g.fillArc(8, 14, 16, 14, 0, 180);

        // Left figure
        g.setColor(new Color(0xCC, 0xEE, 0xE9));
        g.fillOval(5, 8, 6, 6);
        g.fillArc(2, 16, 12, 12, 0, 180);

        // Right figure
        g.fillOval(21, 8, 6, 6);
        g.fillArc(18, 16, 12, 12, 0, 180);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // 1. WhatsApp Classic Doodle Background (120x120 seamless tile)
    private static void generateDoodleBg(File file) throws Exception {
        int w = 120, h = 120;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Beige base
        g.setColor(new Color(0xEC, 0xE5, 0xDD));
        g.fillRect(0, 0, w, h);

        // Faint doodle drawings in #DFD6CD
        g.setColor(new Color(0xDF, 0xD6, 0xCD));
        g.setStroke(new BasicStroke(1.2f));

        // Coffee cup at (15, 15)
        g.drawRoundRect(15, 18, 12, 10, 3, 3);
        g.drawArc(24, 19, 6, 6, -90, 180); // handle
        g.drawLine(17, 15, 17, 17); // steam
        g.drawLine(21, 14, 21, 17);

        // Musical Note at (75, 15)
        g.fillOval(75, 22, 5, 4);
        g.drawLine(79, 14, 79, 23);
        g.drawLine(79, 14, 85, 12);
        g.fillOval(83, 20, 5, 4);
        g.drawLine(87, 12, 87, 21);

        // Speech Bubble at (45, 50)
        g.drawRoundRect(45, 48, 18, 12, 5, 5);
        g.drawLine(48, 60, 45, 65);
        g.drawLine(45, 65, 52, 60);

        // Star at (95, 55)
        drawStar(g, 98, 58, 5, 2.5);

        // Alarm clock at (20, 85)
        g.drawOval(18, 85, 14, 14);
        g.drawLine(25, 92, 25, 88); // hand
        g.drawLine(25, 92, 28, 92);
        g.drawLine(18, 85, 16, 83); // bell
        g.drawLine(32, 85, 34, 83);

        // Heart at (80, 90)
        drawHeart(g, 82, 90, 10);

        // Cloud at (50, 100)
        g.drawArc(48, 98, 8, 8, 90, 180);
        g.drawArc(53, 94, 10, 10, 45, 180);
        g.drawArc(60, 98, 8, 8, -90, 180);
        g.drawLine(50, 106, 66, 106);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    private static void drawStar(Graphics2D g, double cx, double cy, double rOut, double rIn) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? rOut : rIn;
            double angle = i * Math.PI / 5 - Math.PI / 2;
            double x = cx + r * Math.cos(angle);
            double y = cy + r * Math.sin(angle);
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        p.closePath();
        g.draw(p);
    }

    private static void drawHeart(Graphics2D g, int x, int y, int size) {
        g.drawArc(x, y, size / 2, size / 2, 0, 180);
        g.drawArc(x + size / 2, y, size / 2, size / 2, 0, 180);
        g.drawLine(x, y + size / 4, x + size / 2, y + size);
        g.drawLine(x + size, y + size / 4, x + size / 2, y + size);
    }

    // 2. Authentic WhatsApp Contact Silhouette Avatar (32x32)
    private static void generateDefaultAvatar(File file) throws Exception {
        int size = 32;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Circular background in #DFE5E7
        g.setColor(new Color(0xDF, 0xE5, 0xE7));
        g.fillOval(0, 0, size, size);

        // Silhouette color #A6B2B7
        g.setColor(new Color(0xA6, 0xB2, 0xB7));

        // Head
        g.fillOval(10, 6, 12, 12);

        // Shoulders
        g.fillArc(4, 18, 24, 20, 0, 180);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // 3. Single Gray Tick (12x10)
    private static void generateTickSingle(File file) throws Exception {
        BufferedImage img = new BufferedImage(12, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(0x86, 0x96, 0xA0));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g.drawLine(2, 5, 5, 8);
        g.drawLine(5, 8, 10, 2);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // 4. Double Gray Tick (16x10)
    private static void generateTickDouble(File file) throws Exception {
        BufferedImage img = new BufferedImage(16, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(0x86, 0x96, 0xA0));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // First tick
        g.drawLine(2, 5, 5, 8);
        g.drawLine(5, 8, 10, 2);

        // Second overlapping tick
        g.drawLine(6, 5, 9, 8);
        g.drawLine(9, 8, 14, 2);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // 5. Double Blue Tick (16x10)
    private static void generateTickBlue(File file) throws Exception {
        BufferedImage img = new BufferedImage(16, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(0x53, 0xBD, 0xEB));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g.drawLine(2, 5, 5, 8);
        g.drawLine(5, 8, 10, 2);

        g.drawLine(6, 5, 9, 8);
        g.drawLine(9, 8, 14, 2);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // 6. Header WhatsApp Logo (20x20)
    private static void generateHeaderIcon(File file) throws Exception {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Green speech bubble
        g.setColor(new Color(0x25, 0xD3, 0x66));
        g.fillOval(1, 1, 18, 18);

        // Bubble point
        int[] px = {2, 7, 5};
        int[] py = {18, 14, 18};
        g.fillPolygon(px, py, 3);

        // White telephone receiver
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(6, 6, 8, 8, -45, 110);
        g.fillOval(6, 11, 3, 3);
        g.fillOval(11, 6, 3, 3);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // 7. Camera Icon (14x12)
    private static void generateCameraIcon(File file) throws Exception {
        BufferedImage img = new BufferedImage(14, 12, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(0x86, 0x96, 0xA0));
        g.fillRoundRect(1, 3, 12, 8, 2, 2);
        g.fillRect(4, 1, 4, 2);
        g.setColor(Color.WHITE);
        g.fillOval(5, 5, 4, 4);

        g.dispose();
        ImageIO.write(img, "png", file);
    }

    // 8. Mic Icon (12x14)
    private static void generateMicIcon(File file) throws Exception {
        BufferedImage img = new BufferedImage(12, 14, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(0x86, 0x96, 0xA0));
        g.fillRoundRect(4, 1, 4, 7, 3, 3);
        g.setStroke(new BasicStroke(1.2f));
        g.drawArc(2, 4, 8, 6, 0, -180);
        g.drawLine(6, 10, 6, 13);
        g.drawLine(3, 13, 9, 13);

        g.dispose();
        ImageIO.write(img, "png", file);
    }
}
