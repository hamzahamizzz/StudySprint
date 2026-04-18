package com.example.studysprint.utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WebcamManager {
    private static Webcam webcam = null;
    private static ScheduledExecutorService executor = null;
    private static boolean isInitializing = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(WebcamManager::stopCapture));
    }

    public static synchronized void startCapture(Consumer<java.awt.image.BufferedImage> onFrameCaptured) {
        if (isInitializing) return;
        isInitializing = true;

        Thread t = new Thread(() -> {
            try {
                System.out.println("WebcamManager: Initializing...");
                for (Webcam w : Webcam.getWebcams()) {
                    if (w.isOpen()) w.close();
                }
                
                stopCapture();
                Thread.sleep(1000); 

                webcam = Webcam.getDefault();
                if (webcam != null) {
                    webcam.setViewSize(WebcamResolution.QVGA.getSize());
                    
                    int attempts = 0;
                    boolean opened = false;
                    while (attempts < 5 && !opened) {
                        try {
                            System.out.println("WebcamManager: Attempting to open (Try " + (attempts+1) + ")...");
                            opened = webcam.open(); // Using blocking open for reliability
                        } catch (Exception e) {
                            attempts++;
                            Thread.sleep(1500);
                        }
                    }

                    if (opened) {
                        System.out.println("WebcamManager: Webcam OPENED successfully.");
                        executor = Executors.newSingleThreadScheduledExecutor(r -> {
                            Thread execThread = new Thread(r);
                            execThread.setDaemon(true);
                            return execThread;
                        });
                        executor.scheduleAtFixedRate(() -> {
                            try {
                                if (webcam != null && webcam.isOpen()) {
                                    java.awt.image.BufferedImage img = webcam.getImage();
                                    if (img != null) {
                                        onFrameCaptured.accept(img);
                                    }
                                }
                            } catch (Exception e) {}
                        }, 500, 300, TimeUnit.MILLISECONDS);
                    } else {
                        System.err.println("WebcamManager: FAILED to open after retries.");
                    }
                } else {
                    System.err.println("WebcamManager: Default webcam not found.");
                }
            } catch (Exception e) {
                System.err.println("WebcamManager Error: " + e.getMessage());
            } finally {
                isInitializing = false;
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public static synchronized void stopCapture() {
        System.out.println("WebcamManager: Stopping capture...");
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (webcam != null) {
            try {
                if (webcam.isOpen()) {
                    webcam.close();
                }
            } catch (Exception e) {}
            webcam = null;
        }
    }
}
