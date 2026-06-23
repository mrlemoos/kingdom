package dev.leo.kingdom.model.parliament;

import java.util.Optional;

public final class ParliamentSites {

    private ChamberSite commons;
    private ChamberSite lords;
    private RegistrarSite registrar;

    public Optional<ChamberSite> commons() {
        return Optional.ofNullable(commons);
    }

    public Optional<ChamberSite> lords() {
        return Optional.ofNullable(lords);
    }

    public Optional<RegistrarSite> registrar() {
        return Optional.ofNullable(registrar);
    }

    public void setCommons(ChamberSite site) {
        this.commons = site;
    }

    public void setLords(ChamberSite site) {
        this.lords = site;
    }

    public void setRegistrar(RegistrarSite site) {
        this.registrar = site;
    }
}
