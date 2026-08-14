package it.uniroma3.siw.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TelegramService {

    private final TelegramBotListener botListener;

    // Inietto il listener per usare il suo metodo di invio
    public TelegramService(TelegramBotListener botListener) {
        this.botListener = botListener;
    }

    /**
     * Versione originale, invariata: invia solo testo. Mantenuta per
     * compatibilita' con eventuali chiamate esistenti senza immagine.
     */
    @Async
    public void inviaAlertCritico(String targetChatId, String tipoAnomalia, String severita, String dettagli, String videoSorgente) {
        inviaAlertCritico(targetChatId, tipoAnomalia, severita, dettagli, videoSorgente, null);
    }

    /**
     * ❗ NUOVO: overload che accetta i byte dell'immagine del frame. Se
     * presente, invia una SendPhoto con didascalia (via TelegramBotListener.
     * inviaFoto); altrimenti ricade sul comportamento originale a solo testo.
     */
    @Async
    public void inviaAlertCritico(String targetChatId, String tipoAnomalia, String severita, String dettagli,
                                  String videoSorgente, byte[] frameImage) {
        if (targetChatId == null || targetChatId.isEmpty()) return;

        String testo = String.format(
                "🚨 *ALERT SISTEMA NURV* 🚨\n\n" +
                        "📌 *Anomalia:* %s\n" +
                        "⚠️ *Severità:* %s\n" +
                        "🎬 *Video:* %s\n" +
                        "📝 *Dettagli:* %s\n\n" +
                        "👉 _Accedere alla Central Control Station per validare il report._",
                tipoAnomalia.replace("_", " "), severita, videoSorgente, dettagli
        );

        if (frameImage != null && frameImage.length > 0) {
            botListener.inviaFoto(targetChatId, testo, frameImage);
            System.out.println("✅ [Telegram] Alert con foto inviato con successo!");
        } else {
            botListener.inviaRisposta(targetChatId, testo);
            System.out.println("✅ [Telegram] Alert inviato con successo!");
        }
    }
}