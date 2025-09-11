package org.example.dao;

import org.example.Db;
import org.example.dto.TicketDTO;
import org.example.model.Proceso;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProcesoDAO {

    // Zona “humana” de la app (AR)
    private static final ZoneId APP_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // CREATE
    public Proceso create(Proceso p) throws Exception {
        String sql = """
            INSERT INTO procesos (nro_proceso, estado, fecha, producto_id, peso_kg, precio_unitario, importe_total)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        // Si viene null, tomamos ahora en zona AR
        LocalDateTime localFecha = (p.getFecha() != null) ? p.getFecha() : LocalDateTime.now(APP_ZONE);
        // Guardamos en DB como UTC
        Instant utcInstant = localFecha.atZone(APP_ZONE).toInstant();

        Integer newId = null;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNroProceso());
            ps.setString(2, p.getEstado());
            ps.setTimestamp(3, Timestamp.from(utcInstant)); // UTC en DB
            ps.setInt(4, p.getProductoId());
            ps.setBigDecimal(5, p.getPesoKg());
            ps.setBigDecimal(6, p.getPrecioUnitario());
            ps.setBigDecimal(7, p.getImporteTotal());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) newId = rs.getInt(1);
            }
        }

        // Devolvemos un Proceso coherente (fecha en AR y id asignado si lo hubo)
        return new Proceso(
                newId != null ? newId : p.getId(),
                p.getNroProceso(),
                p.getEstado(),
                localFecha,                 // fecha local (AR)
                p.getProductoId(),
                p.getPesoKg(),
                p.getPrecioUnitario(),
                p.getImporteTotal()
        );
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

    // Mapeo: DB (UTC) -> modelo (AR)
    private Proceso map(ResultSet rs) throws Exception {
        Timestamp ts = rs.getTimestamp("fecha");           // leemos UTC
        LocalDateTime fecha = ts.toInstant()               // a Instant (UTC)
                .atZone(APP_ZONE)                          // a AR
                .toLocalDateTime();                        // LDT para el modelo

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

                // UTC -> AR y devolvemos ISO con offset (ej: 2025-09-11T13:45:00-03:00)
                Timestamp ts = rs.getTimestamp("fecha");
                String fechaIsoLocal = ts.toInstant()
                        .atZone(APP_ZONE)
                        .format(ISO_OFFSET);

                return new TicketDTO(
                        rs.getInt("id"),
                        rs.getString("nro_proceso"),
                        rs.getString("estado"),
                        fechaIsoLocal, // ISO con offset -03:00
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
