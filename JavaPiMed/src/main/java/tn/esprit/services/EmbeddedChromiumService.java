package tn.esprit.services;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
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
import java.util.function.Consumer;

public class EmbeddedChromiumService {

    private static final Object INIT_LOCK = new Object();

    private static volatile CefApp cefApp;
    private static volatile boolean initializing;
    private static volatile RuntimeException initFailure;
    private static final Set<BrowserWindowSession> OPEN_WINDOWS = ConcurrentHashMap.newKeySet();
    private static volatile boolean noisyConsoleSuppressed;

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

            client.addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadEnd(CefBrowser browser, org.cef.browser.CefFrame frame, int httpStatusCode) {
                    if (frame != null && frame.isMain()) {
                        Platform.runLater(() -> statusConsumer.accept("Salle chargee dans Chromium embarque."));
                    }
                }

                @Override
                public void onLoadError(CefBrowser browser, org.cef.browser.CefFrame frame, org.cef.handler.CefLoadHandler.ErrorCode errorCode, String errorText, String failedUrl) {
                    if (frame != null && frame.isMain()) {
                        Platform.runLater(() -> statusConsumer.accept("Erreur Chromium: " + errorText));
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

        CefBrowser browser = client.createBrowser(initialUrl, false, false);
        browserRef[0] = browser;

        JPanel panel = new JPanel(new BorderLayout());
        Component browserUi = browser.getUIComponent();
        panel.add(browserUi, BorderLayout.CENTER);

        return new BrowserSession(client, browser, panel);
    }

    public void attachToSwingNode(SwingNode swingNode, BrowserSession session) {
        SwingUtilities.invokeLater(() -> swingNode.setContent(session.component()));
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