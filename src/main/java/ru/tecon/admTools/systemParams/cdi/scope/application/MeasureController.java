package ru.tecon.admTools.systemParams.cdi.scope.application;

import ru.tecon.admTools.systemParams.ejb.MeasureSB;
import ru.tecon.admTools.systemParams.model.Measure;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Maksim Shchelkonogov
 * 03.05.2023
 */
@Named("measureControllerApplication")
@ApplicationScoped
public class MeasureController implements Serializable {

    private List<Measure> measures = new ArrayList<>();

    @Inject
    private Logger logger;

    @EJB
    private MeasureSB measureSB;

    @PostConstruct
    public void init() {
        logger.log(Level.INFO, "load measures");
        measures = measureSB.getMeasures();
    }

    public List<Measure> getMeasures() {
        return measures;
    }
}
