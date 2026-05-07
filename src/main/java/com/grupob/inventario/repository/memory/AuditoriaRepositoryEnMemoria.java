package com.grupob.inventario.repository.memory;

import com.grupob.inventario.domain.model.EventoAuditoria;
import com.grupob.inventario.repository.AuditoriaRepository;
import com.grupob.inventario.repository.FiltroAuditoria;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuditoriaRepositoryEnMemoria implements AuditoriaRepository {

    private final List<EventoAuditoria> eventos = new CopyOnWriteArrayList<>();

    @Override
    public void registrar(EventoAuditoria evento) {
        eventos.add(evento);
    }

    @Override
    public List<EventoAuditoria> consultar(FiltroAuditoria filtro, int pagina, int tamanoPagina) {
        return List.copyOf(eventos.stream()
                .filter(e -> filtro.desde() == null || !e.getFecha().isBefore(filtro.desde()))
                .filter(e -> filtro.hasta() == null || !e.getFecha().isAfter(filtro.hasta()))
                .filter(e -> filtro.username() == null || filtro.username().equals(e.getUsername()))
                .filter(e -> filtro.tipoEvento() == null || filtro.tipoEvento() == e.getTipoEvento())
                .sorted(Comparator.comparing(EventoAuditoria::getFecha).reversed())
                .skip((long) pagina * tamanoPagina)
                .limit(tamanoPagina)
                .toList());
    }
}
