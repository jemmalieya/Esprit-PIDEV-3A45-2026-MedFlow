package tn.esprit.services;

import tn.esprit.entities.Evenement;
import tn.esprit.entities.Ressource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.net.URLEncoder;

public class AccessibilityRoomService {

    public static final String ACCESSIBILITY_CATEGORY = "Accessibilite";
    public static final String ROOM_RESOURCE_NAME = "Salle live accessibilite";
    public static final String TRANSCRIPT_RESOURCE_NAME = "Transcription live";

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final RessourceService ressourceService = new RessourceService();

    public String buildRoomName(Evenement evenement) {
        String base = safe(evenement.getTitre_event());
        String slug = slugify(base);
        if (slug.isBlank()) {
            slug = "event-" + evenement.getId();
        }
        return "MedFlow-" + evenement.getId() + "-" + slug;
    }

    public String buildRoomUrl(Evenement evenement) {
        Optional<Ressource> existing = ressourceService.trouverParEvenementNomType(
                evenement.getId(),
                ROOM_RESOURCE_NAME,
                "external_link"
        );
        return existing.map(Ressource::getUrl_externe_ressource).map(this::safe).map(String::trim).orElse("");
    }

    public boolean hasConfiguredRoomLink(Evenement evenement) {
        return !buildRoomUrl(evenement).isBlank();
    }

    public Ressource saveRoomLink(Evenement evenement, String roomUrl) {
        String sanitizedUrl = safe(roomUrl).trim();
        if (sanitizedUrl.isBlank()) {
            Optional<Ressource> existing = ressourceService.trouverParEvenementNomType(
                    evenement.getId(),
                    ROOM_RESOURCE_NAME,
                    "external_link"
            );
            existing.ifPresent(ressourceService::supprimer);
            return null;
        }

        if (!sanitizedUrl.matches("^(https?)://.+$")) {
            throw new IllegalArgumentException("Lien d'appel invalide.");
        }

        Optional<Ressource> existing = ressourceService.trouverParEvenementNomType(
                evenement.getId(),
                ROOM_RESOURCE_NAME,
                "external_link"
        );

        Ressource resource = existing.orElseGet(Ressource::new);
        boolean isNew = existing.isEmpty();
        Date now = new Date();

        resource.setEvenement(evenement);
        resource.setNom_ressource(ROOM_RESOURCE_NAME);
        resource.setCategorie_ressource(ACCESSIBILITY_CATEGORY);
        resource.setType_ressource("external_link");
        resource.setChemin_fichier_ressource("");
        resource.setMime_type_ressource("text/uri-list");
        resource.setTaille_kb_ressource(1);
        resource.setUrl_externe_ressource(sanitizedUrl);
        resource.setQuantite_disponible_ressource(0);
        resource.setUnite_ressource("");
        resource.setFournisseur_ressource("Lien video configure");
        resource.setCout_estime_ressource(0);
        resource.setEst_publique_ressource(true);
        resource.setNotes_ressource("Lien d'appel video configure pour l'evenement.");
        if (isNew) {
            resource.setDate_creation_ressource(now);
            resource.setDate_mise_a_jour_ressource(now);
            ressourceService.ajouter(resource);
        } else {
            resource.setDate_mise_a_jour_ressource(now);
            ressourceService.modifier(resource);
        }
        return resource;
    }

    public Path createEmbeddedRoomPage(Evenement evenement) throws IOException {
        String roomUrl = buildRoomUrl(evenement);
        String roomName = extractJitsiRoomName(roomUrl);
        if (roomName.isBlank()) {
            throw new IllegalStateException("Le mode integre MedFlow requiert un lien meet.jit.si valide.");
        }

        Path directory = Paths.get(System.getProperty("java.io.tmpdir"), "medflow-accessibility");
        Files.createDirectories(directory);

        String fileName = "embedded-room-event-" + evenement.getId() + ".html";
        Path file = directory.resolve(fileName);

        String eventTitle = escapeHtml(safe(evenement.getTitre_event()));
        String meta = escapeHtml(safe(evenement.getVille_event()));

        String htmlTemplate = """
                <!doctype html>
                <html lang="fr">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>MedFlow Room</title>
                  <style>
                    :root {
                      --bg: #0f1e27;
                      --card: #132b37;
                      --accent: #15a6ca;
                      --text: #f4fbff;
                      --muted: #a9c0cb;
                      --warn: #ffd977;
                    }
                    * { box-sizing: border-box; }
                    html, body { height: 100%; }
                    body {
                      margin: 0;
                      font-family: "Segoe UI", sans-serif;
                      background:
                        radial-gradient(circle at top left, rgba(21,166,202,0.18), transparent 34%),
                        linear-gradient(135deg, #102430, #0b1820 58%, #071117);
                      color: var(--text);
                    }
                    .page {
                      height: 100%;
                      display: grid;
                      grid-template-rows: auto auto 1fr;
                      gap: 14px;
                      padding: 14px;
                    }
                    .workspace {
                      min-height: 0;
                      display: grid;
                      grid-template-columns: minmax(0, 2fr) minmax(320px, 0.95fr);
                      gap: 14px;
                    }
                    .hero, .status {
                      background: rgba(19,43,55,0.92);
                      border: 1px solid rgba(255,255,255,0.08);
                      border-radius: 18px;
                      padding: 14px 18px;
                      backdrop-filter: blur(10px);
                    }
                    .eyebrow {
                      color: var(--accent);
                      font-size: 12px;
                      font-weight: 800;
                      letter-spacing: 0.18em;
                      text-transform: uppercase;
                    }
                    h1 {
                      margin: 6px 0 4px;
                      font-size: 24px;
                    }
                    .meta {
                      color: var(--muted);
                      font-size: 14px;
                    }
                    .status {
                      color: var(--warn);
                      font-weight: 700;
                    }
                    #meet-card, .assistant-card {
                      min-height: 0;
                      background: rgba(19,43,55,0.92);
                      border: 1px solid rgba(255,255,255,0.08);
                      border-radius: 24px;
                      padding: 12px;
                      backdrop-filter: blur(10px);
                    }
                    #meet {
                      min-height: 100%;
                      height: 100%;
                      border-radius: 24px;
                      overflow: hidden;
                      box-shadow: 0 18px 60px rgba(0,0,0,0.32);
                      background: rgba(0,0,0,0.28);
                    }
                    .assistant-card {
                      display: grid;
                      grid-template-rows: auto auto auto 1fr auto;
                      gap: 12px;
                    }
                    .assistant-title {
                      font-size: 18px;
                      font-weight: 800;
                    }
                    .assistant-copy {
                      color: var(--muted);
                      font-size: 13px;
                      line-height: 1.45;
                    }
                    .camera-shell {
                      position: relative;
                      overflow: hidden;
                      border-radius: 18px;
                      background: #081117;
                      min-height: 240px;
                      border: 1px solid rgba(255,255,255,0.08);
                    }
                    #video {
                      width: 100%;
                      height: 100%;
                      object-fit: cover;
                      display: block;
                    }
                    .subtitle {
                      position: absolute;
                      left: 12px;
                      right: 12px;
                      bottom: 12px;
                      border-radius: 14px;
                      padding: 10px 12px;
                      background: rgba(0,0,0,0.62);
                      color: #fff;
                      font-size: 20px;
                      font-weight: 800;
                      text-align: center;
                    }
                    .chip-row {
                      display: flex;
                      gap: 8px;
                      flex-wrap: wrap;
                    }
                    .chip {
                      display: inline-flex;
                      align-items: center;
                      border-radius: 999px;
                      padding: 8px 12px;
                      background: rgba(21,166,202,0.16);
                      color: #c8f4ff;
                      font-size: 12px;
                      font-weight: 800;
                      letter-spacing: 0.02em;
                    }
                    textarea {
                      width: 100%;
                      min-height: 180px;
                      border-radius: 18px;
                      border: 1px solid rgba(255,255,255,0.08);
                      background: rgba(5,15,20,0.45);
                      color: var(--text);
                      padding: 14px;
                      resize: vertical;
                      font: 14px/1.5 "Segoe UI", sans-serif;
                    }
                    .actions {
                      display: flex;
                      gap: 10px;
                      flex-wrap: wrap;
                    }
                    button {
                      border: none;
                      border-radius: 14px;
                      padding: 11px 16px;
                      font-weight: 800;
                      cursor: pointer;
                    }
                    .primary {
                      background: linear-gradient(135deg, #15a6ca, #0f8aae);
                      color: white;
                    }
                    .light {
                      background: rgba(255,255,255,0.1);
                      color: white;
                    }
                    @media (max-width: 1100px) {
                      .workspace {
                        grid-template-columns: 1fr;
                      }
                    }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <div class="hero">
                      <div class="eyebrow">MedFlow Live Room</div>
                      <h1>__EVENT_TITLE__</h1>
                      <div class="meta">__EVENT_META__</div>
                    </div>
                    <div class="status" id="status">
                      Chargement de la salle video integree...
                    </div>
                    <div class="workspace">
                      <div id="meet-card">
                        <div id="meet"></div>
                      </div>
                      <div class="assistant-card">
                        <div class="assistant-title">Assistant langue des signes</div>
                        <div class="assistant-copy">Camera locale + detection MediaPipe. La transcription peut etre enregistree directement dans MedFlow depuis cette fenetre.</div>
                        <div class="chip-row">
                          <div class="chip">Main ouverte = Bonjour</div>
                          <div class="chip">Poing ferme = Merci</div>
                          <div class="chip">Index = Oui</div>
                          <div class="chip">Pouce haut = Bravo</div>
                          <div class="chip">Pouce bas = Non</div>
                          <div class="chip">V = Paix</div>
                          <div class="chip">3 doigts = Aide</div>
                          <div class="chip">4 doigts = Medecin</div>
                          <div class="chip">OK = Merci beaucoup</div>
                          <div class="chip">2 mains ouvertes = Urgence</div>
                        </div>
                        <div class="camera-shell">
                          <video id="video" autoplay muted playsinline></video>
                          <div class="subtitle" id="subtitleText">…</div>
                        </div>
                        <textarea id="transcript" placeholder="La transcription gestuelle apparait ici..."></textarea>
                        <div class="actions">
                          <button class="light" id="btnClear">Vider</button>
                          <button class="primary" id="btnCopy">Copier la transcription</button>
                          <button class="primary" id="btnSave">Enregistrer dans MedFlow</button>
                        </div>
                      </div>
                    </div>
                  </div>

                  <script src="https://meet.jit.si/external_api.js"></script>
                  <script src="https://cdn.jsdelivr.net/npm/@mediapipe/camera_utils/camera_utils.js"></script>
                  <script src="https://cdn.jsdelivr.net/npm/@mediapipe/hands/hands.js"></script>
                  <script>
                    const statusEl = document.getElementById('status');
                    const roomName = __ROOM_NAME__;
                    const roomUrl = __ROOM_URL__;
                    const video = document.getElementById('video');
                    const subtitleText = document.getElementById('subtitleText');
                    const transcriptArea = document.getElementById('transcript');
                    const btnClear = document.getElementById('btnClear');
                    const btnCopy = document.getElementById('btnCopy');
                    const btnSave = document.getElementById('btnSave');
                    let lastWord = '';
                    let lastTime = 0;
                    const gestureHistory = [];
                    const MAX_HISTORY = 6;
                    const vocabulary = [
                      { label: 'Merci beaucoup', match: (g) => g.okSign },
                      { label: 'Aide', match: (g) => g.helpSign },
                      { label: 'Medecin', match: (g) => g.doctorSign },
                      { label: 'Appeler', match: (g) => g.callSign },
                      { label: 'Je t aime', match: (g) => g.loveSign },
                      { label: 'Eau', match: (g) => g.waterSign },
                      { label: 'Question', match: (g) => g.questionSign },
                      { label: 'Douleur', match: (g) => g.painSign },
                      { label: 'Bravo', match: (g) => g.thumbUp },
                      { label: 'Non', match: (g) => g.thumbDown },
                      { label: 'Bonjour', match: (g) => g.stopSign },
                      { label: 'Merci', match: (g) => g.closedFist },
                      { label: 'Oui', match: (g) => g.indexOnly },
                      { label: 'Paix', match: (g) => g.vSign }
                    ];

                    function setStatus(message) {
                      statusEl.textContent = message;
                    }

                    function pushWord(word) {
                      const now = Date.now();
                      if (!word) return;
                      if (word === lastWord && (now - lastTime) < 1500) return;
                      lastWord = word;
                      lastTime = now;
                      subtitleText.textContent = word;
                      transcriptArea.value += word + ' ';
                    }

                    function detectStableWord(candidate) {
                      if (!candidate) return '';
                      gestureHistory.push(candidate);
                      if (gestureHistory.length > MAX_HISTORY) {
                        gestureHistory.shift();
                      }
                      const count = gestureHistory.filter(item => item === candidate).length;
                      return count >= 3 ? candidate : '';
                    }

                    if (!window.JitsiMeetExternalAPI) {
                      setStatus("Le module Jitsi n'a pas pu etre charge. Utilisez le lien de secours : " + roomUrl);
                    } else {
                      const api = new JitsiMeetExternalAPI("meet.jit.si", {
                        roomName,
                        parentNode: document.getElementById("meet"),
                        width: "100%",
                        height: "100%",
                        configOverwrite: {
                          prejoinPageEnabled: false,
                          startWithAudioMuted: false,
                          startWithVideoMuted: false,
                          disableDeepLinking: true
                        },
                        interfaceConfigOverwrite: {
                          MOBILE_APP_PROMO: false,
                          HIDE_DEEP_LINKING_LOGO: true
                        }
                      });

                      setStatus("Salle chargee dans MedFlow. Autorisez camera et micro si le systeme le demande.");

                      api.addListener("videoConferenceJoined", () => {
                        setStatus("Connexion etablie dans la salle integree.");
                      });

                      api.addListener("participantJoined", () => {
                        setStatus("Participant detecte. Salle active dans l'application.");
                      });

                      api.addListener("cameraError", () => {
                        setStatus("Camera indisponible dans cette session. Essayez le lien de secours si besoin.");
                      });

                      api.addListener("audioMuteStatusChanged", (event) => {
                        if (event && event.muted) {
                          setStatus("Micro coupe. Vous pouvez le reactiver depuis la salle.");
                        }
                      });

                      window.addEventListener("error", () => {
                        setStatus("Erreur d'affichage. Utilisez le lien de secours : " + roomUrl);
                      });
                    }

                    const hands = new Hands({
                      locateFile: (file) => `https://cdn.jsdelivr.net/npm/@mediapipe/hands/${file}`
                    });

                    hands.setOptions({
                      maxNumHands: 1,
                      modelComplexity: 1,
                      minDetectionConfidence: 0.7,
                      minTrackingConfidence: 0.7
                    });

                    function dist(a, b) {
                      const dx = a.x - b.x;
                      const dy = a.y - b.y;
                      return Math.sqrt(dx * dx + dy * dy);
                    }

                    function buildGestureState(landmarks) {
                      const thumbTip = landmarks[4];
                      const thumbIp = landmarks[3];
                      const indexTip = landmarks[8];
                      const indexDip = landmarks[7];
                      const middleTip = landmarks[12];
                      const middleDip = landmarks[11];
                      const ringTip = landmarks[16];
                      const ringDip = landmarks[15];
                      const pinkyTip = landmarks[20];
                      const pinkyDip = landmarks[19];
                      const indexBase = landmarks[5];
                      const middleBase = landmarks[9];
                      const ringBase = landmarks[13];
                      const pinkyBase = landmarks[17];
                      const wrist = landmarks[0];

                      const indexUp = indexTip.y < indexDip.y && indexTip.y < indexBase.y;
                      const middleUp = middleTip.y < middleDip.y && middleTip.y < middleBase.y;
                      const ringUp = ringTip.y < ringDip.y && ringTip.y < ringBase.y;
                      const pinkyUp = pinkyTip.y < pinkyDip.y && pinkyTip.y < pinkyBase.y;
                      const thumbLeft = thumbTip.x < thumbIp.x;

                      return {
                        indexUp,
                        middleUp,
                        ringUp,
                        pinkyUp,
                        thumbUp: thumbTip.y < thumbIp.y && dist(thumbTip, wrist) > dist(indexTip, wrist) * 0.72,
                        thumbDown: thumbTip.y > thumbIp.y && !indexUp && !middleUp && !ringUp && !pinkyUp,
                        okSign: dist(thumbTip, indexTip) < 0.05 && middleUp && ringUp && pinkyUp,
                        helpSign: indexUp && middleUp && ringUp && !pinkyUp,
                        doctorSign: indexUp && middleUp && ringUp && pinkyUp && dist(indexTip, middleTip) < 0.06,
                        stopSign: indexUp && middleUp && ringUp && pinkyUp && dist(indexTip, pinkyTip) > 0.18,
                        waterSign: indexUp && middleUp && !ringUp && pinkyUp,
                        questionSign: indexUp && !middleUp && !ringUp && pinkyUp,
                        painSign: !indexUp && middleUp && !ringUp && !pinkyUp,
                        callSign: thumbLeft && !indexUp && !middleUp && !ringUp && pinkyUp,
                        loveSign: thumbLeft && indexUp && !middleUp && !ringUp && pinkyUp,
                        closedFist: !indexUp && !middleUp && !ringUp && !pinkyUp,
                        indexOnly: indexUp && !middleUp && !ringUp && !pinkyUp,
                        vSign: indexUp && middleUp && !ringUp && !pinkyUp
                      };
                    }

                    hands.onResults((results) => {
                      if (!results.multiHandLandmarks || !results.multiHandLandmarks.length) return;

                      const states = results.multiHandLandmarks.map(buildGestureState);

                      if (states.length >= 2) {
                        const left = states[0];
                        const right = states[1];
                        if (left.stopSign && right.stopSign) {
                          const stable = detectStableWord('Urgence');
                          if (stable) pushWord(stable);
                          return;
                        }
                        if (left.thumbUp && right.thumbUp) {
                          const stable = detectStableWord('Tres bien');
                          if (stable) pushWord(stable);
                          return;
                        }
                        if (left.helpSign && right.helpSign) {
                          const stable = detectStableWord('Besoin d aide');
                          if (stable) pushWord(stable);
                          return;
                        }
                      }

                      const candidate = vocabulary.find(entry => entry.match(states[0]))?.label || '';
                      const stable = detectStableWord(candidate);
                      if (stable) {
                        pushWord(stable);
                      }
                    });

                    navigator.mediaDevices.getUserMedia({ video: true })
                      .then((stream) => {
                        video.srcObject = stream;
                        const camera = new Camera(video, {
                          onFrame: async () => {
                            await hands.send({ image: video });
                          },
                          width: 640,
                          height: 480
                        });
                        camera.start();
                      })
                      .catch(() => {
                        subtitleText.textContent = 'Camera indisponible';
                      });

                    btnClear.addEventListener('click', () => {
                      transcriptArea.value = '';
                      subtitleText.textContent = '…';
                    });

                    btnCopy.addEventListener('click', async () => {
                      try {
                        await navigator.clipboard.writeText(transcriptArea.value.trim());
                        setStatus('Transcription copiee. Collez-la dans MedFlow pour l enregistrer.');
                      } catch (e) {
                        setStatus('Copie impossible dans cette session.');
                      }
                    });

                    btnSave.addEventListener('click', () => {
                      const text = transcriptArea.value.trim();
                      if (!text) {
                        setStatus('Aucune transcription a enregistrer.');
                        return;
                      }
                      if (typeof window.cefQuery !== 'function') {
                        setStatus('Pont MedFlow indisponible dans cette session.');
                        return;
                      }
                      window.cefQuery({
                        request: 'saveTranscript:' + text,
                        onSuccess: () => setStatus('Transcription enregistree dans MedFlow.'),
                        onFailure: (_code, message) => setStatus('Erreur MedFlow: ' + message)
                      });
                    });
                  </script>
                </body>
                </html>
                """;

        String html = htmlTemplate
                .replace("__EVENT_TITLE__", eventTitle)
                .replace("__EVENT_META__", meta.isBlank() ? "Salle d'accessibilite integree" : meta)
                .replace("__ROOM_NAME__", toJsStringLiteral(roomName))
                .replace("__ROOM_URL__", toJsStringLiteral(buildRoomUrl(evenement)));

        Files.writeString(file, html, StandardCharsets.UTF_8);
        return file;
    }

    public Ressource ensureRoomResource(Evenement evenement) {
        Optional<Ressource> existing = ressourceService.trouverParEvenementNomType(
                evenement.getId(),
                ROOM_RESOURCE_NAME,
                "external_link"
        );

        return existing.orElse(null);
    }

    public Ressource saveTranscript(Evenement evenement, String transcript) throws IOException {
        Path directory = Paths.get(System.getProperty("user.home"), "MedFlow", "accessibility");
        Files.createDirectories(directory);

        String timestamp = FILE_TS.format(LocalDateTime.now());
        String fileName = "event_" + evenement.getId() + "_transcript_" + timestamp + ".txt";
        Path file = directory.resolve(fileName);

        String payload = "Evenement: " + safe(evenement.getTitre_event()) + System.lineSeparator()
                + "Salle: " + buildRoomUrl(evenement) + System.lineSeparator()
                + "Genere le: " + timestamp + System.lineSeparator()
                + System.lineSeparator()
                + transcript.trim()
                + System.lineSeparator();

        Files.writeString(file, payload, StandardCharsets.UTF_8);

        Optional<Ressource> existing = ressourceService.trouverParEvenementNomType(
                evenement.getId(),
                TRANSCRIPT_RESOURCE_NAME,
                "file"
        );

        Ressource resource = existing.orElseGet(Ressource::new);
        boolean isNew = existing.isEmpty();
        Date now = new Date();

        resource.setEvenement(evenement);
        resource.setNom_ressource(TRANSCRIPT_RESOURCE_NAME);
        resource.setCategorie_ressource(ACCESSIBILITY_CATEGORY);
        resource.setType_ressource("file");
        resource.setChemin_fichier_ressource(file.toAbsolutePath().toString());
        resource.setMime_type_ressource("text/plain");
        resource.setTaille_kb_ressource((int) Math.max(1, Math.round(Files.size(file) / 1024.0)));
        resource.setUrl_externe_ressource("");
        resource.setQuantite_disponible_ressource(0);
        resource.setUnite_ressource("");
        resource.setFournisseur_ressource("MedFlow Accessibility Room");
        resource.setCout_estime_ressource(0);
        resource.setEst_publique_ressource(false);
        resource.setNotes_ressource(buildTranscriptSummary(transcript));
        if (isNew) {
            resource.setDate_creation_ressource(now);
            resource.setDate_mise_a_jour_ressource(now);
            ressourceService.ajouter(resource);
        } else {
            resource.setDate_mise_a_jour_ressource(now);
            ressourceService.modifier(resource);
        }

        return resource;
    }

    public List<Ressource> getAccessibilityResources(int evenementId) {
        return ressourceService.recupererParEvenement(evenementId).stream()
                .filter(resource -> ACCESSIBILITY_CATEGORY.equalsIgnoreCase(safe(resource.getCategorie_ressource())))
                .collect(Collectors.toList());
    }

    public Path createCaptionAssistantPage(Evenement evenement) throws IOException {
        Path directory = Paths.get(System.getProperty("java.io.tmpdir"), "medflow-accessibility");
        Files.createDirectories(directory);

        String fileName = "captions-assistant-event-" + evenement.getId() + ".html";
        Path file = directory.resolve(fileName);

        String html = """
                <!doctype html>
                <html lang="fr">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>MedFlow Captions Assistant</title>
                  <style>
                    :root {
                      --bg1: #f4fbff;
                      --bg2: #eef3f8;
                      --card: #ffffff;
                      --text: #173745;
                      --muted: #607784;
                      --accent: #0e8fb0;
                      --ok: #22a66f;
                    }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0;
                      font-family: "Segoe UI", sans-serif;
                      background: linear-gradient(135deg, var(--bg1), var(--bg2));
                      color: var(--text);
                    }
                    .page {
                      max-width: 1100px;
                      margin: 0 auto;
                      padding: 28px;
                    }
                    .card {
                      background: var(--card);
                      border-radius: 24px;
                      padding: 22px;
                      box-shadow: 0 12px 32px rgba(0,0,0,0.08);
                      margin-bottom: 18px;
                    }
                    h1 { margin: 0 0 6px; font-size: 32px; }
                    .muted { color: var(--muted); }
                    .row { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 16px; }
                    button {
                      border: none;
                      border-radius: 14px;
                      padding: 12px 18px;
                      font-weight: 700;
                      cursor: pointer;
                    }
                    .primary { background: var(--accent); color: white; }
                    .success { background: var(--ok); color: white; }
                    .light { background: #eef4f8; color: var(--text); }
                    textarea {
                      width: 100%;
                      min-height: 360px;
                      margin-top: 14px;
                      border-radius: 18px;
                      border: 1px solid #d7e4eb;
                      padding: 14px;
                      font: 15px/1.5 "Segoe UI", sans-serif;
                      resize: vertical;
                    }
                    .status {
                      margin-top: 12px;
                      background: #eafaf5;
                      color: #166b55;
                      padding: 10px 14px;
                      border-radius: 14px;
                      font-weight: 700;
                    }
                    .chip {
                      display: inline-block;
                      background: #eef9ff;
                      color: #0e6077;
                      border-radius: 999px;
                      padding: 8px 12px;
                      font-weight: 700;
                      margin-right: 8px;
                    }
                  </style>
                </head>
                <body>
                  <div class="page">
                    <div class="card">
                      <div class="chip">Live captions</div>
                      <div class="chip">Browser speech API</div>
                      <h1>Sous-titres live MedFlow</h1>
                      <div class="muted" id="eventLine"></div>
                      <div class="row">
                        <button class="primary" id="startBtn">Demarrer l'ecoute</button>
                        <button class="light" id="stopBtn">Arreter</button>
                        <button class="success" id="copyBtn">Copier la transcription</button>
                        <button class="light" id="clearBtn">Vider</button>
                      </div>
                      <div class="status" id="status">Pret. Utilisez Chrome ou Edge pour la reconnaissance vocale.</div>
                      <textarea id="transcript" placeholder="La transcription apparait ici..."></textarea>
                    </div>
                  </div>
                  <script>
                    const params = new URLSearchParams(window.location.search);
                    const eventTitle = params.get('event') || 'Evenement';
                    const roomUrl = params.get('room') || '';
                    document.getElementById('eventLine').textContent = eventTitle + (roomUrl ? ' | Salle: ' + roomUrl : '');

                    const status = document.getElementById('status');
                    const transcript = document.getElementById('transcript');
                    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
                    let recognition = null;

                    if (!SpeechRecognition) {
                      status.textContent = "Reconnaissance vocale indisponible ici. Ouvrez cette page dans Chrome ou Edge.";
                    } else {
                      recognition = new SpeechRecognition();
                      recognition.lang = 'fr-FR';
                      recognition.continuous = true;
                      recognition.interimResults = true;

                      let committed = '';

                      recognition.onstart = () => {
                        status.textContent = "Ecoute active. Les sous-titres sont en cours de capture.";
                      };

                      recognition.onerror = (event) => {
                        status.textContent = "Erreur micro/reconnaissance: " + event.error;
                      };

                      recognition.onend = () => {
                        status.textContent = "Ecoute arretee.";
                      };

                      recognition.onresult = (event) => {
                        let interim = '';
                        for (let i = event.resultIndex; i < event.results.length; i++) {
                          const text = event.results[i][0].transcript.trim();
                          if (event.results[i].isFinal) {
                            committed += text + "\\n";
                          } else {
                            interim += text + ' ';
                          }
                        }
                        transcript.value = committed + interim;
                      };
                    }

                    document.getElementById('startBtn').addEventListener('click', () => {
                      if (!recognition) return;
                      recognition.start();
                    });

                    document.getElementById('stopBtn').addEventListener('click', () => {
                      if (!recognition) return;
                      recognition.stop();
                    });

                    document.getElementById('copyBtn').addEventListener('click', async () => {
                      try {
                        await navigator.clipboard.writeText(transcript.value);
                        status.textContent = "Transcription copiee. Collez-la dans MedFlow pour l'enregistrer.";
                      } catch (e) {
                        status.textContent = "Copie impossible sur ce navigateur.";
                      }
                    });

                    document.getElementById('clearBtn').addEventListener('click', () => {
                      transcript.value = '';
                      status.textContent = "Transcription videe.";
                    });
                  </script>
                </body>
                </html>
                """;

        Files.writeString(file, html, StandardCharsets.UTF_8);
        return file;
    }

    public URIData buildCaptionAssistantUriData(Evenement evenement) throws IOException {
        Path html = createCaptionAssistantPage(evenement);
        String query = "event=" + URLEncoder.encode(safe(evenement.getTitre_event()), StandardCharsets.UTF_8)
                + "&room=" + URLEncoder.encode(buildRoomUrl(evenement), StandardCharsets.UTF_8);
        return new URIData(html.toUri().toString() + "?" + query, html);
    }

    public String buildTranscriptSummary(String transcript) {
        String normalized = safe(transcript).trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 180) {
            return normalized;
        }
        return normalized.substring(0, 177) + "...";
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(safe(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private String escapeHtml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String toJsStringLiteral(String value) {
        String escaped = safe(value)
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "\\n");
        return "'" + escaped + "'";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public boolean isEmbeddableJitsiUrl(String url) {
        return !extractJitsiRoomName(url).isBlank();
    }

    private String extractJitsiRoomName(String url) {
        try {
            URI uri = URI.create(safe(url).trim());
            String host = safe(uri.getHost()).toLowerCase(Locale.ROOT);
            if (!"meet.jit.si".equals(host)) {
                return "";
            }
            String path = safe(uri.getPath()).replaceAll("^/+", "").trim();
            if (path.isBlank()) {
                return "";
            }
            int slashIndex = path.indexOf('/');
            return slashIndex >= 0 ? path.substring(0, slashIndex) : path;
        } catch (Exception ignored) {
            return "";
        }
    }

    public record URIData(String uri, Path file) {}
}
