package org.example.model;

import java.math.BigDecimal;

public class Producto {
    private Integer id;
    private String nombre;
    private String seccion;
    private BigDecimal precioKg;

    public Producto(Integer id, String nombre, String seccion, BigDecimal precioKg) {
        this.id = id;
        this.nombre = nombre;
        this.seccion = seccion;
        this.precioKg = precioKg;
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSeccion() {
        return seccion;
    }

    public BigDecimal getPrecioKg() {
        return precioKg;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setNombre(String n) {
        this.nombre = n;
    }

    public void setSeccion(String s) {
        this.seccion = s;
    }

    public void setPrecioKg(BigDecimal p) {
        this.precioKg = p;
    }

    @Override
    public String toString() {
        return "Producto{id=%s, nombre='%s', seccion='%s', precioKg=%s}"
                .formatted(id, nombre, seccion, precioKg);
    }
}
