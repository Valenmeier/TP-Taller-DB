package org.example.dao;

import org.example.Db;
import org.example.model.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    // CREATE
    public Producto create(Producto p) throws Exception {
        String sql = "INSERT INTO productos (nombre, seccion, precio_kg) VALUES (?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getSeccion());
            ps.setBigDecimal(3, p.getPrecioKg());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setId(rs.getInt(1));
            }
        }
        return p;
    }

    // READ individual
    public Producto findById(int id) throws Exception {
        String sql = "SELECT id, nombre, seccion, precio_kg FROM productos WHERE id = ? AND activo = 1";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    // READ lista
    public List<Producto> findAll(String seccion) throws Exception {
        String base = "SELECT id, nombre, seccion, precio_kg FROM productos WHERE activo = 1";
        boolean filtrar = seccion != null && !seccion.isBlank();
        String sql = filtrar
                ? base + " AND UPPER(seccion) = UPPER(?) ORDER BY nombre"
                : base + " ORDER BY seccion, nombre";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filtrar) ps.setString(1, seccion);
            try (ResultSet rs = ps.executeQuery()) {
                List<Producto> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    // UPDATE
    public boolean update(Producto p) throws Exception {
        String sql = "UPDATE productos SET nombre=?, seccion=?, precio_kg=? WHERE id=? AND activo = 1";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getSeccion());
            ps.setBigDecimal(3, p.getPrecioKg());
            ps.setInt(4, p.getId());
            return ps.executeUpdate() == 1;
        }
    }

    // SOFT DELETE
    public boolean delete(int id) throws Exception {
        String sql = "UPDATE productos SET activo = 0 WHERE id = ? AND activo = 1";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    // Reactivar un producto desactivado
    public boolean reactivate(int id) throws Exception {
        String sql = "UPDATE productos SET activo = 1 WHERE id = ? AND activo = 0";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    public int contarProcesosEnCurso(int productoId) throws Exception {
        String sql = "SELECT COUNT(*) FROM procesos WHERE producto_id = ? AND estado = 'PROCESANDO'";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Producto map(ResultSet rs) throws Exception {
        return new Producto(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("seccion"),
                rs.getBigDecimal("precio_kg")
        );
    }
}
