package org.example.dao;

import org.example.Db;
import org.example.dto.TicketDTO;
import org.example.model.Proceso;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class ProcesoDAO {

    // CREATE
    public Proceso create(Proceso p) throws Exception {
        String sql = """
            INSERT INTO procesos (nro_proceso, estado, fecha, producto_id, peso_kg, precio_unitario, importe_total)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNroProceso());
            ps.setString(2, p.getEstado());
            ps.setTimestamp(3, Timestamp.valueOf(p.getFecha()));
            ps.setInt(4, p.getProductoId());
            ps.setBigDecimal(5, p.getPesoKg());
            ps.setBigDecimal(6, p.getPrecioUnitario());
            ps.setBigDecimal(7, p.getImporteTotal());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) p.setId(rs.getInt(1));
            }
        }
        return p;
    }

    // READ por nro_proceso
    public Proceso findByNro(String nro) throws Exception {
        String sql = "SELECT * FROM procesos WHERE nro_proceso=?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // READ todos
    public List<Proceso> findAll(String estado) throws Exception {
        String base = "SELECT * FROM procesos";
        boolean filtrar = estado != null && !estado.isBlank();
        String sql = filtrar ? base + " WHERE estado=? ORDER BY fecha DESC"
                : base + " ORDER BY fecha DESC";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (filtrar) ps.setString(1, estado);
            try (ResultSet rs = ps.executeQuery()) {
                List<Proceso> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    // UPDATE estado
    public boolean actualizarEstado(String nro, String nuevoEstado) throws Exception {
        String sql = "UPDATE procesos SET estado=? WHERE nro_proceso=?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setString(2, nro);
            return ps.executeUpdate() == 1;
        }
    }

    // DELETE
    public boolean deleteByNro(String nro) throws Exception {
        String sql = "DELETE FROM procesos WHERE nro_proceso=?";
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nro);
            return ps.executeUpdate() == 1;
        }
    }

    private Proceso map(ResultSet rs) throws Exception {
        Timestamp ts = rs.getTimestamp("fecha");
        LocalDateTime fecha = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return new Proceso(
                rs.getInt("id"),
                rs.getString("nro_proceso"),
                rs.getString("estado"),
                fecha,
                rs.getInt("producto_id"),
                rs.getBigDecimal("peso_kg"),
                rs.getBigDecimal("precio_unitario"),
                rs.getBigDecimal("importe_total")
        );
    }
    public TicketDTO findTicketDTO(String nro) throws Exception {
        String sql = """
            SELECT prc.id,
                   prc.nro_proceso,
                   prc.estado,
                   prc.fecha,
                   prc.producto_id,
                   prc.peso_kg,
                   prc.precio_unitario,
                   prc.importe_total,
                   prod.nombre  AS producto_nombre,
                   prod.seccion AS producto_seccion
            FROM procesos prc
            LEFT JOIN productos prod ON prod.id = prc.producto_id
            WHERE prc.nro_proceso = ?
            """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nro);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new TicketDTO(
                        rs.getInt("id"),
                        rs.getString("nro_proceso"),
                        rs.getString("estado"),
                        rs.getString("fecha"),
                        rs.getInt("producto_id"),
                        rs.getBigDecimal("peso_kg"),
                        rs.getBigDecimal("precio_unitario"),
                        rs.getBigDecimal("importe_total"),
                        rs.getString("producto_nombre"),
                        rs.getString("producto_seccion")
                );
            }
        }
    }
}
