package ru.tecon.admTools.systemParams.cdi;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
import ru.tecon.admTools.systemParams.model.SystemParamsCategories;
import ru.tecon.admTools.utils.AdmTools;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Контроллер формы системные параметры левая часть меню и правая меняется
 * в зависимости от выбранного элемента
 *
 * @author Maksim Shchelkonogov
 */
@Named("systemParams")
@ViewScoped
public class SystemParamsMB implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(SystemParamsMB.class.getName());

    private static final Map<String, SystemParamsCategories> SYSTEM_PARAMS_MAP = Stream.of(new Object[][]{
            {"Подразделения", new SystemParamsCategories("/view/sysParams/struct/structDivisions.xhtml", "structDivisions")},
            {"Объекты", new SystemParamsCategories("/view/sysParams/struct/structObjects.xhtml", "structObjects")},
            {"Связи", new SystemParamsCategories("/view/sysParams/objectLinks.xhtml", "objectLinks")},
            {"Агрегаты", new SystemParamsCategories("/view/sysParams/struct/structAggregates.xhtml", "structAggregates")},
            {"Техпроцессы", new SystemParamsCategories("/view/sysParams/struct/structProcesses.xhtml", "structProcesses")},
            {"Оборудование", new SystemParamsCategories("/view/sysParams/struct/structDevices.xhtml", "structDevices")},
            {"Единицы измерения", new SystemParamsCategories("/view/sysParams/measure.xhtml", "measureMB")},
            {"Справочники", new SystemParamsCategories("/view/sysParams/catalog.xhtml", "catalog")},
            {"Температурные графики", new SystemParamsCategories("/view/sysParams/temperature/tempGraphs.xhtml", "tempGraphs")},
            {"Суточные снижения", new SystemParamsCategories("/view/sysParams/temperature/dailyReduction.xhtml", "dailyReduction")},
            {"Коэффициенты для режимной карты", new SystemParamsCategories("/view/sysParams/coefficientsForRegimeCard.xhtml", "coefficientsRC")},
            {"Расцветка параметров", new SystemParamsCategories("/view/sysParams/paramColor.xhtml", "paramColor")},
            {"Приоритет проблем", new SystemParamsCategories("/view/sysParams/problemPriority.xhtml", "problemPriority")},
            {"Нормативные показатели", new SystemParamsCategories("/view/sysParams/normIndicators.xhtml", "normIndicators")},
            {"Среднемесячная температура", new SystemParamsCategories("/view/sysParams/multiYearTemp.xhtml", "multiYearTemp")},
            {"Основные параметры", new SystemParamsCategories("/view/sysParams/mainParam.xhtml", "mainParamMB")},
            {"Статистические агрегаты", new SystemParamsCategories("/view/sysParams/statAggr.xhtml", "statAggrMB")},
            {"Переключение сезона", new SystemParamsCategories("/view/sysParams/seasonChange.xhtml", "seasonChangeMB")},
            {"Настройка типа параметра", new SystemParamsCategories("/view/sysParams/paramTypeSetting.xhtml", "paramTypeSetting")},
            {"Обобщенная модель", new SystemParamsCategories("/view/sysParams/genModel.xhtml", "genModelMB")},
            {"Критерии работоспособности аналоговых датчиков", new SystemParamsCategories("/view/sysParams/analogSensor.xhtml", "analogSensor")},
            {"Прочее", new SystemParamsCategories("/view/sysParams/other.xhtml")},
            {"Фоновые процессы", new SystemParamsCategories("/view/sysParams/jobs.xhtml", "jobs")},
            {"Источники данных", new SystemParamsCategories("/view/sysParams/dataSource.xhtml", "dataSourceMB")},
            {"Интеграция", new SystemParamsCategories("/view/sysParams/integration.xhtml", "integrationMB")}
    }).collect(Collectors.toMap(k -> (String) k[0], v -> (SystemParamsCategories) v[1], (oldValue, newValue) -> oldValue, LinkedHashMap::new));

    private String content = "";

    private String selectedButton;
    private Integer oldSelectedButtonIndex;

    @Inject
    private transient Logger logger;

    @Inject
    private SystemParamsUtilMB utilMB;

    @PostConstruct
    private void init() {
        // По факту это заглушка для запуска инициализации контроллера SystemParamsUtilMB
        logger.log(Level.INFO, "Init data {0}", utilMB);
    }

    private void updateContent(String parameter) {
        content = SYSTEM_PARAMS_MAP.get(parameter).getPath();
        LOGGER.info("update content: " + content);
    }

    /**
     * Проверка выбран ли элемент меню
     *
     * @param item элемент меню
     * @return стиль для кнопки
     */
    public String checkSelected(String item) {
        if (selectedButton != null) {
            return selectedButton.equals(item) ? "ui-button-selected" : "ui-button-flat";
        } else {
            return "ui-button-flat";
        }
    }

    /**
     * Обработка нажатия кнопки
     *
     * @param item  имя меню
     * @param index индекс меню
     */
    public void selectButton(String item, int index) {
        String beanName = SYSTEM_PARAMS_MAP.get(item).getBeanName();
        if (!beanName.isEmpty()) {
            Object bean = AdmTools.findBean(SYSTEM_PARAMS_MAP.get(item).getBeanName());
            if (bean instanceof AutoUpdate) {
                ((AutoUpdate) bean).update();
            }
        }

        updateContent(item);
        selectedButton = item;
        if (oldSelectedButtonIndex != null) {
            PrimeFaces.current().ajax().update("menuForm:button_" + oldSelectedButtonIndex);
        }
        oldSelectedButtonIndex = index;
        PrimeFaces.current().ajax().update("menuForm:button_" + index, "paramPanel");
    }

    public String getContent() {
        return content;
    }

    public Set<String> getParameters() {
        return SYSTEM_PARAMS_MAP.keySet();
    }
}
