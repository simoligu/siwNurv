package it.uniroma3.siw.controller;

import it.uniroma3.siw.controller.validator.TrattaValidator;
import it.uniroma3.siw.model.Tratta;
import it.uniroma3.siw.model.User;
import it.uniroma3.siw.service.TrattaService;
import it.uniroma3.siw.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class TrattaController {

    @Autowired
    private TrattaService trattaService;

    @Autowired
    private UserService userService;
    @Autowired
    private TrattaValidator trattaValidator;

    /**
     * Vero se l'utente autenticato ha il ruolo di amministratore.
     * L'amministratore gestisce l'anagrafica delle tratte, quindi le consulta
     * tutte; gli altri ruoli solo quelle di cui sono responsabili.
     */
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }

    /**
     * Chi puo' aprire la pagina di una tratta: l'amministratore, il supervisor
     * di quella tratta e gli operatori che vi sono assegnati. Essere
     * autenticati non basta: la pagina espone i video e le anomalie della
     * tratta, che sono dati di un'altra utenza.
     */
    private boolean puoConsultare(User utente, Tratta tratta) {
        if (tratta == null || utente == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        Tratta trattaSupervisor = trattaService.getBySupervisor(utente);
        if (trattaSupervisor != null && trattaSupervisor.getId().equals(tratta.getId())) {
            return true;
        }
        List<Tratta> tratteOperatore = trattaService.getAllByOperatore(utente);
        if (tratteOperatore != null) {
            for (Tratta t : tratteOperatore) {
                if (t.getId().equals(tratta.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    @GetMapping("/tratta/{id}")
    public String dettaglioTratta(@PathVariable Long id,
                                  Model model) {
        Tratta tratta = trattaService.getById(id);
        if (tratta == null) {
            return "redirect:/";
        }
        User currentUser = userService.getCurrentUser();
        if (!puoConsultare(currentUser, tratta)) {
            return "redirect:/accessDenied";
        }
        model.addAttribute("user", currentUser);
        model.addAttribute("tratta", tratta);
        return "user/tratta";
    }

    @GetMapping("/tratte")
    public String tratte(Model model) {
        // L'elenco completo delle tratte e' uno strumento di amministrazione:
        // agli altri ruoli mostrerebbe voci che poi non possono aprire.
        if (!isAdmin()) {
            return "redirect:/accessDenied";
        }
        model.addAttribute("user", userService.getCurrentUser());
        model.addAttribute("tratte", trattaService.getAll());
        return "user/index";
    }

    @GetMapping("/admin/addTratta")
    public String addTratta(Model model) {
        model.addAttribute("user", userService.getCurrentUser());
        model.addAttribute("tratta", new Tratta());
        return "user/admin/formNewTratta";
    }

    @PostMapping("/admin/addTratta")
    public String saveTratta(@Valid @ModelAttribute Tratta tratta,
                             BindingResult bindingResult,
                             Model model) {
        trattaValidator.validate(tratta, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getCurrentUser());
            model.addAttribute("tratta", tratta);
            return "user/admin/formNewTratta";
        }
        model.addAttribute("user", userService.getCurrentUser());
        trattaService.save(tratta);
        return "redirect:/tratta/" + tratta.getId();
    }

    @GetMapping("/admin/deleteTratta/{tratta_id}")
    public String deleteTratta(@PathVariable Long tratta_id) {
        trattaService.delete(tratta_id);
        return "redirect:/tratte";
    }
}


