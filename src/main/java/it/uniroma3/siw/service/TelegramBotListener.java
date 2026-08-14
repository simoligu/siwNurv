package it.uniroma3.siw.service;

import it.uniroma3.siw.model.Acquirente;
import it.uniroma3.siw.repository.AcquirenteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.util.Optional;

@Component
public class TelegramBotListener extends TelegramLongPollingBot {

    private final AcquirenteRepository acquirenteRepository;
    private final String botToken;

    public TelegramBotListener(AcquirenteRepository acquirenteRepository,
                               @Value("${telegram.bot.token}") String botToken) {
        this.acquirenteRepository = acquirenteRepository;
        this.botToken = botToken;
    }

    @Override
    public String getBotUsername() {
        return "nurv_train_alert_bot";
    }

    @Override
    public String getBotToken() {
        return this.botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messaggio = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (messaggio.startsWith("/collega")) {
                gestisciCollegamento(messaggio, chatId);
            }
        }
    }

    private void gestisciCollegamento(String messaggio, Long chatId) {
        String[] parti = messaggio.split(" ");
        if (parti.length < 2) {
            inviaRisposta(chatId.toString(), "Errore: Inserire il codice. Es. /collega NURV-1234");
            return;
        }

        String codice = parti[1].trim().toUpperCase();
        Optional<Acquirente> acc = acquirenteRepository.findByCodiceAssociazione(codice);

        if (acc.isPresent()) {
            Acquirente a = acc.get();
            a.setTelegramChatId(chatId.toString());
            acquirenteRepository.save(a);
            inviaRisposta(chatId.toString(), "✅ Gruppo collegato a '" + a.getNome() + "'!");
        } else {
            inviaRisposta(chatId.toString(), "❌ Codice errato o inesistente.");
        }
    }

    public void inviaRisposta(String chatId, String testo) {
        try {
            execute(new SendMessage(chatId, testo));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * ❗ NUOVO: invia una foto con didascalia su Telegram.
     * Usato per gli alert critici che arrivano con un'immagine del frame
     * allegata dalla pipeline Python (campo frame_b64 -> byte[] frameImage
     * su Anomalia). Se l'invio fallisce (rete, token, chatId invalido), viene
     * loggato ma non propagato: un errore Telegram non deve mai far fallire
     * il salvataggio dell'anomalia nel DB, che avviene sempre prima.
     */
    public void inviaFoto(String chatId, String caption, byte[] fotoBytes) {
        try {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setCaption(caption);
            sendPhoto.setParseMode("Markdown");
            sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(fotoBytes), "alert.jpg"));
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}