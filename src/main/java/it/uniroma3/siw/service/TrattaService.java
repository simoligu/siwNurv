package it.uniroma3.siw.service;

import it.uniroma3.siw.model.TipoDiAnomalia;
import it.uniroma3.siw.model.Tratta;
import it.uniroma3.siw.model.User;
import it.uniroma3.siw.repository.TrattaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@Service
public class TrattaService {

    @Autowired
    private TrattaRepository trattaRepository;

    @Transactional
    public Iterable<Tratta> getAll() {
        return this.trattaRepository.findAll();
    }

    @Transactional
    public Tratta getById(Long id) {
        return this.trattaRepository.findById(id).orElse(null);
    }

    @Transactional
    public Tratta save(Tratta tratta) {
        return this.trattaRepository.save(tratta);
    }

    @Transactional
    public void delete(Long trattaId) {
        this.trattaRepository.deleteById(trattaId);
    }

    @Transactional
    public List<Tratta> getFilteredSorted(String sort, String nome, TipoDiAnomalia anomalia) {
        List<Tratta> tratte = (List<Tratta>) this.trattaRepository.findByCriteria(nome, (anomalia != null) ? anomalia.name() : null);
        if (sort != null) {
            switch (sort) {
                case "A-Z":
                    tratte.sort(Comparator.comparing(
                            Tratta::getNome,
                            Comparator.nullsLast(String::compareTo)
                    ));
                    break;

                case "Z-A":
                    tratte.sort(Comparator.comparing(
                            Tratta::getNome,
                            Comparator.nullsLast(String::compareTo)
                    ).reversed());
                    break;

                case "moreAnomalie":
                    tratte.sort(Comparator.comparing(
                            (Tratta tratta) -> tratta.getVideoAssociati() == null ? 0 :
                                    tratta.getVideoAssociati()
                                            .stream()
                                            .mapToInt(video -> video.getAnomalie() == null ? 0 : video.getAnomalie().size())
                                            .sum()
                    ).reversed());
                    break;

                case "lessAnomalie":
                    tratte.sort(Comparator.comparing(
                            (Tratta tratta) -> tratta.getVideoAssociati() == null ? 0 :
                                    tratta.getVideoAssociati()
                                            .stream()
                                            .mapToInt(video -> video.getAnomalie() == null ? 0 : video.getAnomalie().size())
                                            .sum()
                    ));
                    break;

                case "moreVideos":
                    tratte.sort(Comparator.comparing(
                            (Tratta tratta) -> tratta.getVideoAssociati() == null ? 0 : tratta.getVideoAssociati().size()
                    ).reversed());
                    break;

                case "lessVideos":
                    tratte.sort(Comparator.comparing(
                            (Tratta tratta) -> tratta.getVideoAssociati() == null ? 0 : tratta.getVideoAssociati().size()
                    ));
                    break;

                default:
                    break;
            }
        }

        return tratte;
    }

    @Transactional
    public Tratta getByName(String name) {
        return this.trattaRepository.getByNomeIgnoreCaseSpaceInsensitive(name.replace(" ", "").toLowerCase());
    }

    @Transactional
    public Tratta getByNomeVideo(String nomeVideo) {
        // rimuove estensione .mp4 se presente
        String nome = nomeVideo.replace(".mp4", "").replace(".MP4", "");
        return trattaRepository.findByNomeVideo(nome);
    }

    @Transactional
    public Tratta getBySupervisor(User supervisor) {
        return trattaRepository.findBySupervisor(supervisor);
    }

    /** Tutte le tratte su cui l'utente e' assegnato come operatore. */
    @Transactional
    public List<Tratta> getAllByOperatore(User operatore){
        return trattaRepository.findAllByOperatore(operatore);
    }

    /**
     * La prima tratta dell'operatore, o null se non ne ha.
     * Conservato per le pagine che ne mostrano una sola; a differenza della
     * versione precedente non solleva eccezione se l'operatore ne ha piu' di una.
     */
    @Transactional
    public Tratta getByOperatore(User operatore){
        List<Tratta> tratte = trattaRepository.findAllByOperatore(operatore);
        return (tratte == null || tratte.isEmpty()) ? null : tratte.get(0);
    }

}

