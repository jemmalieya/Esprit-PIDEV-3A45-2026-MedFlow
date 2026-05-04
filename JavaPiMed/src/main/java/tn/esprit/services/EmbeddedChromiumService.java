package tn.esprit.services;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EmbeddedChromiumService {

    private static final Object INIT_LOCK = new Object();

    private static volatile CefApp cefApp;
    private static volatile boolean initializing;
    private static volatile RuntimeException initFailure;
    private static final Set<BrowserWindowSession> OPEN_WINDOWS = ConcurrentHashMap.newKeySet();
    private static volatile boolean noisyConsoleSuppressed;

    static {
        suppressWebcamLoggerNoise();
    }

    public BrowserSession createBrowserSession(String initialUrl, Consumer<String> statusConsumer) {
        return createBrowserSession(initialUrl, statusConsumer, null);
    }

    public BrowserSession createBrowserSession(String initialUrl,
                                               Consumer<String> statusConsumer,
                                               Function<String, String> queryHandler) {
        CefApp app = ensureApp();
        CefClient client = app.createClient();

        if (statusConsumer != null) {
            client.addDisplayHandler(new CefDisplayHandlerAdapter() {
                @Override
                public void onStatusMessage(CefBrowser browser, String value) {
                    if (value != null && !value.isBlank()) {
                        Platform.runLater(() -> statusConsumer.accept(value));
                    }
                }
            });
        }

        final CefBrowser[] browserRef = new CefBrowser[1];
        client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean onBeforePopup(CefBrowser browser, org.cef.browser.CefFrame frame, String targetUrl, String targetFrameName) {
                if (browserRef[0] != null && targetUrl != null && !targetUrl.isBlank()) {
                    browserRef[0].loadURL(targetUrl);
                }
                return true;
            }
        });

        if (queryHandler != null) {
            CefMessageRouter router = CefMessageRouter.create();
            router.addHandler(new CefMessageRouterHandlerAdapter() {
                @Override
                public boolean onQuery(CefBrowser browser,
                                       org.cef.browser.CefFrame frame,
                                       long queryId,
                                       String request,
                                       boolean persistent,
                                       CefQueryCallback callback) {
                    try {
                        String response = queryHandler.apply(request);
                        callback.success(response == null ? "" : response);
                    } catch (Exception e) {
                        callback.failure(-1, e.getMessage() == null ? "Erreur MedFlow" : e.getMessage());
                    }
                    return true;
                }
            }, true);
            client.addMessageRouter(router);
        }

        System.out.println("[JITSI-DEBUG] Creating CefBrowser on thread: " + Thread.currentThread().getName());
        
        // Create browser on Swing EDT to ensure proper native window initialization
        final CefBrowser[] browserArray = new CefBrowser[1];
        final CountDownLatch creationLatch = new CountDownLatch(1);
        
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("[JITSI-DEBUG] CefBrowser.createBrowser() executing on Swing EDT");
                System.out.println("[JITSI-DEBUG] Loading URL: " + initialUrl);
                browserArray[0] = client.createBrowser(initialUrl, false, false);
                System.out.println("[JITSI-DEBUG] CefBrowser created and initialized: " + browserArray[0]);
                System.out.println("[JITSI-DEBUG] Browser ready to render, native window should be visible");
            } finally {
                creationLatch.countDown();
            }
        });
        
        // Wait for browser to be created (up to 10 seconds)
        try {
            if (!creationLatch.await(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("CefBrowser creation timed out (10 seconds)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("CefBrowser creation interrupted", e);
        }
        
        if (browserArray[0] == null) {
            throw new RuntimeException("CefBrowser creation failed - null result");
        }
        
        CefBrowser browser = browserArray[0];
        browserRef[0] = browser;

        System.out.println("[JITSI-DEBUG] Browser created: " + browser);
        System.out.println("[JITSI-DEBUG] Browser UIComponent: " + browser.getUIComponent());

        JPanel panel = new JPanel(new BorderLayout());
        Component browserUi = browser.getUIComponent();
        System.out.println("[JITSI-DEBUG] Adding browser component to JPanel");
        panel.add(browserUi, BorderLayout.CENTER);
        
        // CRITICAL: Explicitly set size on the browser component itself
        System.out.println("[JITSI-DEBUG] Setting browser component bounds to 0,0,1024,600");
        browserUi.setBounds(0, 0, 1024, 600);
        browserUi.validate();
        System.out.println("[JITSI-DEBUG] Browser component after setBounds: " + browserUi.getBounds());
        
        // Set panel size constraints
        panel.setPreferredSize(new java.awt.Dimension(1024, 600));
        panel.setMinimumSize(new java.awt.Dimension(320, 220));
        panel.setBounds(0, 0, 1024, 600);
        panel.validate();
        panel.doLayout();
        
        System.out.println("[JITSI-DEBUG] JPanel final state: " + panel);
        System.out.println("[JITSI-DEBUG] JPanel bounds: " + panel.getBounds());
        System.out.println("[JITSI-DEBUG] JPanel component count: " + panel.getComponentCount());
        System.out.println("[JITSI-DEBUG] Browser component final bounds: " + browserUi.getBounds());

        return new BrowserSession(client, browser, panel);
    }

    public void attachToSwingNode(SwingNode swingNode, BrowserSession session) {
        System.out.println("[JITSI-DEBUG] attachToSwingNode called");
        System.out.println("[JITSI-DEBUG] Session component: " + session.component());
        System.out.println("[JITSI-DEBUG] Component is visible: " + session.component().isVisible());
        
        SwingUtilities.invokeLater(() -> {
            System.out.println("[JITSI-DEBUG] *** Attaching JPanel to SwingNode on Swing EDT ***");
            swingNode.setContent(session.component());
            System.out.println("[JITSI-DEBUG] *** SwingNode.setContent() completed ***");
            
            // Force Swing to repaint the entire tree
            session.component().revalidate();
            session.component().repaint();
            System.out.println("[JITSI-DEBUG] Component revalidated and repainted");
        });
        
        // Small delay to let Chromium window initialize and render
        System.out.println("[JITSI-DEBUG] Waiting 1 second for Chromium to initialize rendering...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("[JITSI-DEBUG] attachToSwingNode completed");
    }

    /**
     * Create a CefBrowser and attach it into the provided SwingNode.
     * This method inserts an empty JPanel into the SwingNode first so JavaFX
     * has a visible Swing parent, then creates the browser on the Swing EDT
     * and adds its native UI component into that panel.
     */
    public void createAndAttachBrowser(SwingNode swingNode, String initialUrl, Consumer<String> statusConsumer) {
        // Work off the FX thread to avoid blocking it
        new Thread(() -> {
            try {
                // Prepare an empty panel and attach it to the SwingNode first
                JPanel placeholder = new JPanel(new BorderLayout());
                SwingUtilities.invokeAndWait(() -> swingNode.setContent(placeholder));

                // Give JavaFX a short moment to layout the SwingNode
                try { Thread.sleep(100); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }

                // Create CefClient and browser on Swing EDT and attach to panel
                CefApp app = ensureApp();
                CefClient client = app.createClient();

                if (statusConsumer != null) {
                    client.addDisplayHandler(new CefDisplayHandlerAdapter() {
                        @Override
                        public void onStatusMessage(CefBrowser browser, String value) {
                            if (value != null && !value.isBlank()) {
                                Platform.runLater(() -> statusConsumer.accept(value));
                            }
                        }
                    });
                }

                final CefBrowser[] browserHolder = new CefBrowser[1];
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

                SwingUtilities.invokeLater(() -> {
                    try {
                        CefBrowser browser = client.createBrowser(initialUrl, false, false);
                        browserHolder[0] = browser;
                        Component ui = browser.getUIComponent();
                        placeholder.add(ui, BorderLayout.CENTER);
                        ui.setBounds(0, 0, placeholder.getWidth() > 0 ? placeholder.getWidth() : 1024,
                                     placeholder.getHeight() > 0 ? placeholder.getHeight() : 600);
                        placeholder.revalidate();
                        placeholder.repaint();
                    } finally {
                        latch.countDown();
                    }
                });

                // Wait for browser creation to complete
                try {
                    if (!latch.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                        throw new RuntimeException("Timed out creating CefBrowser");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }

                CefBrowser browser = browserHolder[0];
                if (browser == null) {
                    throw new RuntimeException("Failed to create CefBrowser");
                }

                // Optionally expose status consumer messages on load events
                client.addLoadHandler(new org.cef.handler.CefLoadHandlerAdapter() {
                    @Override
                    public void onLoadEnd(CefBrowser browser, org.cef.browser.CefFrame frame, int httpStatusCode) {
                        if (frame != null && frame.isMain() && statusConsumer != null) {
                            Platform.runLater(() -> statusConsumer.accept("Salle chargee dans Chromium embarque."));
                        }
                    }

                    @Override
                    public void onLoadError(CefBrowser browser, org.cef.browser.CefFrame frame, org.cef.handler.CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
                        if (frame != null && frame.isMain() && statusConsumer != null) {
                            Platform.runLater(() -> statusConsumer.accept("Erreur Chromium: " + errorText));
                        }
                    }
                });

            } catch (Throwable t) {
                t.printStackTrace();
                if (statusConsumer != null) {
                    Platform.runLater(() -> statusConsumer.accept("Chromium embedding failed: " + t.getMessage()));
                }
            }
        }, "jcef-attach-thread").start();
    }

    public BrowserWindowSession createBrowserWindowSession(String title, String initialUrl, Consumer<String> statusConsumer) {
        return createBrowserWindowSession(title, initialUrl, statusConsumer, null);
    }

    public BrowserWindowSession createBrowserWindowSession(String title,
                                                           String initialUrl,
                                                           Consumer<String> statusConsumer,
                                                           Function<String, String> queryHandler) {
        BrowserSession session = createBrowserSession(initialUrl, statusConsumer, queryHandler);
        JFrame frame = new JFrame(title == null || title.isBlank() ? "MedFlow Video Room" : title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(session.component(), BorderLayout.CENTER);
        frame.setSize(1320, 860);
        frame.setLocationRelativeTo(null);
        BrowserWindowSession windowSession = new BrowserWindowSession(session.client(), session.browser(), session.component(), frame);
        OPEN_WINDOWS.add(windowSession);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                windowSession.disposeFromWindow();
            }
        });
        return windowSession;
    }

    private CefApp ensureApp() {
        installNoiseFilter();

        if (cefApp != null) {
            return cefApp;
        }
        if (initFailure != null) {
            throw initFailure;
        }

        synchronized (INIT_LOCK) {
            if (cefApp != null) {
                return cefApp;
            }
            if (initFailure != null) {
                throw initFailure;
            }
            if (initializing) {
                while (cefApp == null && initFailure == null) {
                    try {
                        INIT_LOCK.wait(200L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Initialisation Chromium interrompue.", e);
                    }
                }
                if (initFailure != null) {
                    throw initFailure;
                }
                return cefApp;
            }

            initializing = true;
            try {
                CefAppBuilder builder = new CefAppBuilder();
                Path installDir = Paths.get(System.getProperty("user.home"), "MedFlow", "jcef-bundle");
                Path cacheDir = Paths.get(System.getProperty("user.home"), "MedFlow", "jcef-cache");

                builder.setInstallDir(installDir.toFile());
                builder.getCefSettings().log_severity = CefSettings.LogSeverity.LOGSEVERITY_DISABLE;
                builder.getCefSettings().command_line_args_disabled = false;
                builder.getCefSettings().windowless_rendering_enabled = false;
                builder.getCefSettings().cache_path = cacheDir.toString();
                builder.getCefSettings().persist_session_cookies = true;
                builder.addJcefArgs("--enable-media-stream");
                builder.addJcefArgs("--use-fake-ui-for-media-stream");
                builder.addJcefArgs("--autoplay-policy=no-user-gesture-required");
                builder.addJcefArgs("--enable-usermedia-screen-capturing");
                builder.addJcefArgs("--allow-file-access-from-files");
                builder.addJcefArgs("--allow-universal-access-from-files");
                builder.addJcefArgs("--unsafely-treat-insecure-origin-as-secure=file://");
                builder.addJcefArgs("--disable-background-networking");
                builder.addJcefArgs("--disable-component-update");
                builder.addJcefArgs("--disable-domain-reliability");
                builder.addJcefArgs("--disable-sync");
                builder.addJcefArgs("--metrics-recording-only");
                builder.addJcefArgs("--disable-notifications");
                builder.addJcefArgs("--disable-client-side-phishing-detection");
                builder.addJcefArgs("--disable-features=MediaRouter,OptimizationHints,AutofillServerCommunication,CertificateTransparencyComponentUpdater,NotificationTriggers,PushMessaging");
                builder.addJcefArgs("--no-pings");
                builder.addJcefArgs("--disable-logging");
                builder.addJcefArgs("--log-severity=3");

                builder.setAppHandler(new MavenCefAppHandlerAdapter() {
                });

                new File(cacheDir.toString()).mkdirs();
                cefApp = builder.build();
                return cefApp;
            } catch (Exception e) {
                initFailure = new RuntimeException("Impossible d'initialiser Chromium embarque.", e);
                throw initFailure;
            } finally {
                initializing = false;
                INIT_LOCK.notifyAll();
            }
        }
    }

    private void installNoiseFilter() {
        if (noisyConsoleSuppressed) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (noisyConsoleSuppressed) {
                return;
            }
            System.setOut(new NoiseFilteringPrintStream(System.out));
            System.setErr(new NoiseFilteringPrintStream(System.err));
            noisyConsoleSuppressed = true;
        }
    }

    private static boolean shouldSuppressLine(String line) {
        if (line == null) {
            return false;
        }
        List<String> patterns = List.of(
                "me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler handleProgress",
                "INFOS: LOCATING |> In progress...",
                "INFOS: INITIALIZING |> In progress...",
                "INFOS: INITIALIZED |> In progress...",
                "A restricted method in java.lang.System has been called",
                "java.lang.System::loadLibrary has been called by me.friwi.jcefmaven",
                "Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module",
                "Restricted methods will be blocked in a future release unless native access is enabled",
                "initialize on Thread[",
                "google_apis\\gcm\\engine\\registration_request.cc:292",
                "google_apis\\gcm\\engine\\mcs_client.cc:700",
                "google_apis\\gcm\\engine\\mcs_client.cc:702",
                "PHONE_REGISTRATION_ERROR",
                "Authentication Failed: wrong_secret",
                "Registration response error message: DEPRECATED_ENDPOINT",
                "Registration response error message: DEPRECATED_ENDPOINT",
                "Registration response error message: QUOTA_EXCEEDED"
        );
        for (String pattern : patterns) {
            if (line.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static final class NoiseFilteringPrintStream extends PrintStream {
        private final PrintStream delegate;
        private final StringBuilder buffer = new StringBuilder();

        private NoiseFilteringPrintStream(PrintStream delegate) {
            super(new OutputStream() {
                @Override
                public void write(int b) {
                }
            }, true, StandardCharsets.UTF_8);
            this.delegate = delegate;
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            String chunk = new String(buf, off, len, StandardCharsets.UTF_8);
            for (int i = 0; i < chunk.length(); i++) {
                char c = chunk.charAt(i);
                if (c == '\r') {
                    continue;
                }
                if (c == '\n') {
                    flushBuffer();
                } else {
                    buffer.append(c);
                }
            }
        }

        @Override
        public void flush() {
            flushBuffer();
            delegate.flush();
        }

        private void flushBuffer() {
            if (buffer.length() == 0) {
                return;
            }
            String line = buffer.toString();
            buffer.setLength(0);
            if (!shouldSuppressLine(line)) {
                delegate.println(line);
            }
        }
    }

    private static void suppressWebcamLoggerNoise() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        System.setProperty("org.slf4j.simpleLogger.log.com.github.sarxos.webcam", "error");
        System.setProperty("org.slf4j.simpleLogger.log.com.github.sarxos.webcam.ds.cgt", "error");
    }

    public record BrowserSession(CefClient client, CefBrowser browser, JComponent component) {
        public void load(String url) {
            if (browser != null && url != null && !url.isBlank()) {
                browser.loadURL(url);
            }
        }

        public void close() {
            try {
                if (browser != null) {
                    browser.close(true);
                }
            } catch (Exception ignored) {
            }
            try {
                if (client != null) {
                    client.dispose();
                }
            } catch (Exception ignored) {
            }
        }
    }

    public record BrowserWindowSession(CefClient client, CefBrowser browser, JComponent component, JFrame frame) {
        public void show() {
            if (frame != null) {
                SwingUtilities.invokeLater(() -> {
                    frame.setVisible(true);
                    frame.toFront();
                    frame.requestFocus();
                });
            }
        }

        public void load(String url) {
            if (browser != null && url != null && !url.isBlank()) {
                browser.loadURL(url);
            }
        }

        public void close() {
            OPEN_WINDOWS.remove(this);
            try {
                if (frame != null) {
                    SwingUtilities.invokeLater(frame::dispose);
                }
            } catch (Exception ignored) {
            }
            try {
                if (browser != null) {
                    browser.close(true);
                }
            } catch (Exception ignored) {
            }
            try {
                if (client != null) {
                    client.dispose();
                }
            } catch (Exception ignored) {
            }
        }

        private void disposeFromWindow() {
            OPEN_WINDOWS.remove(this);
            try {
                if (browser != null) {
                    browser.close(true);
                }
            } catch (Exception ignored) {
            }
            try {
                if (client != null) {
                    client.dispose();
                }
            } catch (Exception ignored) {
            }
            try {
                if (frame != null) {
                    frame.dispose();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
