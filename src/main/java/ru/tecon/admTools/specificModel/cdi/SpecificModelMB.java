package ru.tecon.admTools.specificModel.cdi;

import org.primefaces.PrimeFaces;
import org.primefaces.event.CellEditEvent;
import ru.tecon.admTools.specificModel.ejb.CheckUserSB;
import ru.tecon.admTools.specificModel.ejb.SpecificModelLocal;
import ru.tecon.admTools.specificModel.model.Condition;
import ru.tecon.admTools.specificModel.model.DataModel;
import ru.tecon.admTools.specificModel.model.GraphDecreaseItemModel;
import ru.tecon.admTools.specificModel.model.additionalModel.AnalogData;
import ru.tecon.admTools.specificModel.model.additionalModel.EnumerateData;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Контроллер для формы конкретная модель
 */
@Named("specificModel")
@ViewScoped
public class SpecificModelMB implements Serializable {

    private static Logger log  = Logger.getLogger(SpecificModelMB.class.getName());

    // Параметр для переключения с конкретной модели на конкретную модель экомониторинга
    private boolean eco = false;

    private int objectID;
    private String objectPath;
    private boolean write = false;

    // Модели данных для закладки аналоговые параметры
    private List<DataModel> tableModel;
    private List<DataModel> filteredTableModel = new ArrayList<>();
    private Set<String> techProcFilter;
    private Set<String> paramNameFilter;

    // Модели данных для всплывающих окон на закладке аналоговые параметры

    // Данные для изменения
    private DataModel changedDecreaseGraphItem;
    private int rowIndex;

    // Модели данных для всплывающего окна выбора суточного снижения
    private List<GraphDecreaseItemModel> decreases;
    private GraphDecreaseItemModel selectDecrease;

    // Модель данных для всплывающего окна выбора графика или оптимального значения
    private List<GraphDecreaseItemModel> graphs;
    private GraphDecreaseItemModel selectGraph;
    private String optValue = "";

    // Модели данных для закладки перечислимые параметры

    // Левая таблица с основными данными
    private List<DataModel> enumerableTableModel;
    private DataModel selectedEnumerateItem;

    // Правая таблица с дополнительными данными
    private EnumerateData paramConditionDataModel;
    private List<Condition> conditions;

    @EJB
    private SpecificModelLocal bean;

    @EJB
    private CheckUserSB checkBean;

    @PostConstruct
    private void init() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

        eco = Boolean.parseBoolean(request.getParameter("eco"));

        try {
            objectID = Integer.parseInt(request.getParameter("objectID"));
            write = checkBean.checkSessionWrite(request.getParameter("sessionID"), Integer.parseInt(request.getParameter("formID")));
        } catch (NumberFormatException e) {
            return;
        }

        objectPath = bean.getObjectPath(objectID);

        loadData();

        if (!eco) {
            conditions = bean.getConditions();
        }
        decreases = bean.getDecreases();
        graphs = bean.getGraphs();

        techProcFilter = tableModel.stream().map(DataModel::getTechProcCode).collect(Collectors.toSet());
        paramNameFilter = tableModel.stream().map(DataModel::getParamTypeName).collect(Collectors.toSet());
    }

    private void loadData() {
        tableModel = bean.getData(objectID, eco);

        filteredTableModel.clear();
        filteredTableModel.addAll(tableModel);

        if (!eco) {
            enumerableTableModel = bean.getEnumerableData(objectID);
        } else {
            enumerableTableModel = new ArrayList<>();
        }
    }

    /**
     * Метод обрабатывает инициализацию события изменения значения ячейки таблицы аналоговых параметров
     * @param event событие
     */
    public void onCellEditInit(CellEditEvent event) {
        rowIndex = event.getRowIndex();
        changedDecreaseGraphItem = filteredTableModel.get(rowIndex);
        if (event.getColumn().getClientId().endsWith("graphColumn")) {
            if ((((AnalogData) changedDecreaseGraphItem.getAdditionalData()).getGraphID() == null) &&
                    (((AnalogData) changedDecreaseGraphItem.getAdditionalData()).getGraphName() != null)) {
                optValue = ((AnalogData) changedDecreaseGraphItem.getAdditionalData()).getGraphName();
            } else {
                optValue = "";
            }
            PrimeFaces.current().ajax().update("graphSelectForm");
            PrimeFaces.current().executeScript("PF('graphDialog').show();");
        } else {
            if (event.getColumn().getClientId().endsWith("decreaseColumn")) {
                PrimeFaces.current().executeScript("PF('decreaseDialog').show();");
            }
        }
    }

    /**
     * Метод обрабатывает событие изменения значения ячейки таблицы аналоговых параметров
     * @param event событие
     */
    public void onCellEdit(CellEditEvent event) {
        filteredTableModel.get(event.getRowIndex()).setChange(true);
    }

    /**
     * Метод обрабатывает нажатие на кнопку сохранить и осхраняет все изменения в базе
     */
    public void saveData() {
        log.info("update analog objects:");
        tableModel.stream().filter(DataModel::isChange).forEach(dataModel -> log.info(dataModel.toString()));
        log.info("end;");

        log.info("update enumerate objects:");
        enumerableTableModel.stream().filter(DataModel::isChange).forEach(dataModel -> log.info(dataModel.toString()));
        log.info("end;");

        FacesContext context = FacesContext.getCurrentInstance();

        tableModel.stream().filter(DataModel::isChange).forEach(dataModel -> {
            String message;

            log.info("save analog data: " + dataModel);

            message = bean.saveGraph(objectID, dataModel);
            if (message != null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ошибка записи", dataModel.getParMemo() + ": " + message));
            }

            message = bean.saveRanges(objectID, dataModel);
            if (message != null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ошибка записи", dataModel.getParMemo() + ": " + message));
            }

            bean.saveDecrease(objectID, dataModel);
        });

        enumerableTableModel.stream().filter(DataModel::isChange).forEach(dataModel -> {
            log.info("save enum data: " + dataModel);
            bean.saveEnumParam(objectID, dataModel);
        });

        loadData();
    }

    /**
     * Метод обрабатывает нажатие contextMenu Сбросить границы
     */
    public void clearRanges(int objectID, int parID, int statAgrID) {
        bean.clearRanges(objectID, parID, statAgrID);
        loadData();
    }

    /**
     * Метод обрабатывает нажатие на checkBox в таблице с аналоговыми параметрами
     * @param rowIndex индекс строки
     */
    public void onBooleanValueChange(int rowIndex) {
        filteredTableModel.get(rowIndex).setChange(true);
    }

    /**
     * Метод обрабатывает нажатие на выбрать в всплывающем окне выбора суточного снижения
     */
    public void saveDecrease() {
        ((AnalogData) changedDecreaseGraphItem.getAdditionalData()).setDecreaseName(selectDecrease.getName());
        ((AnalogData) changedDecreaseGraphItem.getAdditionalData()).setDecreaseID(selectDecrease.getId());
        changedDecreaseGraphItem.setChange(true);
        PrimeFaces.current().ajax().update("tabView:tableForm:tableData:" + rowIndex + ":decreaseValuePanel");
    }

    /**
     * Метод обрабатывает нажатие на выбрать в всплывающем окне выбора суточного снижения
     */
    public void saveNoDecrease() {
        ((AnalogData) changedDecreaseGraphItem.getAdditionalData()).setDecreaseName(null);
        ((AnalogData) changedDecreaseGraphItem.getAdditionalData()).setDecreaseID(null);
        changedDecreaseGraphItem.setChange(true);
        PrimeFaces.current().ajax().update("tabView:tableForm:tableData:" + rowIndex + ":decreaseValuePanel");
    }

    /**
     * Метод обрабатывает нажатие на выбрать в всплывающем окне выбора графика
     */
    public void saveGraph() {
        ((AnalogData) changedDecreaseGraphItem.getAdditionalData()).setGraphName(selectGraph.getName());
        ((AnalogData) changedDecreaseGraphItem.getAdditionalData()).setGraphID(selectGraph.getId());
        changedDecreaseGraphItem.setChange(true);
        PrimeFaces.current().ajax().update("tabView:tableForm:tableData:" + rowIndex + ":graphValuePanel");
    }

    /**
     * Метод обрабатывает нажатие на выбрать в всплывающем окне выбора графика
     */
    public void saveNoGraph() {
        ((AnalogData) changedDecreaseGraphItem.getAdditionalData()).setGraphName("");
        ((AnalogData) changedDecreaseGraphItem.getAdditionalData()).setGraphID(null);
        changedDecreaseGraphItem.setChange(true);
        PrimeFaces.current().ajax().update("tabView:tableForm:tableData:" + rowIndex + ":graphValuePanel");
    }

    /**
     * Метод обрабатывает событие выделения строки в таблице с перечислимыми параметрами
     */
    public void onRowSelect() {
        if (selectedEnumerateItem.getAdditionalData() == null) {
            paramConditionDataModel = bean.getParamCondition(objectID, selectedEnumerateItem.getParID(), selectedEnumerateItem.getStatAgr());
            selectedEnumerateItem.setAdditionalData(paramConditionDataModel);
        } else {
            paramConditionDataModel = (EnumerateData) selectedEnumerateItem.getAdditionalData();
        }
    }

    /**
     * Метод обрабатывает событие изменения состояний в уточняющей таблице перечислимых параметров
     * @param event событие
     */
    public void onCellEnumEdit(CellEditEvent event) {
        ((EnumerateData) selectedEnumerateItem.getAdditionalData()).getConditions().get(event.getRowIndex()).setEdited(true);
        selectedEnumerateItem.setChange(true);
        String clientID = event.getColumn().getChildren().get(0).getClientId().replaceAll(":", "\\:");
        PrimeFaces.current().executeScript("document.getElementById('" + clientID + "').parentNode.style.backgroundColor = 'lightgrey'");
    }

    /**
     * Метод обрабатывает нажание кнопки выбрать в выборе графика или оптимального значения
     * на закладке оптимальное значение
     */
    public void saveOptValue() {
        AnalogData data = (AnalogData) changedDecreaseGraphItem.getAdditionalData();
        List<String> updateList = new ArrayList<>();

        if ((data.getGraphID() != null) || (data.getGraphName() == null) || (data.getGraphName().equals(""))) {
            data.setA(-10d, 10d, true, true, true);
            data.setT(-5d, 5d, true, true, true);
            data.setAbsolute(true);

            updateList.add("tabView:tableForm:tableData:" + rowIndex + ":tMinCellEditor");
            updateList.add("tabView:tableForm:tableData:" + rowIndex + ":tMaxCellEditor");
            updateList.add("tabView:tableForm:tableData:" + rowIndex + ":tPercentColumn");
            updateList.add("tabView:tableForm:tableData:" + rowIndex + ":aMinCellEditor");
            updateList.add("tabView:tableForm:tableData:" + rowIndex + ":aMaxCellEditor");
            updateList.add("tabView:tableForm:tableData:" + rowIndex + ":aPercentColumn");
            updateList.add("tabView:tableForm:tableData:" + rowIndex + ":absoluteColumn");

            PrimeFaces.current().executeScript("blockRow(" + rowIndex + ");");
        }

        data.setGraphName(optValue);
        data.setGraphID(null);

        updateList.add("tabView:tableForm:tableData:" + rowIndex + ":graphCellEditor");

        changedDecreaseGraphItem.setChange(true);
        PrimeFaces.current().ajax().update(updateList);
    }

    /**
     * Метод обрабатывает нажание кнопки выбрать в выборе графика или оптимального значения
     * на закладке оптимальное значение
     */
    public void saveNoOptValue() {
        ((AnalogData) changedDecreaseGraphItem.getAdditionalData()).setGraphName("");
        ((AnalogData) changedDecreaseGraphItem.getAdditionalData()).setGraphID(null);
        changedDecreaseGraphItem.setChange(true);
        PrimeFaces.current().ajax().update("tabView:tableForm:tableData:" + rowIndex + ":graphValuePanel");
    }

    public String getLabelForNewOptValue() {
        if (changedDecreaseGraphItem == null) {
            return "";
        }
        AnalogData data = (AnalogData) changedDecreaseGraphItem.getAdditionalData();
        if ((data.getGraphID() != null) || (data.getGraphName() == null) || (data.getGraphName().equals(""))) {
            return "Новое оптимальное значение. Границы будут сброшены!";
        }
        return "";
    }

    /**
     * Получение границ с перерасчетом процентов
     * @return границы
     */
    public String gettMax() {
        if (changedDecreaseGraphItem == null) {
            return "-";
        }

        AnalogData data = (AnalogData) changedDecreaseGraphItem.getAdditionalData();

        if ((data.getGraphID() != null) || (data.getGraphName() == null) || (data.getGraphName().equals(""))) {
            try {
                return new BigDecimal(5)
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN)
                        .add(new BigDecimal(optValue))
                        .setScale(2, RoundingMode.HALF_EVEN).toString();
            } catch (NumberFormatException e) {
                return 5 + "%";
            }
        }

        if (data.gettMax() == null) {
            return "-";
        }

        if (data.istPercent()) {
            try {
                return new BigDecimal(data.gettMax())
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN)
                        .add(new BigDecimal(optValue))
                        .setScale(2, RoundingMode.HALF_EVEN).toString();
            } catch (NumberFormatException e) {
                return data.gettMax() + "%";
            }
        } else {
            try {
                return new BigDecimal(data.gettMax())
                        .add(new BigDecimal(optValue))
                        .setScale(2, RoundingMode.HALF_EVEN).toString();
            } catch (NumberFormatException e) {
                return data.gettMax().toString();
            }
        }
    }

    /**
     * Получение границ с перерасчетом процентов
     * @return границы
     */
    public String gettMin() {
        if (changedDecreaseGraphItem == null) {
            return "-";
        }

        AnalogData data = (AnalogData) changedDecreaseGraphItem.getAdditionalData();

        if ((data.getGraphID() != null) || (data.getGraphName() == null) || (data.getGraphName().equals(""))) {
            try {
                return new BigDecimal(-5)
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN)
                        .add(new BigDecimal(optValue))
                        .setScale(2, RoundingMode.HALF_EVEN).toString();
            } catch (NumberFormatException e) {
                return -5 + "%";
            }
        }

        if (data.gettMin() == null) {
            return "-";
        }

        if (data.istPercent()) {
            try {
                return new BigDecimal(data.gettMin())
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN)
                        .add(new BigDecimal(optValue))
                        .setScale(2, RoundingMode.HALF_EVEN).toString();
            } catch (NumberFormatException e) {
                return data.gettMin() + "%";
            }
        } else {
            try {
                return new BigDecimal(data.gettMin())
                        .add(new BigDecimal(optValue))
                        .setScale(2, RoundingMode.HALF_EVEN).toString();
            } catch (NumberFormatException e) {
                return data.gettMin().toString();
            }
        }
    }

    /**
     * Получение границ с перерасчетом процентов
     * @return границы
     */
    public String getaMax() {
        if (changedDecreaseGraphItem == null) {
            return "-";
        }

        AnalogData data = (AnalogData) changedDecreaseGraphItem.getAdditionalData();

        if ((data.getGraphID() != null) || (data.getGraphName() == null) || (data.getGraphName().equals(""))) {
            try {
                return new BigDecimal(10)
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN)
                        .add(new BigDecimal(optValue))
                        .setScale(2, RoundingMode.HALF_EVEN).toString();
            } catch (NumberFormatException e) {
                return 10 + "%";
            }
        }

        if (data.getaMax() == null) {
            return "-";
        }

        if (data.isaPercent()) {
            try {
                return new BigDecimal(data.getaMax())
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN)
                        .add(new BigDecimal(optValue))
                        .setScale(2, RoundingMode.HALF_EVEN).toString();
            } catch (NumberFormatException e) {
                return data.getaMax() + "%";
            }
        } else {
            if (data.isAbsolute()) {
                try {
                    return new BigDecimal(data.getaMax())
                            .add(new BigDecimal(optValue))
                            .setScale(2, RoundingMode.HALF_EVEN).toString();
                } catch (NumberFormatException e) {
                    return data.getaMax().toString();
                }
            } else {
                return data.getaMax().toString();
            }
        }
    }

    /**
     * Получение границ с перерасчетом процентов
     * @return границы
     */
    public String getaMin() {
        if (changedDecreaseGraphItem == null) {
            return "-";
        }

        AnalogData data = (AnalogData) changedDecreaseGraphItem.getAdditionalData();

        if ((data.getGraphID() != null) || (data.getGraphName() == null) || (data.getGraphName().equals(""))) {
            try {
                return new BigDecimal(-10)
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN)
                        .add(new BigDecimal(optValue))
                        .setScale(2, RoundingMode.HALF_EVEN).toString();
            } catch (NumberFormatException e) {
                return -10 + "%";
            }
        }

        if (data.getaMin() == null) {
            return "-";
        }

        if (data.isaPercent()) {
            try {
                return new BigDecimal(data.getaMin())
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN)
                        .add(new BigDecimal(optValue))
                        .setScale(2, RoundingMode.HALF_EVEN).toString();
            } catch (NumberFormatException e) {
                return data.getaMin() + "%";
            }
        } else {
            if (data.isAbsolute()) {
                try {
                    return new BigDecimal(data.getaMin())
                            .add(new BigDecimal(optValue))
                            .setScale(2, RoundingMode.HALF_EVEN).toString();
                } catch (NumberFormatException e) {
                    return data.getaMin().toString();
                }
            } else {
                return data.getaMin().toString();
            }
        }
    }

    public void setOptValue(String optValue) {
        this.optValue = optValue;
    }

    public String getOptValue() {
        return optValue;
    }

    public String getObjectPath() {
        return objectPath;
    }

    public List<DataModel> getTableModel() {
        return tableModel;
    }

    public List<DataModel> getFilteredTableModel() {
        return filteredTableModel;
    }

    public void setFilteredTableModel(List<DataModel> filteredTableModel) {
        this.filteredTableModel = filteredTableModel;
    }

    public Set<String> getTechProcFilter() {
        return techProcFilter;
    }

    public List<GraphDecreaseItemModel> getDecreases() {
        return decreases;
    }

    public GraphDecreaseItemModel getSelectDecrease() {
        return selectDecrease;
    }

    public void setSelectDecrease(GraphDecreaseItemModel selectDecrease) {
        this.selectDecrease = selectDecrease;
    }

    public List<GraphDecreaseItemModel> getGraphs() {
        return graphs;
    }

    public GraphDecreaseItemModel getSelectGraph() {
        return selectGraph;
    }

    public void setSelectGraph(GraphDecreaseItemModel selectGraph) {
        this.selectGraph = selectGraph;
    }

    public List<DataModel> getEnumerableTableModel() {
        return enumerableTableModel;
    }

    public DataModel getSelectedEnumerateItem() {
        return selectedEnumerateItem;
    }

    public void setSelectedEnumerateItem(DataModel selectedEnumerateItem) {
        this.selectedEnumerateItem = selectedEnumerateItem;
    }

    public EnumerateData getParamConditionDataModel() {
        return paramConditionDataModel;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public Set<String> getParamNameFilter() {
        return paramNameFilter;
    }

    public int getObjectID() {
        return objectID;
    }

    public boolean isWrite() {
        return write;
    }

    public boolean isEco() {
        return eco;
    }
}
