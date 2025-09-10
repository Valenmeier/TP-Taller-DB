package org.example.dto;

import java.math.BigDecimal;

public record TicketDTO(
        Integer id,
        String nroProceso,
        String estado,
        String fecha,
        Integer productoId,
        BigDecimal pesoKg,
        BigDecimal precioUnitario,
        BigDecimal importeTotal,
        String productoNombre,
        String productoSeccion
) {
}
