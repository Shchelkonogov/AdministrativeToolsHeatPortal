package ru.tecon.admTools.reliabilityLimit.cdi;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import org.primefaces.event.CellEditEvent;
import ru.tecon.admTools.reliabilityLimit.ejb.ReliabilityLimitLocal;
import ru.tecon.admTools.reliabilityLimit.model.LimitData;
import ru.tecon.admTools.systemParams.SystemParamException;
import ru.tecon.admTools.systemParams.cdi.SystemParamsUtilMB;
import ru.tecon.admTools.utils.TeconMessage;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Maksim Shchelkonogov
 * 18.08.2026
 */
@Named("reliabilityLimit")
@ViewScoped
public class ReliabilityLimitMB implements Serializable {

    private boolean inIframe;

    private int objectID;
    private String objectPath;

    private List<LimitData> data;

    @EJB
    private ReliabilityLimitLocal bean;

    @Inject
    private SystemParamsUtilMB utilMB;
    @Inject
    private transient Logger logger;

    @PostConstruct
    private void init() {
        // По факту это заглушка для запуска инициализации контроллера SystemParamsUtilMB
        logger.log(Level.INFO, "Init data {0}", utilMB);

        Map<String, String> request = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        objectID = Integer.parseInt(request.get("objectId"));

        objectPath = bean.getObjectPath(objectID);

        PrimeFaces.current().executeScript("load();");
    }

    /**
     * Метод для загрузки данных
     */
    public void loadData() {
        data = bean.getLimitData(objectID);
    }

    /**
     * Метод обрабатывает событие изменения значения ячейки таблицы
     *
     * @param event событие
     */
    public void onCellEdit(CellEditEvent<?> event) {
        data.get(event.getRowIndex()).setChange(true);
        String clientID = event.getColumn().getChildren().get(0).getClientId().replaceAll(":", "\\:");
        PrimeFaces.current().executeScript("document.getElementById('" + clientID + "').parentNode.style.backgroundColor = 'lightgrey'");
    }

    /**
     * Обработчик нажатия кнопки сохранить
     */
    public void onSaveChanges() {
        // логируем все изменения
        logger.info("update limit data:");
        data.stream().filter(LimitData::isChange).forEach(limitData -> logger.info(limitData.toString()));
        logger.info("end;");

        // цикл для сохранения всех изменений в таблице
        data.stream().filter(LimitData::isChange).forEach(limitData -> {

            logger.info("save limit data: " + limitData);

            try {
                bean.updateLimitData(objectID, limitData, utilMB.getLogin());
            } catch (SystemParamException e) {
                if (inIframe) {
                    new TeconMessage(TeconMessage.SEVERITY_ERROR, "Ошибка сохранения", e.getMessage()).send();
                } else {
                    FacesContext.getCurrentInstance()
                            .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ошибка сохранения", e.getMessage()));
                }
            }
        });

        PrimeFaces.current().executeScript("load();");
    }

    public List<LimitData> getData() {
        return data;
    }

    public String getObjectPath() {
        return objectPath;
    }

    public void changeInIframe() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        inIframe = Boolean.parseBoolean(params.get("inIframe"));
    }
}
