package org.example.model;

public class Usuario {
    private Integer id;
    private String usuario;
    private String contrasena;
    private String rol;

    public Usuario(Integer id, String usuario, String contrasena, String rol) {
        this.id = id; this.usuario = usuario; this.contrasena = contrasena; this.rol = rol;
    }
    public Integer getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getContrasena() { return contrasena; }
    public String getRol() { return rol; }

    public void setId(Integer id) { this.id = id; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public void setRol(String rol) { this.rol = rol; }

    @Override public String toString() {
        return "Usuario{id=%s, usuario='%s', rol='%s'}".formatted(id, usuario, rol);
    }
}
