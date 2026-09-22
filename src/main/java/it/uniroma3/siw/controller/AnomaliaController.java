package it.uniroma3.siw.controller;

import it.uniroma3.siw.controller.validator.AnomaliaValidator;
import it.uniroma3.siw.model.*;
import it.uniroma3.siw.service.AnomaliaService;
import it.uniroma3.siw.service.TrattaService;
import it.uniroma3.siw.service.UserService;
import it.uniroma3.siw.service.VideoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AnomaliaController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private AnomaliaService anomaliaService;
    @Autowired
    private UserService userService;
    @Autowired
    private AnomaliaValidator anomaliaValidator;
    @Autowired
    private TrattaService trattaService;

    @GetMapping("/video/{videoId}/addAnomalia")
    public String aggiungiAnomalia(@PathVariable Long videoId, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSupervisor = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("SUPERVISOR"));
        if (isSupervisor) {
            return "redirect:/";
        }
        model.addAttribute("anomalia", new Anomalia());
        model.addAttribute("video_id", videoId);
        model.addAttribute("user", userService.getCurrentUser());
        return "user/formNewAnomalia";
    }

    @PostMapping("/video/{videoId}/addAnomalia")
    public String salvaAnomalia(@PathVariable Long videoId,
                                @RequestParam(required = false) TipoDiAnomalia tipoAnomalia,
                                @Valid @ModelAttribute Anomalia anomalia,
                                BindingResult bindingResult,
                                Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSupervisor = auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("SUPERVISOR"));
        if(isSupervisor){
            return "redirect:/";
        }

        anomalia.setTipoAnomalia(tipoAnomalia); //la setto prima così posso validarla

        anomaliaValidator.validate(anomalia, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getCurrentUser());
            model.addAttribute("anomalia", anomalia);
            model.addAttribute("video_id", videoId);
            return "user/formNewAnomalia";
        }
        Video video = videoService.getById(videoId);
        if (video == null) {
            return "redirect:/error"; // o pagina custom di errore
        }

        anomalia.setVideo(video);
        anomalia.setTratta(video.getTratta());
        anomalia.setUser(userService.getCurrentUser());

        anomaliaService.save(anomalia);

        return "redirect:/tratta/" + video.getTratta().getId();
    }

    @PostMapping("/anomalia/{anomalia_id}/updateRisolta")
    public String aggiornaRisolta(
            @PathVariable Long anomalia_id,
            @RequestParam(value = "risolta", required = false) Boolean risolta) {
        Anomalia anomalia = anomaliaService.getById(anomalia_id);
        if (anomalia == null) {
            return "redirect:/";
        }
        User currentUser = userService.getCurrentUser();

        // Il diritto di chiudere una segnalazione segue la responsabilita'
        // operativa sulla tratta, non il livello di privilegio nel sistema:
        // puo' risolvere un'anomalia solo chi e' supervisor o operatore della
        // tratta a cui quell'anomalia appartiene. Un amministratore, che
        // fornisce il servizio ma non presidia la linea, non ha una tratta
        // propria e ricade quindi nel caso generale: non e' abilitato a questa
        // operazione, senza bisogno di un'eccezione dedicata.
        Tratta trattaAnomalia = anomalia.getTratta();
        Tratta trattaSupervisor = trattaService.getBySupervisor(currentUser);
        Tratta trattaOperatore = trattaService.getByOperatore(currentUser);

        boolean appartieneAllaSuaTratta = (trattaAnomalia!=null) && ((trattaSupervisor != null && trattaSupervisor.getId().equals(trattaAnomalia.getId())) || (trattaOperatore!=null && trattaOperatore.getId().equals(trattaAnomalia.getId())));
        if(!appartieneAllaSuaTratta){
            return "redirect:/accessDenied";
        }

        boolean nuovoStato = Boolean.TRUE.equals(risolta);
        anomalia.setRisolta(nuovoStato); // imposta false se null

        if(nuovoStato){
            anomalia.setRisoltaDa(currentUser);
            anomalia.setDataRisoluzione(java.time.LocalDateTime.now());
        }else{
            anomalia.setRisoltaDa(null);
            anomalia.setDataRisoluzione(null);
        }
        anomaliaService.save(anomalia);
        if(anomalia.getTratta()!=null){
            return "redirect:/tratta/" +anomalia.getTratta().getId() + "#listaAnomalie";
        }
        if(anomalia.getVideo()!=null && anomalia.getVideo().getTratta()!=null){
            return "redirect:/tratta/" +anomalia.getVideo().getTratta().getId() + "#listaAnomalie";
        }
        return "redirect:/ia/alerts";
    }

    /**
     * ❗ NUOVO: serve l'immagine del frame allegata a un'anomalia (se presente).
     * Stesso pattern gia' usato per VideoController.streamVideo e
     * UserController.getImage: restituisce direttamente i byte con il
     * Content-Type corretto, cosi' si puo' usare come src di un <img> in HTML,
     * es: <img th:src="@{/anomalia/{id}/frame(id=${anomalia.id})}" />
     */
    @GetMapping("/anomalia/{id}/frame")
    public ResponseEntity<byte[]> getFrameImage(@PathVariable Long id) {
        Anomalia anomalia = anomaliaService.getById(id);
        if (anomalia == null || anomalia.getFrameImage() == null || anomalia.getFrameImage().length == 0) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(anomalia.getFrameImage(), headers, HttpStatus.OK);
    }

}