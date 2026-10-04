package it.uniroma3.siw.controller;

import it.uniroma3.siw.model.Credentials;
import it.uniroma3.siw.model.Tratta;
import it.uniroma3.siw.model.User;
import it.uniroma3.siw.service.CredentialsService;
import it.uniroma3.siw.service.TelegramBotListener;
import it.uniroma3.siw.service.TrattaService;
import it.uniroma3.siw.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SupervisorController {
    @Autowired private TrattaService trattaService;
    @Autowired private UserService userService;
    @Autowired private CredentialsService credentialsService;
    @Autowired private TelegramBotListener telegramBotListener;

    //Admin: pagina per assegnare supervisor a una tratta
    @GetMapping("/admin/tratta/{trattaId}/gestisciSupervisor")
    public String gestisciSupervisor(@PathVariable Long trattaId, Model model){
        model.addAttribute("tratta", trattaService.getById(trattaId));
        model.addAttribute("utenti", userService.getAllDefaultUsers());
        model.addAttribute("user", userService.getCurrentUser());
        return "user/admin/gestisciSupervisor";
    }

    @PostMapping("/admin/tratta/{trattaId}/assegnaSupervisor")
    public String assegnaSupervisor(@PathVariable Long trattaId, @RequestParam Long userId){
        Tratta tratta = trattaService.getById(trattaId);
        User nuovoSupervisor = userService.getUserById(userId);

        //Promuovi l'utente a supervisor
        Credentials cred = credentialsService.getCredentialsByUser(nuovoSupervisor);
        cred.setRole(Credentials.SUPERVISOR_ROLE);
        credentialsService.save(cred);

        tratta.setSupervisor(nuovoSupervisor);
        trattaService.save(tratta);

        return "redirect:/admin/tratta/" + trattaId + "/gestisciSupervisor";
    }

    @PostMapping("/admin/tratta/{trattaId}/rimuoviSupervisor")
    public String rimuoviSupervisor(@PathVariable Long trattaId){
        Tratta tratta = trattaService.getById(trattaId);
        if(tratta.getSupervisor()!=null){
            //retrocedi a DEFAULT
            Credentials cred = credentialsService.getCredentialsByUser(tratta.getSupervisor());
            cred.setRole(Credentials.DEFAULT_ROLE);
            credentialsService.save(cred);
            tratta.setSupervisor(null);
            trattaService.save(tratta);
        }
        return "redirect:/admin/tratta/" + trattaId + "/gestisciSupervisor";
    }

    //SUPERVISOR: pagina configurazione Telegram
    @GetMapping("/supervisor/configuraTelegram")
    public String configuraTelegram(Model model){
        User currentUser = userService.getCurrentUser();
        Tratta tratta = trattaService.getBySupervisor(currentUser);
        model.addAttribute("tratta", tratta);
        model.addAttribute("nomeBot", telegramBotListener.getNomePubblico());
        model.addAttribute("user", currentUser);
        model.addAttribute("utentiDisponibili", userService.getAllDefaultUsers());
        return "user/supervisor/configuraTelegram";
    }
    @PostMapping("/supervisor/configuraTelegram")
    public String salvaTelegram(@RequestParam String chatId, @RequestParam String inviteLink, @RequestParam Long trattaId){
        Tratta tratta = trattaService.getById(trattaId);
        tratta.setTelegramChatId(chatId);
        tratta.setTelegramInviteLink(inviteLink);
        trattaService.save(tratta);
        return "redirect:/supervisor/configuraTelegram?success=true";
    }

    //metodi per aggiungere e rimuovere operatori
    @PostMapping("/supervisor/tratta/{trattaId}/aggiungiOperatore")
    public String aggiungiOperatore(@PathVariable Long trattaId, @RequestParam Long userId, RedirectAttributes redirectAttributes){
        Tratta tratta = trattaService.getById(trattaId);
        User operatore = userService.getUserById(userId);
        if(tratta.getOperatori().contains(operatore)) {
            redirectAttributes.addFlashAttribute("alertMsg", "Questo operatore è già assegnato a questa tratta.");
            redirectAttributes.addFlashAttribute("alertType", "warning");
        }else{
            tratta.getOperatori().add(operatore);
            trattaService.save(tratta);
            redirectAttributes.addFlashAttribute("alertMsg", "Operatore aggiunto con successo alla tratta!");
            redirectAttributes.addFlashAttribute("alertType", "success");
        }
        return "redirect:/supervisor/configuraTelegram";
    }

    @PostMapping("/supervisor/tratta/{trattaId}/rimuoviOperatore")
    public String rimuoviOperatore(@PathVariable Long trattaId, @RequestParam Long userId, RedirectAttributes redirectAttributes){
        Tratta tratta = trattaService.getById(trattaId);
        User operatore = userService.getUserById(userId);
        if(!tratta.getOperatori().contains(operatore)) {
            redirectAttributes.addFlashAttribute("alertMsg", "Questo operatore non è assegnato a questa tratta.");
            redirectAttributes.addFlashAttribute("alertType", "warning");
        }else{
            tratta.getOperatori().remove(operatore);
            trattaService.save(tratta);
            redirectAttributes.addFlashAttribute("alertMsg", "Operatore rimosso con successo!");
            redirectAttributes.addFlashAttribute("alertType", "success");
        }
        return "redirect:/supervisor/configuraTelegram";
    }
}
