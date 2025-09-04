package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Proceso {
    private Integer id;
    private String nroProceso;
    private String estado;
    private LocalDateTime fecha;
    private Integer productoId;
    private BigDecimal pesoKg;
    private BigDecimal precioUnitario;
    private BigDecimal importeTotal;

    public Proceso(Integer id, String nroProceso, String estado, LocalDateTime fecha,
                   Integer productoId, BigDecimal pesoKg, BigDecimal precioUnitario, BigDecimal importeTotal) {
        this.id = id;
        this.nroProceso = nroProceso;
        this.estado = estado;
        this.fecha = fecha;
        this.productoId = productoId;
        this.pesoKg = pesoKg;
        this.precioUnitario = precioUnitario;
        this.importeTotal = importeTotal;
    }

    public Integer getId() {
        return id;
    }

    public String getNroProceso() {
        return nroProceso;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public Integer getProductoId() {
        return productoId;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getImporteTotal() {
        return importeTotal;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
