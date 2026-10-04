package it.uniroma3.siw.controller;

import it.uniroma3.siw.model.Anomalia;
import it.uniroma3.siw.model.Credentials;
import it.uniroma3.siw.model.Tratta;
import it.uniroma3.siw.model.User;
import it.uniroma3.siw.service.AnomaliaService;
import it.uniroma3.siw.service.TrattaService;
import it.uniroma3.siw.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class IAController {

    @Autowired
    private AnomaliaService anomaliaService;

    @Autowired
    private UserService userService; // <-- aggiungi questa riga

    @Autowired
    private TrattaService trattaService;

    @GetMapping("/ia/alerts")
    public String getIAAlerts(Model model) {
        // L'amministratore consulta l'intero insieme delle segnalazioni;
        // supervisor e operatori vedono solo quelle delle proprie tratte.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Credentials.ADMIN_ROLE));
        User user = userService.getCurrentUser();

        Iterable<Anomalia> anomalie = anomaliaService.getAll();
        if (!isAdmin) {
            List<Anomalia> visibili = new ArrayList<>();
            for (Anomalia a : anomalie) {
                Tratta tratta = a.getTratta();
                if (tratta == null && a.getVideo() != null) {
                    tratta = a.getVideo().getTratta();
                }
                if (trattaService.isDiCompetenza(user, tratta)) {
                    visibili.add(a);
                }
            }
            anomalie = visibili;
        }

        model.addAttribute("anomalie", anomalie);
        model.addAttribute("user", user); // ora funziona
        return "ia/alerts";
    }
}
