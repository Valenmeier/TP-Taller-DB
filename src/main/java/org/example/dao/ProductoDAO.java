package org.example.dao;

import org.example.Db;
import org.example.model.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    // CREATE o REACTIVATE
    public Producto createOrReactivate(Producto p) throws Exception {
        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);

            Integer foundId = null;
            Boolean foundActivo = null;

            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, activo FROM productos WHERE nombre=? AND seccion=? LIMIT 2")) {
                ps.setString(1, p.getNombre());
                ps.setString(2, p.getSeccion());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { foundId = rs.getInt("id"); foundActivo = rs.getBoolean("activo"); }
                    if (rs.next()) {
                        c.rollback();
                        throw new SQLIntegrityConstraintViolationException("Duplicados existentes para nombre+seccion");
                    }
                }
            }

            if (foundId == null) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO productos (nombre, seccion, precio_kg) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, p.getNombre());
                    ps.setString(2, p.getSeccion());
                    ps.setBigDecimal(3, p.getPrecioKg());
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) p.setId(k.getInt(1)); }
                }
                c.commit();
                return p;
            }

            if (!foundActivo) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE productos SET activo=1, precio_kg=? WHERE id=?")) {
                    ps.setBigDecimal(1, p.getPrecioKg());
                    ps.setInt(2, foundId);
                    ps.executeUpdate();
                }
                c.commit();
                return findById(foundId);
            }

            c.rollback();
            throw new SQLIntegrityConstraintViolationException("Producto duplicado (nombre+seccion) activo");
        }
    }

    // READ individual (solo activos)
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

    // READ lista (solo activos)
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

    // UPDATE (solo activos)
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
    public boolean deleteConsolidando(int id) throws Exception {
        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            String nombre = null, seccion = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT nombre, seccion FROM productos WHERE id=? AND activo=1")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { c.rollback(); return false; }
                    nombre = rs.getString(1);
                    seccion = rs.getString(2);
                }
            }
            Integer dupInactivoId = null;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id FROM productos WHERE nombre=? AND seccion=? AND activo=0 AND id<>? LIMIT 1")) {
                ps.setString(1, nombre);
                ps.setString(2, seccion);
                ps.setInt(3, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) dupInactivoId = rs.getInt(1);
                }
            }

            if (dupInactivoId != null) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE procesos SET producto_id=? WHERE producto_id=?")) {
                    ps.setInt(1, id);
                    ps.setInt(2, dupInactivoId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM productos WHERE id=?")) {
                    ps.setInt(1, dupInactivoId);
                    ps.executeUpdate();
                }
            }
            int updated;
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE productos SET activo=0 WHERE id=? AND activo=1")) {
                ps.setInt(1, id);
                updated = ps.executeUpdate();
            }

            if (updated == 1) { c.commit(); return true; }
            c.rollback();
            return false;
        }
    }

    // Conteo en curso
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
