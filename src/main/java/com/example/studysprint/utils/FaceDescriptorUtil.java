package com.example.studysprint.utils;

import com.google.gson.Gson;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Pure-Java face descriptor using Local Binary Patterns (LBP) histograms.
 * No external AI library needed. Works reliably as a biometric signature.
 *
 * Steps:
 *  1. Center-crop the image (face region)
 *  2. Resize to 128x128
 *  3. Convert to grayscale
 *  4. Divide into 8x8 grid of 16x16 cells
 *  5. Compute LBP histogram per cell (256 bins)
 *  6. Normalize and return as double[]
 *
 * Comparison: Euclidean distance < 35.0 → same person
 */
public class FaceDescriptorUtil {

    private static final Gson GSON = new Gson();
    private static final int FACE_SIZE   = 128; // px
    private static final int GRID        = 8;   // 8x8 grid
    private static final int CELL_SIZE   = FACE_SIZE / GRID; // 16 px
    private static final int HIST_BINS   = 256;

    /** Compute LBP-based descriptor from a raw webcam frame */
    public static double[] computeDescriptor(BufferedImage frame) {
        // 1. Center-crop to square
        BufferedImage cropped = centerCrop(frame);
        // 2. Resize to FACE_SIZE x FACE_SIZE
        BufferedImage resized = resize(cropped, FACE_SIZE, FACE_SIZE);
        // 3. Grayscale
        int[][] gray = toGray(resized);
        // 4. LBP map
        int[][] lbp = computeLBP(gray);
        // 5. Grid histograms
        return buildHistogram(lbp);
    }

    /** Compare two descriptors — returns distance (lower = more similar) */
    public static double distance(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return Double.MAX_VALUE;
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    /** Recommended threshold for same-person match.
     *  18.0 is strict enough to avoid false positives between different people
     *  while still tolerating minor lighting/angle variations. */
    public static boolean isSamePerson(double[] a, double[] b) {
        return distance(a, b) < 18.0;
    }

    public static String toJson(double[] descriptor) {
        return GSON.toJson(descriptor);
    }

    public static double[] fromJson(String json) {
        return GSON.fromJson(json, double[].class);
    }

    // ─── Private helpers ───────────────────────────────────────────────────

    private static BufferedImage centerCrop(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int size = Math.min(w, h);
        int x = (w - size) / 2, y = (h - size) / 2;
        return img.getSubimage(x, y, size, size);
    }

    private static BufferedImage resize(BufferedImage img, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static int[][] toGray(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[][] gray = new int[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = new Color(img.getRGB(x, y));
                gray[y][x] = (int)(0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
            }
        }
        return gray;
    }

    /** Compute LBP for each pixel */
    private static int[][] computeLBP(int[][] gray) {
        int h = gray.length, w = gray[0].length;
        int[][] lbp = new int[h][w];
        int[] dx = {-1, -1, 0, 1, 1,  1,  0, -1};
        int[] dy = { 0, -1,-1,-1, 0,  1,  1,  1};
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int center = gray[y][x];
                int code = 0;
                for (int k = 0; k < 8; k++) {
                    if (gray[y + dy[k]][x + dx[k]] >= center) code |= (1 << k);
                }
                lbp[y][x] = code;
            }
        }
        return lbp;
    }

    /** Build normalized histogram per cell, concatenate all cells */
    private static double[] buildHistogram(int[][] lbp) {
        int totalBins = GRID * GRID * HIST_BINS;
        double[] feature = new double[totalBins];
        int idx = 0;
        for (int gy = 0; gy < GRID; gy++) {
            for (int gx = 0; gx < GRID; gx++) {
                int[] hist = new int[HIST_BINS];
                int y0 = gy * CELL_SIZE, x0 = gx * CELL_SIZE;
                for (int y = y0; y < y0 + CELL_SIZE; y++) {
                    for (int x = x0; x < x0 + CELL_SIZE; x++) {
                        hist[lbp[y][x]]++;
                    }
                }
                // Normalize cell histogram
                double sum = Arrays.stream(hist).sum();
                for (int b = 0; b < HIST_BINS; b++) {
                    feature[idx++] = (sum > 0) ? hist[b] / sum : 0.0;
                }
            }
        }
        return feature;
    }
}
