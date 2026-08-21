package ru.tecon.admTools.components.navigation;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.NamingContainer;
import jakarta.faces.component.UIComponentBase;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AjaxBehaviorEvent;
import org.primefaces.PrimeFaces;
import org.primefaces.component.inputtext.InputText;
import org.primefaces.component.selectonemenu.SelectOneMenu;
import ru.tecon.admTools.components.navigation.ejb.NavigationBeanLocal;
import ru.tecon.admTools.components.navigation.model.LazyLoadingTreeNode;
import ru.tecon.admTools.components.navigation.model.ObjTypePropertyModel;
import ru.tecon.admTools.components.navigation.model.SpObject;
import ru.tecon.admTools.components.navigation.model.TreeNodeModel;
import ru.tecon.admTools.systemParams.cdi.SystemParamsUtilMB;
import ru.tecon.admTools.systemParams.cdi.scope.application.ObjectTypeController;
import ru.tecon.admTools.systemParams.model.ObjectType;
import ru.tecon.admTools.utils.AdmTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Обработчик компоненты "Навигация"
 *
 * @author Maksim Shchelkonogov
 * 07.09.2023
 */
@FacesComponent("navigationComponentWS")
public class NavigationComponentWS extends UIComponentBase implements NamingContainer {

    private static final String DEFAULT_SP_NAME = "Выберите справочник";

    private static final Logger logger = Logger.getLogger(NavigationComponentWS.class.getName());

    private InputText searchTextUI;
    private SelectOneMenu selectedObjectTypeUI;

    /**
     * Returns the component family of {@link UINamingContainer}.
     * (that's just required by composite component)
     */
    @Override
    public String getFamily() {
        return UINamingContainer.COMPONENT_FAMILY;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        logger.log(Level.INFO, "Navigate component params \"bean: {0}, showOrgTree: {1}, showTerTree: {2}, showLinkTree: {3}\"",
                new Object[]{getEjb(),
                        getAttributeValue("showTerTree", false),
                        getAttributeValue("showLinkTree", false)});

        getStateHelper().put("selectedTab", "org");

        // Установка выбранного типа объекта
        ObjectTypeController bean = AdmTools.findBean("objectTypeControllerApplication");
        selectedObjectTypeUI.setValue(bean.getDefaultObjectType());

        // Установка текста поиска, списка свойств поиска, выбранное свойство поиска
        searchTextUI.setValue("");
        setSearchList(getEjb().getObjTypeProps(bean.getDefaultObjectType().getId()));
        setSelectedSearch(getSearchList().get(0).getObjTypeId());

        // Устанавливаем дерево организационной структуры
        setOrgTree(new LazyLoadingTreeNode<>(getEjb().getRootOrgTree(), getTreeLoadFunction()));

        super.encodeBegin(context);
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        super.encodeEnd(context);

        openFirstNode();
    }

    /**
     * Очистка данных компоненты
     */
    public void clearNavigate() {
        ObjectTypeController bean = AdmTools.findBean("objectTypeControllerApplication");
        selectedObjectTypeUI.setValue(bean.getDefaultObjectType());

        searchTextUI.setValue("");
        setSearchList(getEjb().getObjTypeProps(((ObjectType) selectedObjectTypeUI.getValue()).getId()));
        setSelectedSearch(getSearchList().get(0).getObjTypeId());

        getOrgTree().getChildren().clear();
    }

    /**
     * Перезагрузка данных компоненты
     */
    public void reloadNavigate() {
        boolean reload = Boolean.parseBoolean(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("reloadObjectType"));

        if (reload) {
            ObjectTypeController bean = AdmTools.findBean("objectTypeControllerApplication");
            selectedObjectTypeUI.setValue(bean.getDefaultObjectType());
        }

        searchTextUI.setValue("");
        setSearchList(getEjb().getObjTypeProps(((ObjectType) selectedObjectTypeUI.getValue()).getId()));
        setSelectedSearch(getSearchList().get(0).getObjTypeId());

        reloadOrgTree();
    }

    /**
     * Перезагрузка дерева организационной структуры
     */
    private void reloadOrgTree() {
        LazyLoadingTreeNode<TreeNodeModel> orgTree = getOrgTree();
        orgTree.setLoadFunction(getTreeLoadFunction());
        orgTree.reload();

        openFirstNode();
    }

    /**
     * Получение функции загрузки данных для дерева организационной структуры
     *
     * @return функция загрузки данных для дерева организационной структуры
     */
    private Function<String, List<TreeNodeModel>> getTreeLoadFunction() {
        SystemParamsUtilMB utilMB = AdmTools.findBean("systemParamsUtil");
        String login = utilMB.getLogin();

        switch (getSelectedTab()) {
            case "org":
                return s -> getEjb().getOrgTree(((ObjectType) selectedObjectTypeUI.getValue()).getId(), getSelectedSearch(), String.valueOf(searchTextUI.getValue()), login, s);
            case "ter":
                return s -> getEjb().getTerTree(((ObjectType) selectedObjectTypeUI.getValue()).getId(), getSelectedSearch(), String.valueOf(searchTextUI.getValue()), login, s);
            case "link":
                return s -> getEjb().getLinkTree(((ObjectType) selectedObjectTypeUI.getValue()).getId(), getSelectedSearch(), String.valueOf(searchTextUI.getValue()), login, s);
            default:
                return s -> new ArrayList<>();
        }
    }

    /**
     * Раскрытие первого уровня дерева организационной структуры
     */
    public void openFirstNode() {
        for (int i = 0; i < getOrgTree().getChildCount(); i++) {
            PrimeFaces.current().executeScript("PF('navigateOrgTreeWidget').expandNode($(PF('navigateOrgTreeWidget').jqId + '\\\\:" + i + "'))");
        }

        if (getOrgTree().getChildCount() == 0) {
            PrimeFaces.current().executeScript("PF('navigateBui').hide();");
        }
    }

    /**
     * Ajax обработчик выбора типа объекта
     *
     * @param ignore событие
     */
    public void onObjectTypeChange(AjaxBehaviorEvent ignore) {
        // Установка текста поиска, списка свойств поиска, выбранное свойство поиска
        searchTextUI.setValue("");
        setSearchList(getEjb().getObjTypeProps(((ObjectType) selectedObjectTypeUI.getValue()).getId()));
        setSelectedSearch(getSearchList().get(0).getObjTypeId());

        reloadOrgTree();
    }

    /**
     * Ajax обработчик выбора свойства поиска
     *
     * @param ignore событие
     */
    public void onSearchTypeChange(AjaxBehaviorEvent ignore) {
        // Установка текста поиска
        searchTextUI.setValue("");

        if (isSearchBySp()) {
            setSearchBySpName("");
            handleFilterChange();
            PrimeFaces.current().executeScript("$('#linkerForm\\\\:linkerTabView\\\\:navigate\\\\:overlay .ui-button-text').text('" + DEFAULT_SP_NAME + "')");
        }

        reloadOrgTree();
    }

    /**
     * Ajax обработчик изменения текста поиска
     */
    public void onSearch() {
        reloadOrgTree();
    }

    /**
     * Получение текста подсказки свойства поиска
     *
     * @return текст подсказки
     */
    public String getSearchTypeName() {
        long selectedSearch = getSelectedSearch();

        List<ObjTypePropertyModel> searchList = getSearchList();

        for (ObjTypePropertyModel item: searchList) {
            if (item.getObjTypeId() == selectedSearch) {
                return item.getObjTypeValue();
            }
        }
        return "";
    }

    /**
     * Метод вызывается с сервера при каждом изменении текста в поиске
     */
    public void handleFilterChange() {
        setSearchBySpList(getEjb().getSpData(getSearchBySpId(), getSearchBySpName()));
    }

    /**
     * Метод вызывается с сервера при изменении выбора значения в списке справочников
     */
    public void handleValueChange() {
        Long selectedSp = getSelectedSp();
        for (SpObject spObject: getSearchBySpList()) {
            if (spObject.getId() == selectedSp) {
                PrimeFaces.current().executeScript("$('#linkerForm\\\\:linkerTabView\\\\:navigate\\\\:overlay .ui-button-text').text('" + spObject.getName() + "')");
                break;
            }
        }

        searchTextUI.setValue(selectedSp);

        reloadOrgTree();
    }

    /**
     * Получение признака поиска по справочнику
     *
     * @return признак поиска по справочнику
     */
    public boolean isSearchBySp() {
        long selectedSearch = getSelectedSearch();
        List<ObjTypePropertyModel> searchList = getSearchList();

        for (ObjTypePropertyModel item: searchList) {
            if (item.getObjTypeId() == selectedSearch) {
                return item.getSpHeaderId() != null;
            }
        }
        return false;
    }

    /**
     * Получение значения справочника
     *
     * @return значение справочника
     */
    public Integer getSearchBySpId() {
        long selectedSearch = getSelectedSearch();
        List<ObjTypePropertyModel> searchList = getSearchList();

        for (ObjTypePropertyModel item: searchList) {
            if (item.getObjTypeId() == selectedSearch) {
                return item.getSpHeaderId();
            }
        }
        return null;
    }

    public String isActiveTree(String treeType) {
        return getSelectedTab().equals(treeType) ? "active" : "";
    }

    /**
     * Вспомогательный метод для получения свойств компоненты
     *
     * @param key ключ свойства
     * @param defaultValue значение по умолчанию
     * @return значение свойства
     * @param <T> тип свойства
     */
    @SuppressWarnings("unchecked")
    private <T> T getAttributeValue(String key, T defaultValue) {
        T value = (T) getAttributes().get(key);
        return (value != null) ? value : defaultValue;
    }

    private NavigationBeanLocal getEjb() {
        return getAttributeValue("bean", null);
    }

    public long getSelectedSearch() {
        return (long) getStateHelper().get("selectedSearch");
    }

    public void setSelectedSearch(long selectedSearch) {
        getStateHelper().put("selectedSearch", selectedSearch);
    }

    public Long getSelectedSp() {
        return (Long) getStateHelper().get("selectedSp");
    }

    public void setSelectedSp(Long selectedSp) {
        getStateHelper().put("selectedSp", selectedSp);
    }

    public InputText getSearchTextUI() {
        return searchTextUI;
    }

    public void setSearchTextUI(InputText searchTextUI) {
        this.searchTextUI = searchTextUI;
    }

    @SuppressWarnings("unchecked")
    public List<ObjTypePropertyModel> getSearchList() {
        return (List<ObjTypePropertyModel>) getStateHelper().get("searchList");
    }

    public void setSearchList(List<ObjTypePropertyModel> searchList) {
        getStateHelper().put("searchList", searchList);
    }

    public String getSelectedTab() {
        return (String) getStateHelper().get("selectedTab");
    }

    public String getSearchBySpName() {
        return (String) getStateHelper().get("searchBySpName");
    }

    public void setSearchBySpName(String searchBySpName) {
        getStateHelper().put("searchBySpName", searchBySpName);
    }

    @SuppressWarnings("unchecked")
    public List<SpObject> getSearchBySpList() {
        return (List<SpObject>) getStateHelper().get("searchBySpList");
    }

    public void setSearchBySpList(List<SpObject> searchBySpList) {
        getStateHelper().put("searchBySpList", searchBySpList);
    }

    public void setSelectedTab(String selectedTab) {
        getStateHelper().put("selectedTab", selectedTab);

        switch (selectedTab) {
            case "org":
                setOrgTree(new LazyLoadingTreeNode<>(getEjb().getRootOrgTree(), getTreeLoadFunction()));
                break;
            case "ter":
                setOrgTree(new LazyLoadingTreeNode<>(getEjb().getRootTerTree(), getTreeLoadFunction()));
                break;
            case "link":
                setOrgTree(new LazyLoadingTreeNode<>(getEjb().getRootLinkTree(), getTreeLoadFunction()));
                break;
        }
    }

    @SuppressWarnings("unchecked")
    public LazyLoadingTreeNode<TreeNodeModel> getOrgTree() {
        return (LazyLoadingTreeNode<TreeNodeModel>) getStateHelper().get("orgTree");
    }

    public void setOrgTree(LazyLoadingTreeNode<TreeNodeModel> orgTree) {
        getStateHelper().put("orgTree", orgTree);
    }

    public boolean isEmptyOrgTree() {
        return getOrgTree().getChildCount() == 0;
    }

    public SelectOneMenu getSelectedObjectTypeUI() {
        return selectedObjectTypeUI;
    }

    public void setSelectedObjectTypeUI(SelectOneMenu selectedObjectTypeUI) {
        this.selectedObjectTypeUI = selectedObjectTypeUI;
    }
}
