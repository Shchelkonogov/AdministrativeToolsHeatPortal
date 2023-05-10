package ru.tecon.admTools.systemParams.cdi;

import org.primefaces.PrimeFaces;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import ru.tecon.admTools.systemParams.SystemParamException;
import ru.tecon.admTools.systemParams.cdi.scope.application.MeasureController;
import ru.tecon.admTools.systemParams.ejb.MeasureSB;
import ru.tecon.admTools.systemParams.model.Measure;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Контроллер для формы единицы измерений
 *
 * @author Maksim Shchelkonogov
 */
@Named("measureMB")
@ViewScoped
public class MeasureMB implements Serializable {

    private Measure selectedMeasure;
    private Measure addMeasure = new Measure();

    private boolean disableRemoveBtn = true;

    @Inject
    private transient Logger logger;

    @EJB
    private MeasureSB measureSB;

    @Inject
    private SystemParamsUtilMB utilMB;

    @Inject
    private MeasureController measures;

    /**
     * Обработчик сохранения изменения строки
     *
     * @param event событие
     */
    public void onRowEdit(RowEditEvent<Measure> event) {
        logger.info("update row " + event.getObject());

        try {
            measureSB.updateMeasure(event.getObject(), utilMB.getLogin(), utilMB.getIp());
            measures.init();
        } catch (SystemParamException e) {
            FacesContext.getCurrentInstance()
                    .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ошибка обновления", e.getMessage()));
        }

        selectedMeasure = null;
        disableRemoveBtn = true;
    }

    /**
     * Обработчик события выделения строки
     *
     * @param event событие
     */
    public void onRowSelect(SelectEvent<Measure> event) {
        logger.info("select link: " + event.getObject());

        disableRemoveBtn = false;
    }

    /**
     * Обработчик удаления единицы измерения, возникает при нажатии на кнопку удалить (-)
     */
    public void onRemoveMeasure() {
        logger.info("remove measure: " + selectedMeasure);

        try {
            measureSB.removeMeasure(selectedMeasure, utilMB.getLogin(), utilMB.getIp());

            selectedMeasure = null;
            disableRemoveBtn = true;

            measures.init();
        } catch (SystemParamException e) {
            FacesContext.getCurrentInstance()
                    .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ошибка удаления", e.getMessage()));
        }
    }

    public void addMeasureWrapper() {
        PrimeFaces.current().executeScript("saveMeasureWrapper();");
    }

    /**
     * Добавление новой единицы измерения
     */
    public void onAddMeasure() {
        logger.info("add new measure " + addMeasure);

        try {
            measureSB.addMeasure(addMeasure, utilMB.getLogin(), utilMB.getIp());
            measures.init();

            PrimeFaces.current().executeScript("PF('addMeasureDialog').hide();");
        } catch (SystemParamException e) {
            FacesContext.getCurrentInstance()
                    .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ошибка обновления", e.getMessage()));
        }

        selectedMeasure = null;
        disableRemoveBtn = true;
    }

    public void onAddMeasureDialogClose() {
        addMeasure = new Measure();
    }

    public Measure getSelectedMeasure() {
        return selectedMeasure;
    }

    public void setSelectedMeasure(Measure selectedMeasure) {
        this.selectedMeasure = selectedMeasure;
    }

    public boolean isDisableRemoveBtn() {
        return disableRemoveBtn;
    }

    public Measure getAddMeasure() {
        return addMeasure;
    }

    public void setAddMeasure(Measure addMeasure) {
        this.addMeasure = addMeasure;
    }
}
