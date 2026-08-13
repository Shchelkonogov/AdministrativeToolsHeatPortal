package ru.tecon.admTools.systemParams.cdi;

import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import ru.tecon.admTools.systemParams.SystemParamException;
import ru.tecon.admTools.systemParams.ejb.AnalogSensorSB;
import ru.tecon.admTools.systemParams.model.analogSensor.CriteriaData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Контроллер для формы Фоновые процессы
 *
 * @author Maksim Shchelkonogov
 * 06.06.2025
 */
@Named("analogSensor")
@ViewScoped
public class AnalogSensorMB implements Serializable, AutoUpdate {

    private List<CriteriaData> criteriaList;
    private int offlineDays;
    private int dynamicLack;
    private int deltaTnv;

    @Inject
    private transient Logger logger;

    @Inject
    private SystemParamsUtilMB utilMB;

    @EJB
    private AnalogSensorSB bean;

    @Override
    public void update() {
        criteriaList = bean.getCriteria();
        offlineDays = bean.getOfflineDays();
        dynamicLack = bean.getDynamicLack();
        deltaTnv = bean.getDeltaTNV();
    }

    /**
     * Обработчик выбора checkBox в колонке состояние
     *
     * @param data     строка в которой нажали кнопку
     * @param rowIndex индекс строки
     */
    public void onChange(CriteriaData data, int rowIndex) {
        try {
            bean.changeCriteriaState(data, utilMB.getLogin(), utilMB.getIp());

            PrimeFaces.current().ajax().update("analogSensorPanel:criteria:" + rowIndex + ":state");
        } catch (SystemParamException e) {
            data.setState(!data.isState());
            PrimeFaces.current().ajax().update(Arrays.asList("analogSensorPanel:criteria:" + rowIndex + ":state", "growl"));
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ошибка записи", e.getMessage()));
        }
    }

    /**
     * Обработчик нажатия кнопки сохранить
     */
    public void onSaveChanges() {
        FacesContext context = FacesContext.getCurrentInstance();

        List<String> errorMessages = new ArrayList<>();

        try {
            bean.updateOfflineDays(offlineDays, utilMB.getLogin(), utilMB.getIp());
        } catch (SystemParamException e) {
            errorMessages.add("Количество дней, в течении которых не было связи с объектом в целом");
            offlineDays = bean.getOfflineDays();
            logger.warning(e.getMessage());
        }

        try {
            bean.updateDynamicLack(dynamicLack, utilMB.getLogin(), utilMB.getIp());
        } catch (SystemParamException e) {
            errorMessages.add("количество дней отсутствия динамики");
            dynamicLack = bean.getDynamicLack();
            logger.warning(e.getMessage());
        }

        try {
            bean.updateDeltaTNV(deltaTnv, utilMB.getLogin(), utilMB.getIp());
        } catch (SystemParamException e) {
            errorMessages.add("Отклонение Тнв");
            deltaTnv = bean.getDeltaTNV();
            logger.warning(e.getMessage());
        }

        if (!errorMessages.isEmpty()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ошибка записи", String.join(", ", errorMessages)));
        }
    }

    public List<CriteriaData> getCriteriaList() {
        return criteriaList;
    }

    public int getOfflineDays() {
        return offlineDays;
    }

    public void setOfflineDays(int offlineDays) {
        this.offlineDays = offlineDays;
    }

    public int getDeltaTnv() {
        return deltaTnv;
    }

    public void setDeltaTnv(int deltaTnv) {
        this.deltaTnv = deltaTnv;
    }

    public int getDynamicLack() {
        return dynamicLack;
    }

    public void setDynamicLack(int dynamicLack) {
        this.dynamicLack = dynamicLack;
    }
}
