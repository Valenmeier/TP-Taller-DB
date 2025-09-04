package org.example.dao;

import org.example.Db;
import org.example.model.Usuario;
import org.example.security.Passwords;

import java.sql.*;
import java.util.*;

public class UsuarioDAO {

    // CREATE
    public Usuario create(Usuario u) throws Exception {
        String sql = "INSERT INTO usuarios (usuario, contrasena, rol) VALUES (?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsuario());
            ps.setString(2, Passwords.hash(u.getContrasena()));
            ps.setString(3, u.getRol());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) u.setId(rs.getInt(1));
            }
            u.setContrasena(null);
            return u;
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new IllegalArgumentException("El usuario ya existe");
        }
    }


    // READ uno
    public Usuario findById(int id) throws Exception {
        String sql = "SELECT id, usuario, contrasena, rol FROM usuarios WHERE id=?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // READ todos
    public List<Usuario> findAll() throws Exception {
        String sql = "SELECT id, usuario, contrasena, rol FROM usuarios ORDER BY usuario";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Usuario> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        }
    }

    // UPDATE
    public boolean updatePerfil(Usuario u) throws Exception {
        String sql = "UPDATE usuarios SET usuario=?, rol=? WHERE id=?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getUsuario());
            ps.setString(2, u.getRol());
            ps.setInt(3, u.getId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updatePassword(int userId, String nuevaPlano) throws Exception {
        String sql = "UPDATE usuarios SET contrasena=? WHERE id=?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, Passwords.hash(nuevaPlano));
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    // DELETE
    public boolean delete(int id) throws Exception {
        String sql = "DELETE FROM usuarios WHERE id=?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    // LOGIN (simple)
    public Usuario findByUsuarioYContrasena(String usuario, String contrasenaPlano) throws Exception {
        String sql = "SELECT id, usuario, contrasena, rol FROM usuarios WHERE usuario = ?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("contrasena");
                    if (org.example.security.Passwords.verify(contrasenaPlano, hash)) {
                        return new Usuario(
                                rs.getInt("id"),
                                rs.getString("usuario"),
                                null,
                                rs.getString("rol")
                        );
                    }
                }
            }
        }
        return null;
    }


    private Usuario map(ResultSet rs) throws Exception {
        return new Usuario(
                rs.getInt("id"),
                rs.getString("usuario"),
                rs.getString("contrasena"),
                rs.getString("rol")
        );
    }
    private Usuario mapSafe(ResultSet rs) throws Exception {
        return new Usuario(
                rs.getInt("id"),
                rs.getString("usuario"),
                null,
                rs.getString("rol")
        );
    }

}
