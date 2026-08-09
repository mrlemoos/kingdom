package dev.mrlemoos.kingdom.model.parliament;

import java.util.Optional;

public final class ParliamentSites {

    private ChamberSite commons;
    private ChamberSite lords;
    private ChamberSite speakerChair;
    private ChamberSite bar;
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

    /**
     * The Bar of the House: where the Speaker stands to address the Crown with the return of the
     * Commons. Falls back to a spot before the Crown when none has been set.
     */
    public Optional<ChamberSite> bar() {
        return Optional.ofNullable(bar);
    }

    public void setBar(ChamberSite site) {
        this.bar = site;
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
