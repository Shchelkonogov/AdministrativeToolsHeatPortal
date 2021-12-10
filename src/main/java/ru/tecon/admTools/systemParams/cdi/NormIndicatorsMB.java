package ru.tecon.admTools.systemParams.cdi;

import org.primefaces.PrimeFaces;
import org.primefaces.event.CellEditEvent;
import ru.tecon.admTools.systemParams.SystemParamException;
import ru.tecon.admTools.systemParams.ejb.NormIndicatorsSB;
import ru.tecon.admTools.systemParams.model.normIndicators.*;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.faces.view.facelets.FaceletContext;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Контроллер для формы нормативные показатели
 * @author Maksim Shchelkonogov
 */
@Named("normIndicators")
@ViewScoped
public class NormIndicatorsMB implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(NormIndicatorsMB.class.getName());

    private List<IndicatorTV> indicatorTVList = new ArrayList<>();
    private List<IndicatorCO> indicatorCOList = new ArrayList<>();
    private List<IndicatorGVS> indicatorGVSList = new ArrayList<>();
    private List<IndicatorVENT> indicatorVENTList = new ArrayList<>();
    private List<IndicatorOther> indicatorOtherList = new ArrayList<>();

    private String login;
    private String ip;
    private boolean write = false;

    @EJB
    private NormIndicatorsSB normIndicatorsBean;

    @PostConstruct
    private void init() {
        FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance()
                .getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
        ip = (String) faceletContext.getAttribute("ip");
        login = (String) faceletContext.getAttribute("login");
        write = (boolean) faceletContext.getAttribute("write");

        loadData();
    }

    /**
     * Метод выполняется при загрузки формы
     */
    public void onFormLoad() {
        LOGGER.info("load form data");

        loadData();
    }

    /**
     * Загрузка данных формы
     */
    private void loadData() {
        indicatorTVList = normIndicatorsBean.getTV();
        indicatorCOList = normIndicatorsBean.getCO();
        indicatorGVSList = normIndicatorsBean.getGVS();
        indicatorVENTList = normIndicatorsBean.getVENT();
        indicatorOtherList = normIndicatorsBean.getOther();
    }

    /**
     * Обработчик нажатия кнопки сохранить
     */
    public void onSaveChanges(String type) {
        FacesContext context = FacesContext.getCurrentInstance();

        List<String> errorMessages = new ArrayList<>();

        switch (type) {
            case "TV": {
                indicatorTVList.stream().filter(IndicatorTV::isChange).forEach(indicatorTV -> {
                    LOGGER.info("update norm indicator TV " + indicatorTV);

                    try {
                        normIndicatorsBean.updateTV(indicatorTV, login, ip);
                    } catch (SystemParamException e) {
                        errorMessages.add("Тепловой ввод");
                        LOGGER.warning(e.getMessage());
                    }
                });

                indicatorTVList = normIndicatorsBean.getTV();
            }
            case "CO": {
                indicatorCOList.stream().filter(IndicatorCO::isChange).forEach(indicatorCO -> {
                    LOGGER.info("update norm indicator CO " + indicatorCO);

                    try {
                        normIndicatorsBean.updateCO(indicatorCO, login, ip);
                    } catch (SystemParamException e) {
                        errorMessages.add(indicatorCO.getName());
                        LOGGER.warning(e.getMessage());
                    }
                });

                indicatorCOList = normIndicatorsBean.getCO();
            }
            case "GVS": {
                indicatorGVSList.stream().filter(IndicatorGVS::isChange).forEach(indicatorGVS -> {
                    LOGGER.info("update norm indicator GVS " + indicatorGVS);

                    try {
                        normIndicatorsBean.updateGVS(indicatorGVS, login, ip);
                    } catch (SystemParamException e) {
                        errorMessages.add(indicatorGVS.getName());
                        LOGGER.warning(e.getMessage());
                    }
                });

                indicatorGVSList = normIndicatorsBean.getGVS();
            }
            case "VENT": {
                indicatorVENTList.stream().filter(IndicatorVENT::isChange).forEach(indicatorVENT -> {
                    LOGGER.info("update norm indicator VENT " + indicatorVENT);

                    try {
                        normIndicatorsBean.updateVENT(indicatorVENT, login, ip);
                    } catch (SystemParamException e) {
                        errorMessages.add(indicatorVENT.getName());
                        LOGGER.warning(e.getMessage());
                    }
                });

                indicatorVENTList = normIndicatorsBean.getVENT();
            }
            case "Other": {
                indicatorOtherList.stream().filter(IndicatorOther::isChange).forEach(indicatorOther -> {
                    LOGGER.info("update norm indicator other " + indicatorOther);

                    try {
                        normIndicatorsBean.updateOther(indicatorOther, login, ip);
                    } catch (SystemParamException e) {
                        errorMessages.add("Источник - Потребитель");
                        LOGGER.warning(e.getMessage());
                    }
                });

                indicatorOtherList = normIndicatorsBean.getOther();
            }
        }

        if (!errorMessages.isEmpty()) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ошибка записи", String.join(", ", errorMessages)));
        }
    }

    /**
     * Обработик изменения ячейки таблицы
     * @param event событие изменения
     */
    public void onCellEdit(CellEditEvent event) {
        String clientID = event.getColumn().getChildren().get(0).getClientId().replaceAll(":", "\\:");
        PrimeFaces.current().executeScript("document.getElementById('" + clientID + "').style.backgroundColor = 'lightgrey'");
    }


    public List<IndicatorTV> getIndicatorTVList() {
        return indicatorTVList;
    }

    public List<IndicatorCO> getIndicatorCOList() {
        return indicatorCOList;
    }

    public List<IndicatorGVS> getIndicatorGVSList() {
        return indicatorGVSList;
    }

    public List<IndicatorVENT> getIndicatorVENTList() {
        return indicatorVENTList;
    }

    public List<IndicatorOther> getIndicatorOtherList() {
        return indicatorOtherList;
    }

    public boolean isWrite() {
        return write;
    }
}