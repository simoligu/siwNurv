package it.uniroma3.siw.repository;

import java.util.List;

import it.uniroma3.siw.model.TipoDiAnomalia;
import it.uniroma3.siw.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import it.uniroma3.siw.model.Tratta;
import org.springframework.data.repository.query.Param;

public interface TrattaRepository extends CrudRepository<Tratta, Long> {
    List<Tratta> findByNomeContainingIgnoreCase(String nome);

    Tratta findBySupervisor(User supervisor);

    @Query(value = """
            SELECT DISTINCT t.* 
            FROM tratta t
            WHERE 
                (:nome IS NULL OR LOWER(t.nome) LIKE LOWER(CONCAT(:nome, '%')))
                AND (
                    :anomalia IS NULL OR EXISTS (
                        SELECT 1
                        FROM video v
                        JOIN anomalia a ON a.video_id = v.id
                        WHERE v.tratta_id = t.id 
                        AND a.tipo_anomalia = :anomalia
                        AND a.risolta = false
                    )
                )
                ORDER BY t.nome ASC
            """, nativeQuery = true)
    Iterable<Tratta> findByCriteria(@Param("nome") String nome, @Param("anomalia") String anomalia);

    @Query(value = """
    SELECT t.* FROM tratta t
    JOIN video v ON v.tratta_id = t.id
    WHERE LOWER(REPLACE(v.nome, ' ', '')) = LOWER(REPLACE(:nomeVideo, ' ', ''))
    LIMIT 1
    """, nativeQuery = true)
    Tratta findByNomeVideo(@Param("nomeVideo") String nomeVideo);


    @Query(value = "SELECT * FROM tratta WHERE LOWER(REPLACE(nome, ' ', '')) LIKE :name", nativeQuery = true)
    Tratta getByNomeIgnoreCaseSpaceInsensitive(@Param("name") String name);

    // Restituisce una lista perche' Tratta.operatori e' @ManyToMany: lo stesso
    // utente puo' comparire fra gli operatori di piu' tratte. Con un solo
    // Tratta come tipo di ritorno, quel caso solleverebbe
    // IncorrectResultSizeDataAccessException invece di essere gestito.
    @Query("SELECT t FROM Tratta t JOIN t.operatori o WHERE o = :operatore")
    List<Tratta> findAllByOperatore(@Param("operatore") User operatore);

}
