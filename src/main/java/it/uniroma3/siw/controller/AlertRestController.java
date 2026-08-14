package it.uniroma3.siw.controller;

import it.uniroma3.siw.model.Anomalia;
import it.uniroma3.siw.model.TipoDiAnomalia;
import it.uniroma3.siw.model.Tratta;
import it.uniroma3.siw.model.Video;
import it.uniroma3.siw.repository.AcquirenteRepository;
import it.uniroma3.siw.service.AnomaliaService;
import it.uniroma3.siw.service.TrattaService;
import it.uniroma3.siw.service.VideoService;
import it.uniroma3.siw.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class AlertRestController {

    @Autowired
    private AnomaliaService anomaliaService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserService userService;

    @Autowired
    private TrattaService trattaService;

    // Mappa Stringa Gravità (Python) su Intero Gravità (DB: 1-5)
    private static final Map<String, Integer> SEVERITY_MAP = new HashMap<>();
    static {
        SEVERITY_MAP.put("CRITICA", 5);
        SEVERITY_MAP.put("ALTA", 4);
        SEVERITY_MAP.put("MEDIO", 3);
        SEVERITY_MAP.put("BASSO", 2);
        SEVERITY_MAP.put("UNKNOWN", 1);
    }

    // ❗ Alias tra le label usate dalla pipeline Python (solo YOLO e background
    // subtraction) e i valori dell'enum TipoDiAnomalia, per i casi in cui i nomi
    // non possono coincidere per costruzione (le classi YOLO sono in inglese,
    // l'enum e' in italiano). Le label del modulo DeepLab (PALO_INCLINATO,
    // VEGETAZIONE_INVASIVA, SCARTAMENTO_ANOMALO) ora combaciano direttamente
    // con l'enum e non necessitano di alias.
    private static final Map<String, TipoDiAnomalia> LABEL_ALIAS = new HashMap<>();
    static {
        //--- Modulo DeepLab ---
        LABEL_ALIAS.put("SCARTAMENTO_ANOMALO", TipoDiAnomalia.DILATAZIONE_FERROVIA);
        LABEL_ALIAS.put("VEGETAZIONE_INVASIVA", TipoDiAnomalia.VEGETAZIONE_VICINA);
        // --- Modulo YOLO (ostacoli, label = nome classe COCO in maiuscolo) ---
        LABEL_ALIAS.put("PERSON", TipoDiAnomalia.PERSONA_SUI_BINARI);
        LABEL_ALIAS.put("DOG", TipoDiAnomalia.ANIMALE_SUI_BINARI);
        LABEL_ALIAS.put("CAT", TipoDiAnomalia.ANIMALE_SUI_BINARI);
        LABEL_ALIAS.put("COW", TipoDiAnomalia.ANIMALE_SUI_BINARI);
        LABEL_ALIAS.put("HORSE", TipoDiAnomalia.ANIMALE_SUI_BINARI);
        LABEL_ALIAS.put("SHEEP", TipoDiAnomalia.ANIMALE_SUI_BINARI);
        LABEL_ALIAS.put("BIRD", TipoDiAnomalia.ANIMALE_SUI_BINARI);
        LABEL_ALIAS.put("CAR", TipoDiAnomalia.VEICOLO);
        LABEL_ALIAS.put("TRUCK", TipoDiAnomalia.VEICOLO);
        LABEL_ALIAS.put("BUS", TipoDiAnomalia.VEICOLO);
        LABEL_ALIAS.put("MOTORCYCLE", TipoDiAnomalia.VEICOLO);
        LABEL_ALIAS.put("BICYCLE", TipoDiAnomalia.VEICOLO);

        // --- Modulo background subtraction ---
        LABEL_ALIAS.put("ANOMALIA_STRUTTURALE", TipoDiAnomalia.ALTRO); // esplicito: nessuna label piu' specifica calza
    }

    // Mappa Label Stringa (Python) su Enum Java in modo robusto
    private TipoDiAnomalia mapLabelToEnum(String label) {
        String normalizedLabel = label.toUpperCase().replace(" ", "_");

        // 1. prova prima l'alias esplicito (copre i casi in cui il nome Python
        //    non coincide esattamente col nome dell'enum)
        if (LABEL_ALIAS.containsKey(normalizedLabel)) {
            return LABEL_ALIAS.get(normalizedLabel);
        }

        // 2. fallback: prova la corrispondenza automatica diretta (utile per
        //    label che gia' coincidono esattamente, es. PALO_INCLINATO,
        //    DERAGLIAMENTO, ecc., senza dover elencare ogni singolo caso sopra)
        try {
            return TipoDiAnomalia.valueOf(normalizedLabel);
        } catch (IllegalArgumentException e) {
            System.err.println("WARN: Tipo anomalia non trovato ne' in LABEL_ALIAS ne' nell'enum per label: " + label);
            return TipoDiAnomalia.ALTRO;
        }
    }


    @PostMapping
    public ResponseEntity<String> riceviAllerta(@RequestBody AlertDtoController payload) {
        try {
            if (payload == null || payload.label == null) {
                return ResponseEntity.badRequest().body("Payload alert mancante o incompleto.");
            }

            // Mappatura Video (Logica semplificata: cerca video per ID, se non fornito, video rimane null)
            Video video = null;

            // Creazione Anomalia
            Anomalia a = new Anomalia();

            // 1. Mappatura Tipo e Descrizione
            TipoDiAnomalia tipoAnomalia = mapLabelToEnum(payload.label);
            a.setTipoAnomalia(tipoAnomalia);
            a.setDescrizione("IA: " + payload.label);
            a.setVideo(video);

            // 2. Mappatura Gravità (Stringa e Intero)
            String severity = payload.severity != null ? payload.severity.toUpperCase() : "UNKNOWN";
            a.setGravitaString(severity);
            a.setGravita(SEVERITY_MAP.getOrDefault(severity, 1));

            // 3. Mappatura Dettagli Tecnici
            a.setDettagliTecnici(payload.details);

            // 4. Mappatura Campi IA
            if (payload.conf != null) a.setConfidenza(payload.conf.floatValue());
            if (payload.frame_idx != null) a.setFrameIndex(payload.frame_idx);
            if (payload.time_s != null) a.setTimeSeconds(payload.time_s.floatValue());
            a.setSorgenteVideoIA(payload.source_video);

            // 5. Mappatura Bounding Box
            if (payload.bbox != null) {
                a.setX(payload.bbox.x);
                a.setY(payload.bbox.y);
                a.setW(payload.bbox.w);
                a.setH(payload.bbox.h);
                // Aggiungi area alla descrizione per visibilità
                if (payload.area != null) a.setDescrizione(a.getDescrizione() + " | Area:" + payload.area);
            }

            // 6. ❗ NUOVO: decodifica l'immagine del frame (JPEG in base64) se presente.
            // Un base64 malformato non deve far fallire il salvataggio dell'anomalia:
            // in quel caso si prosegue semplicemente senza immagine.
            if (payload.getFrameB64() != null && !payload.getFrameB64().isEmpty()) {
                try {
                    byte[] frameBytes = Base64.getDecoder().decode(payload.getFrameB64());
                    a.setFrameImage(frameBytes);
                } catch (IllegalArgumentException e) {
                    System.err.println("WARN: frame_b64 non decodificabile, anomalia salvata senza immagine: " + e.getMessage());
                }
            }

            // 7. Assegnazione Entità
            if(payload.trattaId != null){
                Tratta tratta = trattaService.getById(payload.trattaId);
                a.setTratta(tratta);
            }
            a.setVideo(video);

            anomaliaService.saveFromAI(a, severity, payload.getChatId());

            return ResponseEntity.status(HttpStatus.CREATED).body("Anomalia salvata (ed eventualmente mandata su Telegram) correttamente: " + tipoAnomalia.name());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore salvataggio anomalia: " + e.getMessage());
        }
    }
}