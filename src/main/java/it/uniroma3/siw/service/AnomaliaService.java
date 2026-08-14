package it.uniroma3.siw.service;

import it.uniroma3.siw.model.Anomalia;
import it.uniroma3.siw.model.Tratta;
import it.uniroma3.siw.model.Video;
import it.uniroma3.siw.repository.AnomaliaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class AnomaliaService {
    @Autowired
    private AnomaliaRepository anomaliaRepository;

    @Autowired
    private TelegramService telegramService; // Il servizio Telegram

    @Autowired
    private TrattaService trattaService;

    @Autowired
    private VideoService videoService;

    @Transactional
    public void save(Anomalia anomalia) {
        anomaliaRepository.save(anomalia);
    }

    /*
     *Salva l'anomalia inviata dall'IA nel database e valuta se inoltrarla nel canale Telegram
     */
    @Transactional
    public void saveFromAI(Anomalia anomalia, String severitaOriginale, String chatIdDalPayload){
        Tratta tratta = anomalia.getTratta();

        if(tratta == null && anomalia.getSorgenteVideoIA()!=null){
            tratta = trattaService.getByNomeVideo(anomalia.getSorgenteVideoIA());
            anomalia.setTratta(tratta);
        }
        if(tratta!=null && anomalia.getSorgenteVideoIA()!=null){
            Video video = videoService.getByNomeInTratta(anomalia.getSorgenteVideoIA(),tratta);
            if(video!=null){
                anomalia.setVideo(video);
            }
        }
        anomaliaRepository.save(anomalia);

        //3. Logica di filtraggio proattivo: inviamo su Telegram solo i report più urgenti
        if("CRITICA".equalsIgnoreCase(severitaOriginale) || "ALTA".equalsIgnoreCase(severitaOriginale)) {
            String chatId= null;
            if(tratta!=null && tratta.getTelegramChatId()!=null){
                chatId = tratta.getTelegramChatId();
            }
            if(chatId!=null && !chatId.isEmpty()){

                // ❗ NUOVO: passa anche l'immagine del frame (se presente) al
                // service Telegram, che decidera' se mandare SendPhoto o solo
                // testo in base alla presenza dei byte.
                telegramService.inviaAlertCritico(
                        chatId,
                        (anomalia.getTipoAnomalia()!=null ? anomalia.getTipoAnomalia().toString() : "Generic"),
                        severitaOriginale.toUpperCase(),
                        anomalia.getDettagliTecnici(),
                        anomalia.getSorgenteVideoIA(),
                        anomalia.getFrameImage()
                );
            }
        }
    }

    @Transactional
    public Anomalia getById(Long anomaliaId) {
        return anomaliaRepository.findById(anomaliaId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Iterable<Anomalia> getAll() {
        return anomaliaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Anomalia> getByTratta(Tratta tratta){
        if(tratta==null) return java.util.Collections.emptyList();
        return anomaliaRepository.findByTrattaOrderByIdDesc(tratta);
    }


}