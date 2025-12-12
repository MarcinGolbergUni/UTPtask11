package pj;

import java.awt.*;

public
    class PositionAndColor {

    public static int encode(int x, int y, Color color){
        int result = x & 0xFFF;
            result |= (y & 0xFFF) << 12;
            result |= (colorToByte(color) & 0xFF) << 24;
        return result;
    }

    public static int[] decode(int value) {

        int x = value & 0xFFF;                  // dolne 12 bitów
        int y = (value >> 12) & 0xFFF;          // kolejne 12 bitów
        int colorByte = (value >> 24) & 0xFF;   // górny bajt (3-3-2)

        return new int[]{ x, y, colorByte};
    }

    public static int colorToByte(Color color){
        int r = color.getRed() >> 5;   // 8 bit - 3 bity (0–7)
        int g = color.getGreen() >> 5; // 8 bit - 3 bity (0–7)
        int b = color.getBlue() >> 6;  // 8 bit - 2 bity (0–3)

        return (r << 5) | (g << 2) | b;
    }

    public static Color byteToColor(int value) {

        int r = (value >> 5) & 0b111;        // 3-bit R
        int g = (value >> 2) & 0b111;        // 3-bit G
        int b = value & 0b11;                // 2-bit B

        int red   = (r * 255) / 7;
        int green = (g * 255) / 7;
        int blue  = (b * 255) / 3;

        return new Color(red, green, blue);
    }
}
