package co.edu.sena.cimm.eventos.model;

public class Asistente {

    private Long id;
    private String nombreCompleto;
    private String documento;
    private String email;
    private String telefono;

    public Asistente() {
    }

    public Asistente(Long id, String nombreCompleto, String documento,
                     String email, String telefono) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.documento = documento;
        this.email = email;
        this.telefono = telefono;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}
