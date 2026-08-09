package dev.mrlemoos.kingdom.model.parliament;

import java.util.Optional;

public final class ParliamentSites {

    private ChamberSite commons;
    private ChamberSite lords;
    private ChamberSite speakerChair;
    private RegistrarSite registrar;

    public Optional<ChamberSite> commons() {
        return Optional.ofNullable(commons);
    }

    /** Where the Speaker presides; the Commons point is used when no chair has been set. */
    public Optional<ChamberSite> speakerChair() {
        return Optional.ofNullable(speakerChair);
    }

    public void setSpeakerChair(ChamberSite site) {
        this.speakerChair = site;
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
