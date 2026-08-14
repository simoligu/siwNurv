package it.uniroma3.siw.controller;

import com.fasterxml.jackson.annotation.JsonProperty;

// Classe per rappresentare il Bounding Box
class BBoxDto {
    public Integer x;
    public Integer y;
    public Integer w;
    public Integer h;
    // Getters/Setters omessi per brevità, ma necessari in un vero progetto
}

// Data Transfer Object per ricevere l'alert dal Python
public class AlertDtoController {
    public String project;
    public Integer frame_idx;
    public Double time_s;
    public BBoxDto bbox;
    public Integer area;
    public String label;
    public Double conf;
    // ❗ Campi critici per le anomalie strutturali:
    public String severity; // CRITICA, MEDIO, BASSO
    public String details;  // Dettagli tecnici
    public String source_video;
    public Long videoId; // Opzionale
    public Long trattaId;
    public String chatId;

    // ❗ NUOVO: immagine del frame allegata dalla pipeline Python (JPEG in base64).
    // Il campo Python si chiama "frame_b64" (snake_case); @JsonProperty mappa
    // quel nome JSON sulla variabile Java frameB64 senza dover snaturare le
    // convenzioni di naming Java.
    @JsonProperty("frame_b64")
    public String frameB64;

    // Getters/Setters omessi per brevità
    public void setChatId(String chatId){ this.chatId = chatId;}
    public String getChatId(){ return chatId;}

    public String getFrameB64(){ return frameB64; }
    public void setFrameB64(String frameB64){ this.frameB64 = frameB64; }

}