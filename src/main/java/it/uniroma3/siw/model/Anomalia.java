package it.uniroma3.siw.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

import java.util.Objects;

// Import necessari per JPA e Validazione (preservati)

@Entity
public class Anomalia {

	// === Campi IA esistenti ===
	private Integer frameIndex;
	private Float timeSeconds;
	private Integer x;
	private Integer y;
	private Integer w;
	private Integer h;
	private Float confidenza;
	private String sorgenteVideoIA;

	// ❗ NUOVI CAMPI AGGIUNTI PER L'IA STRUTTURALE
	// Campo per la Gravità testuale (es. CRITICA, MEDIO, BASSO)
	private String gravitaString; // Tutto attaccato, senza spazi


	// Campo per i dettagli tecnici (es. misurazione dilatazione)
	@Column(length = 500)
	private String dettagliTecnici;

	// ❗ NUOVO: immagine del frame al momento dell'anomalia, inviata dalla
	// pipeline Python (JPEG, gia' decodificato da base64 nel controller).
	// Stesso pattern gia' usato per Video.file e Image.content (bytea).
	@Column(columnDefinition = "bytea")
	private byte[] frameImage;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private boolean risolta = false;

	@Min(value = 1, message = "Inserisci un valore maggiore o uguale a 0")
	@Max(value = 5, message = "Inserisci un valore minore o uguale a 5")
	private int gravita;

	@NotBlank(message = "Inserisci una descrizione")
	@Length(max = 100)
	private String descrizione;

	@Enumerated(EnumType.STRING)
	@Column(nullable=true)
	private TipoDiAnomalia tipoAnomalia;

	@ManyToOne
	private User user;

	@ManyToOne
	private Video video;

	@ManyToOne
	private Acquirente acquirente;

	@ManyToOne
	private Tratta tratta;

	@ManyToOne
	private User risoltaDa;

	private java.time.LocalDateTime dataRisoluzione;

	// ------------------------------------
	// METODI equals() e hashCode() ORIGINALI
	// ------------------------------------
	@Override
	public int hashCode() {
		return Objects.hash(gravita, tipoAnomalia, video);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Anomalia other = (Anomalia) obj;
		return gravita == other.gravita && tipoAnomalia == other.tipoAnomalia && Objects.equals(video, other.video);
	}
	// ------------------------------------


	// --- Getters e Setters ---

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public boolean getRisolta() { return risolta; }
	public void setRisolta(boolean risolta) { this.risolta = risolta; }

	public int getGravita() { return gravita; }
	public void setGravita(int gravita) { this.gravita = gravita; }

	public String getGravitaString() { return gravitaString; }
	public void setGravitaString(String gravitaString) { this.gravitaString = gravitaString; }

	public String getDescrizione() { return descrizione; }
	public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

	public String getDettagliTecnici() { return dettagliTecnici; }
	public void setDettagliTecnici(String dettagliTecnici) { this.dettagliTecnici = dettagliTecnici; }

	public TipoDiAnomalia getTipoAnomalia() { return tipoAnomalia; }
	public void setTipoAnomalia(TipoDiAnomalia tipoAnomalia) { this.tipoAnomalia = tipoAnomalia; }

	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }

	public Video getVideo() { return video; }
	public void setVideo(Video video) { this.video = video; }

	public Integer getFrameIndex() { return frameIndex; }
	public void setFrameIndex(Integer frameIndex) { this.frameIndex = frameIndex; }

	public Float getTimeSeconds() { return timeSeconds; }
	public void setTimeSeconds(Float timeSeconds) { this.timeSeconds = timeSeconds; }

	public Integer getX() { return x; }
	public void setX(Integer x) { this.x = x; }

	public Integer getY() { return y; }
	public void setY(Integer y) { this.y = y; }

	public Integer getW() { return w; }
	public void setW(Integer w) { this.w = w; }

	public Integer getH() { return h; }
	public void setH(Integer h) { this.h = h; }

	public Float getConfidenza() { return confidenza; }
	public void setConfidenza(Float confidenza) { this.confidenza = confidenza; }

	public String getSorgenteVideoIA() { return sorgenteVideoIA; }
	public void setSorgenteVideoIA(String sorgenteVideoIA) { this.sorgenteVideoIA = sorgenteVideoIA; }

	public Acquirente getAcquirente(){ return acquirente; }
	public void setAcquirente(Acquirente acquirente){ this.acquirente = acquirente; }

	public Tratta getTratta() { return tratta; }
	public void setTratta(Tratta tratta) { this.tratta = tratta; }

	public User getRisoltaDa(){return risoltaDa;}
	public void setRisoltaDa(User risoltaDa){this.risoltaDa = risoltaDa;}

	public java.time.LocalDateTime getDataRisoluzione(){return dataRisoluzione;}
	public void setDataRisoluzione(java.time.LocalDateTime dataRisoluzione){this.dataRisoluzione = dataRisoluzione;}

	public byte[] getFrameImage(){ return frameImage; }
	public void setFrameImage(byte[] frameImage){ this.frameImage = frameImage; }
}