package ru.tecon.admTools.linker.cdi.scope.view;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ValueChangeEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import org.primefaces.PrimeFaces;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.inputtext.InputText;
import org.primefaces.component.tree.Tree;
import org.primefaces.component.tree.TreeDragDropInfo;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.primefaces.model.TreeNode;
import org.primefaces.util.LangUtils;
import ru.tecon.admTools.components.navigation.ejb.NavigationBeanLocal;
import ru.tecon.admTools.components.navigation.model.LazyLoadingTreeNode;
import ru.tecon.admTools.components.navigation.model.TreeNodeModel;
import ru.tecon.admTools.linker.cdi.converter.LinkSchemaDataConverterWS;
import ru.tecon.admTools.linker.cdi.converter.LinkedDataConverterWS;
import ru.tecon.admTools.linker.cdi.converter.OpcObjectForLinkDataConverterWS;
import ru.tecon.admTools.linker.cdi.converter.OpcObjectForNoLinkDataConverterWS;
import ru.tecon.admTools.linker.ejb.LinkerStateless;
import ru.tecon.admTools.linker.model.*;
import ru.tecon.admTools.systemParams.SystemParamException;
import ru.tecon.admTools.systemParams.cdi.SystemParamsUtilMB;
import ru.tecon.admTools.systemParams.cdi.scope.application.ObjectTypeController;
import ru.tecon.admTools.systemParams.model.ObjectType;
import ru.tecon.admTools.utils.TeconMessage;

import java.io.Serializable;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Контроллер для формы "Линковщик"
 *
 * @author Maksim Shchelkonogov
 * 07.07.2023
 */
@Named("linkerControllerWS")
@ViewScoped
public class LinkerControllerWS implements Serializable {

    private boolean inIframe;

    private final Map<String, UIComponent> componentMap = new HashMap<>();

    private boolean cheat = false;

    // Закладка "Линкованные объекты / Объекты"

    private ObjectType selectedObjectType;

    private DataTable linkedTable;
    private final LazyLinkedDataModel<LinkedData> linkedData = new LazyLinkedDataModel<>(
            new LinkedDataConverterWS(),
            new ArrayList<>(),
            Stream.of(new Object[][]{
                    {"dbObject.name", SortOrder.ASCENDING},
                    {"opcObject.name", SortOrder.ASCENDING}
            }).collect(Collectors.toMap(k -> (String) k[0], v -> (SortOrder) v[1], (oldValue, newValue) -> oldValue, LinkedHashMap::new))
    );
    private LinkedData selectedLinkedData;
    private Integer selectedLinkedObjectId;

    private String newMaskName;

    private final LazyLinkedDataModel<OpcObjectForLinkData> opcObjectsForLinkData = new LazyLinkedDataModel<>(
            new OpcObjectForLinkDataConverterWS(),
            new ArrayList<>()
    );
    private List<OpcObjectForLinkData> selectedOpcObjectsForLink = new ArrayList<>();

    private final List<String> templateLinkNames = new ArrayList<>();

    // Закладка "Линкованные объекты / Вычислимые параметры"

    private Tree calcTree;
    private InputText calcTreeFilter;

    private final TreeNode<TreeData> rootCalcTree = new DefaultTreeNode<>(new TreeData(), null);
    private TreeNode<TreeData> selectedCalcTreeNode;

    private String filterCalcTreeValue = "";

    private List<CalcDataTable> calcDataTableList = new ArrayList<>();

    // Закладка "Линкованные объекты / Параметры"

    private static final String DEFAULT_MODEL_TYPE = "Обобщенная модель";

    private Tree paramTree;
    private InputText paramTreeFilter;

    private final TreeNode<TreeData> rootKmOmTree = new DefaultTreeNode<>(new TreeData(), null);
    private TreeNode<TreeData> selectedParamTreeNode;

    private String filterParamTreeValue = "";
    private String paramTreePanelHeader = DEFAULT_MODEL_TYPE;

    private Tree paramOpcTree;
    private InputText paramOpcTreeFilter;

    private final TreeNode<TreeData> rootOpcTree = new DefaultTreeNode<>(new TreeData(), null);
    private TreeNode<TreeData> selectedParamOpcTreeNode;

    private String filterParamOpcTreeValue = "";
    private String paramOpcTreePanelHeader = "Все параметры";

    // Данные для перехода на Источники данных

    private final List<Redirect> redirectList = new ArrayList<>();

    // Данные "Нелинкованные объекты"

    private final LazyLinkedDataModel<OpcObjectForLinkData> opcObjectsForNoLinkData = new LazyLinkedDataModel<>(
            new OpcObjectForNoLinkDataConverterWS(),
            new ArrayList<>()
    );
    private OpcObjectForLinkData selectedOpcObjectsForNoLink = null;

    private LazyLoadingTreeNode<TreeNodeModel> selectedNavigateObject = null;

    private final Set<OpcObjectForLinkData> objectsForLink = new HashSet<>();

    private final LazyLinkedDataModel<OpcObjectForLinkData> opcObjectParams = new LazyLinkedDataModel<>(
            new OpcObjectForLinkDataConverterWS(),
            new ArrayList<>()
    );

    private final LazyLinkedDataModel<LinkSchemaData> linkSchemaTable = new LazyLinkedDataModel<>(
            new LinkSchemaDataConverterWS(),
            new ArrayList<>()
    );
    private LinkSchemaData selectedLinkSchema = null;

    @Inject
    private ObjectTypeController objectTypeController;

    @Inject
    private transient Logger logger;

    @Inject
    private SystemParamsUtilMB utilMB;

    @EJB
    private LinkerStateless linkerBean;

    @EJB(beanName = "navigationBean")
    private NavigationBeanLocal navigationBean;

    @PostConstruct
    private void init() {
        // По факту это заглушка для запуска инициализации контроллера SystemParamsUtilMB
        logger.log(Level.INFO, "Init data {0}", utilMB);

        selectedObjectType = objectTypeController.getDefaultObjectType();

        PrimeFaces.current().executeScript("PF('objectTabViewWidget').disable(1); PF('objectTabViewWidget').disable(2);");

        redirectList.addAll(linkerBean.getRedirect());

        // Загрузка объектов для линковки
        loadOpcObjectsForNoLink();
    }

    /**
     * Обработчик изменения выбранной закладки
     *
     * @param event событие изменения закладки
     */
    public void onTabChange(TabChangeEvent<?> event) {
        logger.log(Level.INFO, "Tab changed. Active Tab {0}", event.getTab().getTitle());

        switch (event.getTab().getTitle()) {
            case "Нелинкованные объекты":
                cheat = false;
                // Загрузка объектов для линковки
                loadOpcObjectsForNoLink();

                // Очищаю вкладку "Линкованные объекты / Объекты"
                linkedData.getData().clear();
                selectedLinkedData = null;
                selectedLinkedObjectId = null;

                // Очищаю вкладку "Линкованные объекты / Вычислимые параметры"
                filterCalcTreeValue = "";
                if (selectedCalcTreeNode != null) {
                    PrimeFaces.current().executeScript("unselectNode('calcTreeWidget', false);");
                    selectedCalcTreeNode = null;
                }
                rootCalcTree.getChildren().clear();
                calcDataTableList.clear();

                // Очищаю вкладку "Линкованные объекты / Параметры"
                filterParamTreeValue = "";
                paramTreePanelHeader = DEFAULT_MODEL_TYPE;
                if (selectedParamTreeNode != null) {
                    PrimeFaces.current().executeScript("unselectNode('paramTreeWidget', false);");
                    selectedParamTreeNode = null;
                }
                rootKmOmTree.getChildren().clear();
                filterParamOpcTreeValue = "";
                paramOpcTreePanelHeader = "Все параметры";
                if (selectedParamOpcTreeNode != null) {
                    PrimeFaces.current().executeScript("unselectNode('paramOpcTreeWidget', false);");
                    selectedParamOpcTreeNode = null;
                }
                rootOpcTree.getChildren().clear();

                PrimeFaces.current().ajax().update("linkerForm:linkerTabView:objectTabView:linkedDataTable",
                        "linkerForm:linkerTabView:objectTabView:calcDataTable",
                        "linkerForm:linkerTabView:objectTabView:settingCalcBar:removeCalcLinkBtn",
                        "linkerForm:linkerTabView:objectTabView:settingCalcBar:createCalcLinkBtn",
                        "linkerForm:linkerTabView:objectTabView:filterCalcTree",
                        "linkerForm:linkerTabView:objectTabView:filterParamTree",
                        "linkerForm:linkerTabView:objectTabView:filterParamOpcTree",
                        "linkerForm:linkerTabView:objectTabView:paramTreePanelHeader",
                        "linkerForm:linkerTabView:objectTabView:paramOpcTreePanelHeader");

                PrimeFaces.current().executeScript("filterTree([{name:'widget', value:'calcTreeWidget'}]); " +
                        "filterTree([{name:'widget', value:'paramTreeWidget'}]); " +
                        "filterTree([{name:'widget', value:'paramOpcTreeWidget'}]); " +
                        "PF('filterParamTreeSelectOneMenuWidget').selectValue(2); " +
                        "PF('filterParamOpcTreeSelectOneMenuWidget').selectValue(1); " +
                        "PF('objectTabViewWidget').select(0, true); " +
                        "PF('objectTabViewWidget').disable(1); " +
                        "PF('objectTabViewWidget').disable(2); " +
                        "updateEmptyRow(); " +
                        "PF('opcObjectsForNoLinkTableWidget').filter(); " +
                        "reloadNavigate();");
                break;
            case "Линкованные объекты":
                cheat = false;
                selectedOpcObjectsForNoLink = null;
                opcObjectsForNoLinkData.getData().clear();
                objectsForLink.clear();

                PrimeFaces.current().ajax().update("linkerForm:linkerTabView:settingNoLinkBar:paramBtns",
                        "linkerForm:linkerTabView:selectObjects",
                        "linkerForm:linkerTabView:settingNoLinkBar:linkBtnForNoLink",
                        "linkerForm:linkerTabView:opcObjectsForNoLinkTable");

                PrimeFaces.current().executeScript("PF('objectTabViewWidget').select(0); " +
                        "PF('objectTabViewWidget').disable(1); " +
                        "PF('objectTabViewWidget').disable(2); " +
                        "clearNavigate();");
                break;
            case "Объекты":
                cheat = true;
                linkedData.setData(linkerBean.getLinkedData(selectedObjectType.getId(), utilMB.getLogin()));
                selectedLinkedData = null;

                if (selectedLinkedObjectId != null) {
                    setSelectedLinkedData(linkedData.getData().stream().filter(data -> data.getDbObject().getId() == selectedLinkedObjectId).findFirst().orElse(null));
                    linkerBean.loadRecountData(selectedLinkedData);

                    selectedLinkedObjectId = null;
                }

                // Очищаю вкладку "Линкованные объекты / Вычислимые параметры"
                filterCalcTreeValue = "";
                if (selectedCalcTreeNode != null) {
                    PrimeFaces.current().executeScript("unselectNode('calcTreeWidget', false);");
                    selectedCalcTreeNode = null;
                }
                rootCalcTree.getChildren().clear();
                calcDataTableList.clear();

                // Очищаю вкладку "Линкованные объекты / Параметры"
                paramTreePanelHeader = DEFAULT_MODEL_TYPE;
                filterParamTreeValue = "";
                if (selectedParamTreeNode != null) {
                    PrimeFaces.current().executeScript("unselectNode('paramTreeWidget', false);");
                    selectedParamTreeNode = null;
                }
                rootKmOmTree.getChildren().clear();
                filterParamOpcTreeValue = "";
                paramOpcTreePanelHeader = "Все параметры";
                if (selectedParamOpcTreeNode != null) {
                    PrimeFaces.current().executeScript("unselectNode('paramOpcTreeWidget', false);");
                    selectedParamOpcTreeNode = null;
                }
                rootOpcTree.getChildren().clear();

                PrimeFaces.current().ajax().update("linkerForm:linkerTabView:objectTabView:calcDataTable",
                        "linkerForm:linkerTabView:objectTabView:settingCalcBar:removeCalcLinkBtn",
                        "linkerForm:linkerTabView:objectTabView:settingCalcBar:createCalcLinkBtn",
                        "linkerForm:linkerTabView:objectTabView:filterCalcTree",
                        "linkerForm:linkerTabView:objectTabView:filterParamTree",
                        "linkerForm:linkerTabView:objectTabView:filterParamOpcTree",
                        "linkerForm:linkerTabView:objectTabView:paramTreePanelHeader",
                        "linkerForm:linkerTabView:objectTabView:paramOpcTreePanelHeader");

                if (selectedLinkedData == null) {
                    PrimeFaces.current().executeScript("PF('objectTabViewWidget').disable(1); " +
                            "PF('objectTabViewWidget').disable(2);");
                }

                PrimeFaces.current().executeScript("filterTree([{name:'widget', value:'calcTreeWidget'}]); " +
                        "filterTree([{name:'widget', value:'paramTreeWidget'}]); " +
                        "filterTree([{name:'widget', value:'paramOpcTreeWidget'}]); " +
                        "addOnDblClickEvents(); " +
                        "PF('filterParamTreeSelectOneMenuWidget').selectValue(2); " +
                        "PF('filterParamOpcTreeSelectOneMenuWidget').selectValue(1);");
                break;
            case "Параметры":
                cheat = false;
                linkedData.getData().clear();

                if (selectedLinkedData != null) {
                    selectedLinkedObjectId = selectedLinkedData.getDbObject().getId();

                    selectedLinkedData = null;
                }

                if ((selectedLinkedObjectId != null) && (rootKmOmTree.getChildren().isEmpty())) {
                    prepareTree(linkerBean.getTreeData(selectedLinkedObjectId, TreeType.OM), rootKmOmTree);

                    prepareTree(linkerBean.getTreeData(selectedLinkedObjectId, TreeType.OPC), rootOpcTree, null);
                }

                PrimeFaces.current().executeScript("document.getElementById('linkerForm:linkerTabView:objectTabView:paramTree_filter').value = 'init'; " +
                        "filterTree([{name:'widget', value:'paramTreeWidget'}]); " +
                        "document.getElementById('linkerForm:linkerTabView:objectTabView:paramOpcTree_filter').value = 'init'; " +
                        "filterTree([{name:'widget', value:'paramOpcTreeWidget'}]);");
                break;
            case "Вычислимые параметры":
                cheat = false;
                linkedData.getData().clear();

                if (selectedLinkedData != null) {
                    selectedLinkedObjectId = selectedLinkedData.getDbObject().getId();

                    selectedLinkedData = null;
                }

                if ((selectedLinkedObjectId != null) && (rootCalcTree.getChildren().isEmpty())) {
                    List<TreeData> treeData = linkerBean.getTreeData(selectedLinkedObjectId, TreeType.CALC);

                    prepareTree(treeData, rootCalcTree);
                }

                PrimeFaces.current().ajax().update("linkerForm:linkerTabView:objectTabView:calcDataTable");
                PrimeFaces.current().executeScript("document.getElementById('linkerForm:linkerTabView:objectTabView:calcTree_filter').value = 'init'; " +
                        "filterTree([{name:'widget', value:'calcTreeWidget'}]);");
                break;
        }
    }

    /**
     * Метод раскрывает все элементы дерева
     * @param tree дерево
     */
    public void doExpandAll(TreeNode<?> tree) {
        for (TreeNode<?> element: tree.getChildren()) {
            collapsingOrExpanding(element, true);
        }
    }

    /**
     * Метод скрывает все элементы дерева
     * @param tree дерево
     */
    public void doCollapseAll(TreeNode<?> tree) {
        for (TreeNode<?> element: tree.getChildren()) {
            collapsingOrExpanding(element, false);
        }
    }

    public void cheatDOOM() {
        if (cheat && (selectedLinkedData != null)) {
            PrimeFaces.current().ajax().update("cheatDOOMForm:objectCheatDOOMName");
            PrimeFaces.current().executeScript("PF('cheatDOOMDialogWidget').show(); " +
                    "PF('doom1Dialog').show(); " +
                    "PF('doom2Dialog').show(); " +
                    "PF('doom3Dialog').show(); " +
                    "PF('doom4Dialog').show(); " +
                    "playSound();");
        }
    }

    public void closeCheatDOOM() {
        if (cheat) {
            PrimeFaces.current().executeScript(
                    "PF('doom1Dialog').hide(); " +
                            "PF('doom2Dialog').hide(); " +
                            "PF('doom3Dialog').hide(); " +
                            "PF('doom4Dialog').hide(); " +
                            "stopSound();");
        }
    }

    public void cheat() {
        if (cheat && (selectedLinkedData != null)) {
            PrimeFaces.current().ajax().update("cheatDialogHeader");
            PrimeFaces.current().executeScript("PF('cheatDialogWidget').show();");
        }
    }

    public void enableCheat1(boolean doom) {
        logger.log(Level.INFO, "Применяем код 1 для " + selectedLinkedData.getDbObject());
        try {
            linkerBean.cheat1(selectedLinkedData.getDbObject().getId(), selectedLinkedData.getServerName(), selectedObjectType.getId());

            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_SUCCESS, "Линковка",
                        doom ? "Успешное применение кода: 'Параметры привязаны к устройству'" : "Параметры привязаны к устройству").send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null,
                                new FacesMessage(FacesMessage.SEVERITY_INFO, "Линковка",
                                        doom ? "Успешное применение кода: 'Параметры привязаны к устройству'" : "Параметры привязаны к устройству"));
            }
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Линковка", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Линковка", e.getMessage()));
            }
        }
        if (doom) {
            closeCheatDOOM();
        }
    }

    private void collapsingOrExpanding(TreeNode<?> node, boolean option) {
        if (node.getChildren().size() == 0) {
            node.setSelected(false);
        } else {
            for (TreeNode<?> child: node.getChildren()) {
                collapsingOrExpanding(child, option);
            }
            node.setExpanded(option);
            node.setSelected(false);
        }
    }

    /**
     * Создание дерева на основе плоского списка данных
     *
     * @param treeData плоский список данных
     * @param root     корневой элемент дерева
     */
    private void prepareTree(List<TreeData> treeData, TreeNode<TreeData> root) {
        prepareTree(treeData, root, "S");
    }

    /**
     * Создание дерева на основе плоского списка данных с указанием идентификатора корневого элемента
     *
     * @param treeData   плоский список данных
     * @param root       корневой элемент дерева
     * @param rootPrefix идентификатор корневого элемента
     */
    private void prepareTree(List<TreeData> treeData, TreeNode<TreeData> root, String rootPrefix) {
        Map<String, TreeNode<TreeData>> nodes = new HashMap<>();
        nodes.put(rootPrefix, root);
        for (TreeData entry: treeData) {
            TreeNode<TreeData> parent = nodes.get(entry.getParent());
            DefaultTreeNode<TreeData> node = new DefaultTreeNode<>(entry.getMyType(), entry, parent);
            nodes.put(entry.getItemId(), node);
        }

        // сортировка дерева
        sortTree(root);

        // раскрытие первого уровня
//        for (TreeNode<TreeData> node: root.getChildren()) {
//            if (!node.isLeaf()) {
//                node.setExpanded(true);
//            }
//        }
    }

    /**
     * Сортировка дерева по имени
     *
     * @param treeNode узел дерева для сортировки вниз
     */
    private void sortTree(TreeNode<TreeData> treeNode) {
        if (!treeNode.getChildren().isEmpty()) {
            treeNode.getChildren().sort(Comparator.comparing(o -> o.getData().getName()));

            for (int i = 0; i < treeNode.getChildren().size(); i++) {
                treeNode.getChildren().get(i).setRowKey(
                        treeNode.getRowKey().equals("root") ?
                                String.valueOf(i) : treeNode.getRowKey() + "_" + i);
                sortTree(treeNode.getChildren().get(i));
            }
        }
    }

    public void customFilter() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String widget = params.get("widget");
        boolean updateSelect = Boolean.parseBoolean(params.get("updateSelect"));

        if ((widget != null) &&
                componentMap.containsKey(widget) && (componentMap.get(widget) instanceof Tree) &&
                componentMap.containsKey(widget + "Filter") && (componentMap.get(widget + "Filter") instanceof InputText)) {
            Tree tree = (Tree) componentMap.get(widget);
            InputText filter = (InputText) componentMap.get(widget + "Filter");

            String selectRowKey = params.get("selectRowKey") == null ? tree.getSelectedRowKeysAsString() : params.get("selectRowKey");

            if (filter.getValue().toString().isEmpty()) {
                doCollapseAll(tree.getValue());

                if (updateSelect && (selectRowKey != null)) {
                    String[] s = selectRowKey.split("_");
                    TreeNode<?> value = tree.getValue();
                    for (String index: s) {
                        value = value.getChildren().get(Integer.parseInt(index));
                        if (!value.isLeaf()) {
                            value.setExpanded(true);
                        } else {
                            value.setSelected(true);
                        }
                    }

                    PrimeFaces.current().executeScript("PF('" + widget + "').unselectAllNodes(); " +
                            "PF('" + widget + "').selections=['" + selectRowKey + "']; " +
                            "selectNode('" + widget + "', true); " +
                            "scrollToSelectedNode('" + widget + "');");
                }

                PrimeFaces.current().ajax().update(tree.getClientId());
                PrimeFaces.current().executeScript("document.getElementById('" + tree.getClientId() + "_filter').value = 'init';");
                if (tree.isDraggable() && tree.isDraggable()) {
                    PrimeFaces.current().executeScript("initDragDropForTree('" + widget + "');");
                }
            } else {
                if (updateSelect && (selectRowKey != null)) {
                    PrimeFaces.current().executeScript("PF('" + widget + "').unselectAllNodes(); " +
                            "PF('" + widget + "').filter(); " +
                            "PF('" + widget + "').selections=['" + selectRowKey + "']; " +
                            "selectNode('" + widget + "', true); " +
                            "scrollToSelectedNode('" + widget + "');");
                } else {
                    PrimeFaces.current().executeScript("PF('" + widget + "').filter();");
                }
            }
        }
    }

    /**
     * Фильтр дерева в закладке "Линкованные объекты / Вычислимые параметры" и "Линкованные объекты / Параметры"
     *
     * @param treeNode дерево
     * @param ignore   фильтр (не используется)
     * @param locale   locale
     * @return true если проходит фильтрацию
     */
    public boolean customFilter(TreeNode<TreeData> treeNode, Object ignore, Locale locale) {
        if (treeNode.getData() == null) {
            return true;
        }

        String filterText;
        switch (treeNode.getData().getTreeType()) {
            case CALC:
                filterText = filterCalcTreeValue.trim().toLowerCase(locale);
                break;
            case KM:
            case OM:
                filterText = filterParamTreeValue.trim().toLowerCase(locale);
                break;
            case OPC:
                filterText = filterParamOpcTreeValue.trim().toLowerCase(locale);
                break;
            default:
                filterText = "";
        }

        if (LangUtils.isBlank(filterText)) {
            return true;
        }

        return treeNode.getData().getName().toLowerCase(locale).contains(filterText);
    }

    /**
     * Обработчик события drop в деревья на форме "Линкованные объекты / Параметры"
     *
     * @param info событие dragAndDrop
     * @return возвращает разрешен ли dragAndDrop
     */
    public boolean onDrop(TreeDragDropInfo info) {
        TreeData dragNode = (TreeData) info.getDragNode().getData();
        TreeData dropNode = (TreeData) info.getDropNode().getData();

        if (dropNode.getTreeType() != dragNode.getTreeType()) {
            switch (dropNode.getTreeType()) {
                case OPC:
                    if (dragNode.getMyType().equals("NLSA") && dropNode.getMyType().equals("NLOP")) {
                        logger.log(Level.INFO, "Create link for {0} and {1}", new Object[]{dragNode, dropNode});

                        linkParam(info.getDragNode(), info.getDropNode());
                    }
                    break;
                case OM:
                case KM:
                    if (dragNode.getMyType().equals("NLOP") && dropNode.getMyType().equals("NLSA")) {
                        logger.log(Level.INFO, "Create link for {0} and {1}", new Object[]{dropNode, dragNode});

                        linkParam(info.getDropNode(), info.getDragNode());
                    }
                    break;
            }
        }

        return false;
    }

    /**
     * Обработчик выделения объекта автоматизации в таблице "Нелинкованные объекты"
     * @param event событие выделения
     */
    public void onSelectObjectForLink(SelectEvent<OpcObjectForLinkData> event) {
        objectsForLink.add(event.getObject());
    }

    /**
     * Удаление объекта автоматизации из группы для линковки "Нелинкованные объекты"
     */
    public void removeObjectForLink() {
        String id = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("id");

        objectsForLink.removeIf(linkData -> linkData.getId().equals(id));
    }

    /**
     * Обработчик удаления объекта автоматизации "Нелинкованные объекты"
     */
    public void removeOpcObject() {
        logger.log(Level.INFO, "remove opc object {0}", selectedOpcObjectsForNoLink);

        try {
            linkerBean.removeOpcObject(selectedOpcObjectsForNoLink);

            selectedOpcObjectsForNoLink = null;
            loadOpcObjectsForNoLink();
            objectsForLink.clear();
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Удаление объекта", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Удаление объекта", e.getMessage()));
            }
        }
    }

    /**
     * Очистка параметров выделенного объекта автоматизации "Нелинкованные объекты"
     */
    public void removeOpcObjectParams() {
        logger.log(Level.INFO, "remove opc object params {0}", selectedOpcObjectsForNoLink);

        try {
            linkerBean.removeOpcObjectParams(selectedOpcObjectsForNoLink);

            selectedOpcObjectsForNoLink = null;
            loadOpcObjectsForNoLink();
            objectsForLink.clear();
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Удаление параметров", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Удаление параметров", e.getMessage()));
            }
        }
    }

    /**
     * Запрос параметров для выбранного объекта автоматизации "Нелинкованные объекты"
     */
    public void requestOpcParamForNoLink() {
        logger.log(Level.INFO, "request opc params for object {0}", selectedOpcObjectsForNoLink);

        try {
            Future<Void> future = linkerBean.requestOpcParams(selectedOpcObjectsForNoLink.getOpcObject().getName());

            try {
                future.get(1, TimeUnit.MINUTES);
            } catch (InterruptedException | TimeoutException e) {
                throw new SystemParamException("Внутренняя ошибка сервера");
            } catch (ExecutionException e) {
                if (e.getCause() instanceof SystemParamException) {
                    SystemParamException cause = (SystemParamException) e.getCause();
                    throw new SystemParamException(cause.getMessage());
                } else {
                    throw new SystemParamException("Внутренняя ошибка сервера");
                }
            }

            loadOpcObjectParams();
            PrimeFaces.current().executeScript("PF('showOpcObjectParamsDialogWidget').show(); PF('showOpcObjectParamsTableWidget').filter();");
            PrimeFaces.current().ajax().update("showOpcObjectParamsDialogHeader");
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Запрос параметров", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Запрос параметров", e.getMessage()));
            }
        }
    }

    /**
     * Создание фиктивного узла учета "Нелинкованные объекты"
     */
    public void createFictitiousYY() {
        logger.log(Level.INFO, "create fictitious yy {0}", selectedNavigateObject);

        try {
            linkerBean.createFictitiousYY(selectedNavigateObject.getData().getName());

            selectedOpcObjectsForNoLink = null;
            loadOpcObjectsForNoLink();
            objectsForLink.clear();
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Создание фиктивного узла учета", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Создание фиктивного узла учета", e.getMessage()));
            }
        }
    }

    /**
     * Обработчик закрытия окна с просмотром параметров "Нелинкованные объекты"
     */
    public void onShowOpcObjectParamsDialogClose() {
        selectedOpcObjectsForNoLink.setParamCount(opcObjectParams.getData().size());
        int updateIndex = opcObjectsForNoLinkData.getWrappedData().indexOf(selectedOpcObjectsForNoLink);

        PrimeFaces.current().ajax().update("linkerForm:linkerTabView:opcObjectsForNoLinkTable:" + updateIndex + ":paramCount");

        opcObjectParams.getData().clear();
    }

    /**
     * Загрузка данных для окна с просмотром параметров "Нелинкованные объекты"
     */
    public void loadOpcObjectParams() {
        opcObjectParams.setData(linkerBean.getOpcObjectParams(selectedOpcObjectsForNoLink));
    }

    /**
     * Загрузка данных для таблицы объектов автоматизации "Нелинкованные объекты"
     */
    public void loadOpcObjectsForNoLink() {
        opcObjectsForNoLinkData.setData(linkerBean.getOpcObjectsForLink());
    }

    /**
     * Нажатие на кнопку слинковать, загрузка схем линковок "Нелинкованные объекты"
     */
    public void linkNoLinkedObjects() {
        logger.log(Level.INFO, "link no linked object {0} {1}", new Object[]{selectedNavigateObject, objectsForLink});

        if (linkerBean.checkLicense(selectedNavigateObject.getData().getItemId(), utilMB.getSessionId())) {
            try {
                linkSchemaTable.setData(linkerBean.getSchemaListForLink(selectedNavigateObject.getData().getMyId(), new ArrayList<>(objectsForLink)));

                PrimeFaces.current().executeScript("PF('schemaLinkDialogWidget').show(); PF('schemaLinkTableWidget').filter();");
            } catch (SystemParamException e) {
                if (inIframe) {
                    new TeconMessage(TeconMessage.SEVERITY_ERROR, "Загрузка схем линковки", e.getMessage()).send();
                } else {
                    FacesContext.getCurrentInstance()
                            .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Загрузка схем линковки", e.getMessage()));
                }
            }
        } else {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Загрузка схем линковки", "Отсутствует лицензия на объект").send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Загрузка схем линковки", "Отсутствует лицензия на объект"));
            }
        }
    }

    /**
     * Очистка данных окна выбора схемы линковки "Нелинкованные объекты"
     */
    public void onSchemaLinkDialogClose() {
        linkSchemaTable.getData().clear();
        selectedLinkSchema = null;
    }

    /**
     * Обработка отказа завершать линковку (нажатие на крестик или отмена) "Нелинкованные объекты"
     */
    public void schemaLinkDialogClose() {
        logger.log(Level.INFO, "Close link by schema with \"no\"");
        linkerBean.linkBySchemaNo(selectedNavigateObject.getData().getMyId(), new ArrayList<>(objectsForLink));
    }

    /**
     * Линковка объекта по выбранной схеме "Нелинкованные объекты"
     */
    public void linkBySchemaYes() {
        logger.log(Level.INFO, "Link by schema with \"yes\" {0}", selectedLinkSchema);
        linkerBean.linkBySchemaYes(selectedNavigateObject.getData().getMyId(), new ArrayList<>(objectsForLink), selectedLinkSchema, utilMB.getLogin(), utilMB.getIp());

        selectedOpcObjectsForNoLink = null;
        loadOpcObjectsForNoLink();
        objectsForLink.clear();
    }

    /**
     * Линковка объекта по выбранной схеме без параметров "Нелинкованные объекты"
     */
    public void linkBySchemaNoParam() {
        logger.log(Level.INFO, "Link by schema with \"yes no param\"");
        linkerBean.linkBySchemaNoParam(selectedNavigateObject.getData().getMyId(), new ArrayList<>(objectsForLink), utilMB.getLogin(), utilMB.getIp());

        selectedOpcObjectsForNoLink = null;
        loadOpcObjectsForNoLink();
        objectsForLink.clear();
    }

    /**
     * Линковка двух элементов дерева в форме "Линкованные объекты / Параметры"
     *
     * @param node1 элемент дерева KM/OM
     * @param node2 эелемент дерева OPC
     */
    private void linkParam(TreeNode<?> node1, TreeNode<?> node2) {
        TreeData node1Data = (TreeData) node1.getData();
        TreeData node2Data = (TreeData) node2.getData();

        try {
            String newOpcId = linkerBean.linkParam(selectedLinkedObjectId, node1Data.getItemId(), node2Data.getItemId());

            // Устанавливаем обновленные типы
            paramOpcTree.setSelection(node2);
            node2.setType("LOP");
            node2Data.setMyType("LOP");
            paramTree.setSelection(node1);
            node1.setType("LSA");
            node1Data.setMyType("LSA");

            // Устанавливаем в дерево opc обновленную id
            String newId = node2Data.getItemId().replaceAll("OP\\d+$", "OP" + newOpcId);
            node2Data.setItemId(newId);
            node2Data.setMyId(newId);

            PrimeFaces.current().executeScript("filterTree([{name:'widget', value:'paramTreeWidget'}, {name:'updateSelect', value:'true'}, {name:'selectRowKey', value:'" + paramTree.getSelectedRowKeysAsString() + "'}]); " +
                    "filterTree([{name:'widget', value:'paramOpcTreeWidget'}, {name:'updateSelect', value:'true'}, {name:'selectRowKey', value:'" + paramOpcTree.getSelectedRowKeysAsString() + "'}]);");

            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_SUCCESS, "Линковка", "Успешное создание связи").send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Линковка", "Успешное создание связи"));
            }
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Линковка", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Линковка", e.getMessage()));
            }
        }

        PrimeFaces.current().ajax().update("growl");

        PrimeFaces.current().executeScript("$('.ui-tree-draghelper').remove();");
    }

    /**
     * Разрыв связи выделенных параметров в форме "Линкованные объекты / Параметры"
     */
    public void removeParamLink() {
        logger.log(Level.INFO, "Remove link for {0} and {1}", new Object[]{selectedParamTreeNode, selectedParamOpcTreeNode});
        try {
            if ((selectedParamTreeNode == null) || (selectedParamOpcTreeNode == null)) {
                throw new SystemParamException("Связь не найдена");
            }

            String newOpcId = linkerBean.unlinkParam(selectedLinkedObjectId, selectedParamTreeNode.getData().getItemId(), selectedParamOpcTreeNode.getData().getItemId());

            // Устанавливаем в дерево opc обновленную id
            selectedParamTreeNode.setType("NLSA");
            selectedParamTreeNode.getData().setMyType("NLSA");
            selectedParamOpcTreeNode.setType("NLOP");
            selectedParamOpcTreeNode.getData().setMyType("NLOP");

            // Устанавливаем в дерево opc обновленную id
            String newId = selectedParamOpcTreeNode.getData().getItemId().replaceAll("OP\\d+$", "OP" + newOpcId);
            selectedParamOpcTreeNode.getData().setItemId(newId);
            selectedParamOpcTreeNode.getData().setMyId(newId);

            PrimeFaces.current().executeScript("filterTree([{name:'widget', value:'paramTreeWidget'}, {name:'updateSelect', value:'true'}]); " +
                    "filterTree([{name:'widget', value:'paramOpcTreeWidget'}, {name:'updateSelect', value:'true'}]);");

            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_SUCCESS, "Линковка", "Успешный разрыв связи").send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Линковка", "Успешный разрыв связи"));
            }
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Линковка", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Линковка", e.getMessage()));
            }
        }

        PrimeFaces.current().ajax().update("growl");
    }

    /**
     * Обработчик выделения элементов деревьев в форме "Линкованные объекты / Параметры"
     *
     * @param event событие выделения
     */
    public void onSelectParamTree(NodeSelectEvent event) {
        // Поскольку требуется выделить новую строку, но при этом нельзя запускать ajax,
        //  то пришлось хитро делать выделение.
        // В тихом режиме выключаю выделение по onstart в js.
        // Запуская фильтрацию.
        // Затем запуская js function для тихого выделения и перемещения scroll к выделенному объекту
        //  (тоже хитро, что бы запустилось уже поле onComplete после filter()).

        TreeData treeNodeData = (TreeData) event.getTreeNode().getData();
        switch (treeNodeData.getTreeType()) {
            case OPC:
                if (event.getTreeNode().getType().equals("LOP")) {
                    // find element in "om / km" tree then set expanded, select and scroll to select item
                    logger.log(Level.INFO, "select linked parameter {0} and find linked aggregate in 'om/km' tree", treeNodeData);

                    String omKmParam = linkerBean.findOmKmParam(selectedLinkedObjectId, treeNodeData.getItemId());

                    if (omKmParam == null) {
                        return;
                    }

                    TreeNode<TreeData> node = findNode(rootKmOmTree, omKmParam);

                    if (node != null) {
                        String rowKey = node.getRowKey();

                        doCollapseAll(rootKmOmTree);

                        while (node != null) {
                            if (!node.isLeaf()) {
                                node.setExpanded(true);
                            }
                            node = node.getParent();
                        }

                        setFilterParamTreeValue("");
                        setSelectedParamTreeNode(null);
                        PrimeFaces.current().ajax().update("linkerForm:linkerTabView:objectTabView:filterParamTree",
                                "linkerForm:linkerTabView:objectTabView:paramTree");
                        PrimeFaces.current().executeScript("document.getElementById('linkerForm:linkerTabView:objectTabView:paramTree_filter').value = 'init'; " +
                                "initDragDropForTree('paramTreeWidget');");

                        PrimeFaces.Ajax ajax = PrimeFaces.current().ajax();
                        ajax.addCallbackParam("command", "updateAfterSelectWrapper");
                        ajax.addCallbackParam("widgetName", "paramTreeWidget");
                        ajax.addCallbackParam("rowKey", rowKey);
                    } else {
                        // Бывает хитрый момент, когда связи нет в таблицы dz_par_dev_link, то надо писать сообщение
                        PrimeFaces.current().ajax().update("growl");

                        if (inIframe) {
                            new TeconMessage(TeconMessage.SEVERITY_WARN, "Линковка", "Свяжите параметр с устройством в паспорте объекта").send();
                        } else {
                            FacesContext.getCurrentInstance()
                                    .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Линковка", "Свяжите параметр с устройством в паспорте объекта"));
                        }
                    }
                }
                break;
            case OM:
            case KM:
                if (event.getTreeNode().getType().equals("LSA")) {
                    // find element in "opc" tree then set expanded, select and scroll to select item
                    logger.log(Level.INFO, "select linked aggregate {0} and find linked parameter in 'opc' tree", treeNodeData);

                    String opcParam = linkerBean.findOpcParam(selectedLinkedObjectId, treeNodeData.getItemId());

                    if (opcParam == null) {
                        return;
                    }

                    TreeNode<TreeData> node = findNode(rootOpcTree, opcParam);

                    if (node != null) {
                        String rowKey = node.getRowKey();

                        doCollapseAll(rootOpcTree);

                        while (node != null) {
                            if (!node.isLeaf()) {
                                node.setExpanded(true);
                            }
                            node = node.getParent();
                        }

                        setSelectedParamOpcTreeNode(null);
                        setFilterParamOpcTreeValue("");
                        PrimeFaces.current().ajax().update("linkerForm:linkerTabView:objectTabView:filterParamOpcTree",
                                "linkerForm:linkerTabView:objectTabView:paramOpcTree");
                        PrimeFaces.current().executeScript("document.getElementById('linkerForm:linkerTabView:objectTabView:paramOpcTree_filter').value = 'init'; " +
                                "initDragDropForTree('paramOpcTreeWidget');");

                        PrimeFaces.Ajax ajax = PrimeFaces.current().ajax();
                        ajax.addCallbackParam("command", "updateAfterSelectWrapper");
                        ajax.addCallbackParam("widgetName", "paramOpcTreeWidget");
                        ajax.addCallbackParam("rowKey", rowKey);
                    } else {
                        // Бывает хитрый момент, когда связи нет в таблицы dz_par_dev_link, то надо писать сообщение
                        PrimeFaces.current().ajax().update("growl");

                        if (inIframe) {
                            new TeconMessage(TeconMessage.SEVERITY_WARN, "Линковка", "Связь не найдена").send();
                        } else {
                            FacesContext.getCurrentInstance()
                                    .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Линковка", "Связь не найдена"));
                        }
                    }
                }
                break;
        }
    }

    /**
     * Метод продолжение выделения элемента, что бы было в другом запросе и выполнилось уже после фильтрации
     */
    public void updateParamTreesWrapper() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        PrimeFaces.Ajax ajax = PrimeFaces.current().ajax();
        ajax.addCallbackParam("command", "updateAfterSelect");
        ajax.addCallbackParam("widgetName", params.get("widgetName"));
        ajax.addCallbackParam("rowKey", params.get("rowKey"));

        // Устанавливаем выделенные node
        String rowKey = params.get("rowKey");
        String[] split = rowKey.split("_");
        TreeNode<TreeData> node;
        switch (params.get("widgetName")) {
            case "paramOpcTreeWidget":
                node = rootOpcTree;
                for (String s: split) {
                    node = node.getChildren().get(Integer.parseInt(s));
                }
                setSelectedParamOpcTreeNode(node);
                break;
            case "paramTreeWidget":
                node = rootKmOmTree;
                for (String s: split) {
                    node = node.getChildren().get(Integer.parseInt(s));
                }
                setSelectedParamTreeNode(node);
                break;
        }
    }

    /**
     * Поиск элемента в дереве по шаблону
     *
     * @param node  элемент для начала поиска
     * @param regex шаблон
     * @return найденный элемент
     */
    private TreeNode<TreeData> findNode(TreeNode<TreeData> node, String regex) {
        for (TreeNode<TreeData> child: node.getChildren()) {

            if (child.isLeaf()) {
                if (child.getData().getItemId().matches(regex)) {
                    return child;
                }
            } else {
                TreeNode<TreeData> result = findNode(child, regex);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Обработчик фильтрации дерева OM/KM по типу в форме "Линкованные объекты / Параметры"
     *
     * @param event событие изменение selectOneMenu
     */
    public void onParamTreeModelChange(ValueChangeEvent event) {
        if (selectedLinkedObjectId != null) {
            switch (Integer.parseInt(event.getNewValue().toString())) {
                case 1:
                    filterParamTreeValue = "";
                    rootKmOmTree.getChildren().clear();
                    prepareTree(linkerBean.getTreeData(selectedLinkedObjectId, TreeType.KM), rootKmOmTree);
                    paramTreePanelHeader = "Конкретная модель";
                    break;
                case 2:
                    filterParamTreeValue = "";
                    rootKmOmTree.getChildren().clear();
                    prepareTree(linkerBean.getTreeData(selectedLinkedObjectId, TreeType.OM), rootKmOmTree);
                    paramTreePanelHeader = "Обобщенная модель";
                    break;
            }
        }
    }

    public void onSubsFilter(ValueChangeEvent event) {
        linkedTable.getFilterByAsMap().values().stream()
                .filter(meta -> meta.getField().equals("subscribed"))
                .findFirst().ifPresent(filterMeta -> filterMeta.setFilterValue(event.getNewValue()));
    }

    /**
     * Обработчик фильтрации дерева OPC по типу в форме "Линкованные объекты / Параметры"
     *
     * @param event событие изменение selectOneMenu
     */
    public void onParamOpcTreeModelChange(ValueChangeEvent event) {
        if (selectedLinkedObjectId != null) {
            switch (Integer.parseInt(event.getNewValue().toString())) {
                case 1:
                    filterParamOpcTreeValue = "";
                    rootOpcTree.getChildren().clear();
                    prepareTree(linkerBean.getTreeData(selectedLinkedObjectId, TreeType.OPC, (short) 2), rootOpcTree, null);
                    paramOpcTreePanelHeader = "Все параметры";
                    break;
                case 2:
                    filterParamOpcTreeValue = "";
                    rootOpcTree.getChildren().clear();
                    prepareTree(linkerBean.getTreeData(selectedLinkedObjectId, TreeType.OPC, (short) 1), rootOpcTree, null);
                    paramOpcTreePanelHeader = "Линкованные параметры";
                    break;
                case 3:
                    filterParamOpcTreeValue = "";
                    rootOpcTree.getChildren().clear();
                    prepareTree(linkerBean.getTreeData(selectedLinkedObjectId, TreeType.OPC, (short) 0), rootOpcTree, null);
                    paramOpcTreePanelHeader = "Нелинкованные параметры";
                    break;
            }
        }
    }

    /**
     * Обработчик нажатия кнопки "Обновить" в форме "Линкованные объекты / Параметры"
     */
    public void refreshParamOpcTree() {
        rootOpcTree.getChildren().forEach(node -> linkerBean.readOpcParams(node.getData().getName()));

        filterParamOpcTreeValue = "";
        rootOpcTree.getChildren().clear();
        prepareTree(linkerBean.getTreeData(selectedLinkedObjectId, TreeType.OPC, (short) 2), rootOpcTree, null);
        paramOpcTreePanelHeader = "Все параметры";
    }

    /**
     * Обработчки нажатия кнопки "Запросить параметры" в форме "Линкованные объекты / Параметры"
     */
    public void requestOpcParam() {
        boolean update = false;
        List<String> errorList = new ArrayList<>();
        List<Future<Void>> requests = new ArrayList<>();

        for (TreeNode<TreeData> node: rootOpcTree.getChildren()) {
            try {
                requests.add(linkerBean.requestOpcParams(node.getData().getName()));
                update = true;
            } catch (SystemParamException e) {
                errorList.add(e.getMessage());
            }
        }

        for (Future<Void> future: requests) {
            try {
                future.get(1, TimeUnit.MINUTES);
            } catch (InterruptedException | TimeoutException e) {
                errorList.add("Внутренняя ошибка сервера");
            } catch (ExecutionException e) {
                if (e.getCause() instanceof SystemParamException) {
                    SystemParamException cause = (SystemParamException) e.getCause();
                    errorList.add(cause.getMessage());
                } else {
                    errorList.add("Внутренняя ошибка сервера");
                }
            }
        }

        if (!errorList.isEmpty()) {
            PrimeFaces.current().ajax().update("growl");

            errorList.forEach(error -> {
                if (inIframe) {
                    new TeconMessage(TeconMessage.SEVERITY_ERROR, "Линковка", error).send();
                } else {
                    FacesContext.getCurrentInstance()
                            .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Линковка", error));
                }
            });
        }

        if (update) {
            filterParamOpcTreeValue = "";
            rootOpcTree.getChildren().clear();
            prepareTree(linkerBean.getTreeData(selectedLinkedObjectId, TreeType.OPC, (short) 2), rootOpcTree, null);
            paramOpcTreePanelHeader = "Все параметры";

            PrimeFaces.current().executeScript("filterTree([{name:'widget', value:'paramOpcTreeWidget'}]); " +
                    "unselectNode('paramTreeWidget', false); unselectNode('paramOpcTreeWidget', false); " +
                    "PF('filterParamOpcTreeSelectOneMenuWidget').selectValue(1);");
            PrimeFaces.current().ajax().update("linkerForm:linkerTabView:objectTabView:paramOpcTreePanelHeader",
                    "linkerForm:linkerTabView:objectTabView:filterParamOpcTree");
        }
    }

    /**
     * Обработчик выбора элемента дерева в закладке "Линкованные объекты / Вычислимые параметры"
     *
     * @param event событие выделения
     */
    public void onCalcTreeNodeSelect(NodeSelectEvent event) {
        List<String> types = Arrays.asList("LSA", "NLBOP", "NLSA");
        if (event.getTreeNode().isLeaf() && types.contains(event.getTreeNode().getType())) {
            logger.log(Level.INFO, "select tree node item {0}", event.getTreeNode());

            try {
                SystemParam systemParam = ((TreeData) event.getTreeNode().getData()).getSystemParam();

                calcDataTableList = linkerBean.getCalcParamTable(selectedLinkedObjectId, systemParam.getId(), systemParam.getAgrId());
            } catch (ParseException e) {
                calcDataTableList.clear();
            }
        } else {
            calcDataTableList.clear();
        }
    }

    /**
     * Метод разрывает связь параметра
     */
    public void removeCalcLink() {
        logger.log(Level.INFO, "remove calc param link for object {0}, param {1}", new Object[]{selectedLinkedObjectId, selectedCalcTreeNode});

        try {
            SystemParam systemParam = selectedCalcTreeNode.getData().getSystemParam();

            linkerBean.unlinkCalcParam(selectedLinkedObjectId, systemParam.getId(), systemParam.getAgrId());

            selectedCalcTreeNode.setType("NLSA");
        } catch (ParseException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Линковка", "Внутренняя ошибка сервера").send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Линковка", "Внутренняя ошибка сервера"));
            }
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Линковка", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Линковка", e.getMessage()));
            }
        }
    }

    /**
     * Метод создает связь параметра
     */
    public void createCalcLink() {
        logger.log(Level.INFO, "create calc param link for object {0}, param {1}", new Object[]{selectedLinkedObjectId, selectedCalcTreeNode});

        try {
            SystemParam systemParam = selectedCalcTreeNode.getData().getSystemParam();

            linkerBean.linkCalcParam(selectedLinkedObjectId, systemParam.getId(), systemParam.getAgrId());

            selectedCalcTreeNode.setType("LSA");
        } catch (ParseException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Линковка", "Внутренняя ошибка сервера").send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Линковка", "Внутренняя ошибка сервера"));
            }
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Линковка", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Линковка", e.getMessage()));
            }
        }
    }

    /**
     * Обработчик изменения типа объекта на закладке "Линкованные объект / Объекты"
     */
    public void onObjectTypeChange() {
        logger.log(Level.INFO, "change object type filter {0}", selectedObjectType);

        linkedData.setData(linkerBean.getLinkedData(selectedObjectType.getId(), utilMB.getLogin()));
        selectedLinkedData = null;
    }

    /**
     * @param event событие выбора строки в таблице "Линкованные объекты / Объекты"
     */
    public void onLinkedRowSelect(SelectEvent<LinkedData> event) {
        logger.log(Level.INFO, "Select linked data {0}", event.getObject());

        linkerBean.loadRecountData(selectedLinkedData);
    }

    public void onContextMenu(SelectEvent<LinkedData> event) {
        logger.log(Level.INFO, "Show context menu for linked data {0}", event.getObject());

        onLinkedRowSelect(event);

        if (isRenderContextSubMenu()) {
            templateLinkNames.clear();
            templateLinkNames.addAll(linkerBean.getTemplateLinkNames(selectedLinkedData));
        }
    }

    /**
     * Линковка параметров у выделенного объекта автоматизации в "Линкованные объекты / Объекты"
     * @param templateName имя шаблона
     */
    public void addLinkTemplate(String templateName) {
        logger.log(Level.INFO, "Add template {0} for {1}", new Object[]{templateName, selectedLinkedData});

        try {
            linkerBean.addLinkTemplate(templateName, selectedLinkedData);
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_SUCCESS, "Шаблон линковки", "Успешная линковка параметров " + templateName).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Шаблон линковки", "Успешная линковка параметров " + templateName));
            }
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Шаблон линковки", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Шаблон линковки", e.getMessage()));
            }
        }
    }

    /**
     * Запуск пересчета дерева организационной структуры
     */
    public void updateOrgTree() {
        logger.log(Level.INFO, "Update org tree");

        try {
            linkerBean.updateOrgTree();

            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_SUCCESS, "Обновление", "Успешное обновление дерева организационной структуры").send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Обновление", "Успешное обновление дерева организационной структуры"));
            }
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Обновление", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Обновление", e.getMessage()));
            }
        }
    }

    /**
     * Обработчик закрытия окна создания маски для схемы линковки
     */
    public void onCreateMaskDialogClose() {
        newMaskName = null;
    }

    /**
     * Обертка для создания маски для схемы линковки
     */
    public void createMaskWrapper() {
        PrimeFaces.current().executeScript("createMaskWrapper();");
    }

    /**
     * Создание маски для схемы линковки
     */
    public void createMask() {
        logger.log(Level.INFO, "Create mask {0} for {1}", new Object[]{newMaskName, selectedLinkedData});

        String oldValue = selectedLinkedData.getSchema().getName();
        selectedLinkedData.getSchema().setName(newMaskName);

        try {
            linkerBean.changeSchemaName(selectedLinkedData);

            // Коллекция всех объектов
            List<LinkedData> allData = ((LazyLinkedDataModel<LinkedData>) linkedData).getData();

            // Коллекция объектов у которых такой же "объект системы" как и в исправляемом
            List<LinkedData> sameDbObjectsList = allData.stream()
                    .filter(entry -> entry.getDbObject().getId() == selectedLinkedData.getDbObject().getId())
                    .collect(Collectors.toList());

            // Коллекция элементов которые надо обновить
            Set<String> updateList = sameDbObjectsList.stream()
                    .map(entry -> {
                        int index = linkedData.getWrappedData().indexOf(entry);
                        if (index != -1) {
                            return "linkerForm:linkerTabView:objectTabView:linkedDataTable:" + index + ":schemaName";
                        } else {
                            return "";
                        }
                    })
                    .collect(Collectors.toSet());

            // Замена значений
            allData.forEach(entry -> {
                if (sameDbObjectsList.contains(entry)) {
                    entry.getSchema().setName(newMaskName);
                }
            });

            PrimeFaces.current().ajax().update(updateList);

            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_SUCCESS, "Схема", "Схема линковки с именем \"" + newMaskName + "\" создана").send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Схема", "Схема линковки с именем \"" + newMaskName + "\" создана"));
            }
        } catch (SystemParamException e) {
            selectedLinkedData.getSchema().setName(oldValue);

            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Схема", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Схема", e.getMessage()));
            }
        }
    }

    /**
     * Обработчик выбора пересчета
     *
     * @param type тип пересчета
     */
    public void recount(RecountTypes type) {
        try {
            selectedLinkedData.getRecount().put(type.name(), !selectedLinkedData.getRecount().get(type.name()));
            String message = linkerBean.recount(selectedLinkedData, type);

            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_SUCCESS, "Пересчет", message).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Пересчет", message));
            }
        } catch (SystemParamException e) {
            selectedLinkedData.getRecount().put(type.name(), !selectedLinkedData.getRecount().get(type.name()));

            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Пересчет", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Пересчет", e.getMessage()));
            }
        }
    }

    public String getRecountStatus(RecountTypes type) {
        if (selectedLinkedData != null) {
            return selectedLinkedData.getRecount().get(type.name()) ? "Вкл" : "Выкл";
        } else {
            return "НД";
        }
    }

    /**
     * Загрузка объектов для долинковки
     */
    public void loadOpcObjectsForLink() {
        opcObjectsForLinkData.setData(linkerBean.getOpcObjectsForLink());
    }

    /**
     * Обработчик закрытия окна долинковки
     */
    public void onAddLinkDialogClose() {
        opcObjectsForLinkData.getData().clear();
        selectedOpcObjectsForLink.clear();
    }

    /**
     * Долинковка выбранных объектов
     */
    public void addLink() {
        logger.log(Level.INFO, "add for {0} links {1}", new Object[]{selectedLinkedData, selectedOpcObjectsForLink});

        for (OpcObjectForLinkData entry: selectedOpcObjectsForLink) {
            try {
                linkerBean.addLink(selectedLinkedData, entry, utilMB.getLogin(), utilMB.getIp());
            } catch (SystemParamException e) {
                if (inIframe) {
                    new TeconMessage(TeconMessage.SEVERITY_ERROR, "Ошибка линковки", e.getMessage()).send();
                } else {
                    FacesContext.getCurrentInstance()
                            .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ошибка линковки", e.getMessage()));
                }
            }
        }

        linkedData.setData(linkerBean.getLinkedData(selectedObjectType.getId(), utilMB.getLogin()));
        selectedLinkedData = null;
    }

    /**
     * Обработчик удаления линковки. Кнопка "Разлинковать"
     */
    public void removeLink() {
        logger.log(Level.INFO, "remove all link for {0}", selectedLinkedData);

        try {
            linkerBean.removeLink(selectedLinkedData, utilMB.getLogin(), utilMB.getIp());
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Ошибка разлинковки", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ошибка разлинковки", e.getMessage()));
            }
        }

        linkedData.setData(linkerBean.getLinkedData(selectedObjectType.getId(), utilMB.getLogin()));
        selectedLinkedData = null;
    }

    /**
     * Обработчик удаления всех линковок. Кнопка "Разлинковать все"
     */
    public void removeAllLinks() {
        logger.log(Level.INFO, "remove all links for {0}", selectedLinkedData);

        try {
            linkerBean.removeAllLinks(selectedLinkedData, utilMB.getLogin(), utilMB.getIp());
        } catch (SystemParamException e) {
            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Ошибка разлинковки", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Ошибка разлинковки", e.getMessage()));
            }
        }

        linkedData.setData(linkerBean.getLinkedData(selectedObjectType.getId(), utilMB.getLogin()));
        selectedLinkedData = null;
    }

    /**
     * Обработчик checkBox в таблице "Линкованные объекты / Объекты" отвечающий за статус подписи.
     *
     * @param data данные для изменения подписи
     */
    public void subscribe(LinkedData data) {
        logger.log(Level.INFO, "Change subscribe {0}", data);

        try {
            linkerBean.subscribe(data, utilMB.getLogin());
        } catch (SystemParamException e) {
            data.setSubscribed(!data.isSubscribed());

            if (inIframe) {
                new TeconMessage(TeconMessage.SEVERITY_ERROR, "Подпись", e.getMessage()).send();
            } else {
                FacesContext.getCurrentInstance()
                        .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Подпись", e.getMessage()));
            }
        }
    }

    public void openReport() {
        openReport(selectedLinkedObjectId);
    }

    public void openReport(int id) {
        if (inIframe) {
            String url = linkerBean.getRedirectUrl("linkerReport").replace("[objectId]", String.valueOf(id));
            PrimeFaces.current().executeScript("window.parent.postMessage({fileUrl: '" + url + "'}, '*');");
        } else {
            ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
            String contextPath = externalContext.getRequestContextPath();
            PrimeFaces.current().executeScript("window.open('" + contextPath + "/linker/report?objectId=" + id + "', '_blank').focus();");
        }
    }

    public String getFullObjectName() {
        if (selectedLinkedObjectId != null) {
            return " (" + linkerBean.getFullObjectName(selectedLinkedObjectId) + ")";
        } else {
            return "";
        }
    }

    public LazyLinkedDataModel<LinkedData> getLinkedData() {
        return linkedData;
    }

    public ObjectType getSelectedObjectType() {
        return selectedObjectType;
    }

    public void setSelectedObjectType(ObjectType selectedObjectType) {
        this.selectedObjectType = selectedObjectType;
    }

    public LinkedData getSelectedLinkedData() {
        return selectedLinkedData;
    }

    public void setSelectedLinkedData(LinkedData selectedLinkedData) {
        this.selectedLinkedData = selectedLinkedData;
    }

    public boolean isDisableLinksBtn() {
        return selectedLinkedData == null;
    }

    public boolean isEnableCalcLinksBtn() {
        if ((selectedCalcTreeNode != null) && !calcDataTableList.isEmpty()) {
            for (CalcDataTable entry: calcDataTableList) {
                if (entry.getColor() == 0) {
                    return false;
                }
            }

            return selectedCalcTreeNode.getType().equals("NLSA");
        } else {
            return false;
        }
    }

    public boolean isEnableCalcUnlinksBtn() {
        if (selectedCalcTreeNode != null) {
            return selectedCalcTreeNode.getType().equals("LSA");
        } else {
            return false;
        }
    }

    public String getNewMaskName() {
        return newMaskName;
    }

    public void setNewMaskName(String newMaskName) {
        this.newMaskName = newMaskName;
    }

    public LazyDataModel<OpcObjectForLinkData> getOpcObjectsForLinkData() {
        return opcObjectsForLinkData;
    }

    public List<OpcObjectForLinkData> getSelectedOpcObjectsForLink() {
        return selectedOpcObjectsForLink;
    }

    public void setSelectedOpcObjectsForLink(List<OpcObjectForLinkData> selectedOpcObjectsForLink) {
        this.selectedOpcObjectsForLink = selectedOpcObjectsForLink;
    }

    public TreeNode<TreeData> getRootCalcTree() {
        return rootCalcTree;
    }

    public TreeNode<TreeData> getSelectedCalcTreeNode() {
        return selectedCalcTreeNode;
    }

    public void setSelectedCalcTreeNode(TreeNode<TreeData> selectedCalcTreeNode) {
        this.selectedCalcTreeNode = selectedCalcTreeNode;
    }

    public String getFilterCalcTreeValue() {
        return filterCalcTreeValue;
    }

    public void setFilterCalcTreeValue(String filterCalcTreeValue) {
        this.filterCalcTreeValue = filterCalcTreeValue;
    }

    public List<CalcDataTable> getCalcDataTableList() {
        return calcDataTableList;
    }

    public TreeNode<TreeData> getRootKmOmTree() {
        return rootKmOmTree;
    }

    public String getFilterParamTreeValue() {
        return filterParamTreeValue;
    }

    public void setFilterParamTreeValue(String filterParamTreeValue) {
        this.filterParamTreeValue = filterParamTreeValue;
    }

    public TreeNode<TreeData> getSelectedParamTreeNode() {
        return selectedParamTreeNode;
    }

    public void setSelectedParamTreeNode(TreeNode<TreeData> selectedParamTreeNode) {
        this.selectedParamTreeNode = selectedParamTreeNode;
    }

    public String getFilterParamOpcTreeValue() {
        return filterParamOpcTreeValue;
    }

    public void setFilterParamOpcTreeValue(String filterParamOpcTreeValue) {
        this.filterParamOpcTreeValue = filterParamOpcTreeValue;
    }

    public TreeNode<TreeData> getSelectedParamOpcTreeNode() {
        return selectedParamOpcTreeNode;
    }

    public void setSelectedParamOpcTreeNode(TreeNode<TreeData> selectedParamOpcTreeNode) {
        this.selectedParamOpcTreeNode = selectedParamOpcTreeNode;
    }

    public TreeNode<TreeData> getRootOpcTree() {
        return rootOpcTree;
    }

    public String getParamTreePanelHeader() {
        return paramTreePanelHeader;
    }

    public String getParamOpcTreePanelHeader() {
        return paramOpcTreePanelHeader;
    }

    public NavigationBeanLocal getNavigationBean() {
        return navigationBean;
    }

    public LazyLinkedDataModel<OpcObjectForLinkData> getOpcObjectsForNoLinkData() {
        return opcObjectsForNoLinkData;
    }

    public OpcObjectForLinkData getSelectedOpcObjectsForNoLink() {
        return selectedOpcObjectsForNoLink;
    }

    public void setSelectedOpcObjectsForNoLink(OpcObjectForLinkData selectedOpcObjectsForNoLink) {
        this.selectedOpcObjectsForNoLink = selectedOpcObjectsForNoLink;
    }

    public LazyLoadingTreeNode<TreeNodeModel> getSelectedNavigateObject() {
        return selectedNavigateObject;
    }

    public void setSelectedNavigateObject(LazyLoadingTreeNode<TreeNodeModel> selectedNavigateObject) {
        this.selectedNavigateObject = selectedNavigateObject;
    }

    public boolean isDisabledParamButtonsForNoLink() {
        return selectedOpcObjectsForNoLink == null;
    }

    public boolean isDisabledLinkBtnForNoLink() {
        return objectsForLink.isEmpty() || (selectedNavigateObject == null) || !selectedNavigateObject.getData().isLeaf();
    }

    public boolean isRenderNavigateCustomBtn() {
        return selectedObjectType.getCode().equals("УУ");
    }

    public boolean isRenderContextSubMenu() {
        return selectedObjectType.getCode().equals("УУ");
    }

    public boolean isDisabledNavigateCustomBtn() {
        return (selectedNavigateObject == null) || !selectedNavigateObject.getData().isLeaf();
    }

    public Set<OpcObjectForLinkData> getObjectsForLink() {
        return objectsForLink;
    }

    public LazyLinkedDataModel<OpcObjectForLinkData> getOpcObjectParams() {
        return opcObjectParams;
    }

    public LazyLinkedDataModel<LinkSchemaData> getLinkSchemaTable() {
        return linkSchemaTable;
    }

    public LinkSchemaData getSelectedLinkSchema() {
        return selectedLinkSchema;
    }

    public void setSelectedLinkSchema(LinkSchemaData selectedLinkSchema) {
        this.selectedLinkSchema = selectedLinkSchema;
    }

    public List<Redirect> getRedirectList() {
        return redirectList;
    }

    public List<?> getReportList() {
        return Collections.singletonList("");
    }

    public DataTable getLinkedTable() {
        return linkedTable;
    }

    public void setLinkedTable(DataTable linkedTable) {
        this.linkedTable = linkedTable;
    }

    public List<String> getTemplateLinkNames() {
        return templateLinkNames;
    }

    public List<String> getStartList() {
        return Collections.singletonList("Обновить дерево Орг. структуры");
    }

    public Tree getParamTree() {
        return paramTree;
    }

    public void setParamTree(Tree paramTree) {
        componentMap.put(paramTree.getWidgetVar(), paramTree);
        this.paramTree = paramTree;
    }

    public InputText getParamTreeFilter() {
        return paramTreeFilter;
    }

    public void setParamTreeFilter(InputText paramTreeFilter) {
        componentMap.put(paramTreeFilter.getWidgetVar(), paramTreeFilter);
        this.paramTreeFilter = paramTreeFilter;
    }

    public Tree getCalcTree() {
        return calcTree;
    }

    public void setCalcTree(Tree calcTree) {
        componentMap.put(calcTree.getWidgetVar(), calcTree);
        this.calcTree = calcTree;
    }

    public InputText getCalcTreeFilter() {
        return calcTreeFilter;
    }

    public void setCalcTreeFilter(InputText calcTreeFilter) {
        componentMap.put(calcTreeFilter.getWidgetVar(), calcTreeFilter);
        this.calcTreeFilter = calcTreeFilter;
    }

    public Tree getParamOpcTree() {
        return paramOpcTree;
    }

    public void setParamOpcTree(Tree paramOpcTree) {
        componentMap.put(paramOpcTree.getWidgetVar(), paramOpcTree);
        this.paramOpcTree = paramOpcTree;
    }

    public InputText getParamOpcTreeFilter() {
        return paramOpcTreeFilter;
    }

    public void setParamOpcTreeFilter(InputText paramOpcTreeFilter) {
        componentMap.put(paramOpcTreeFilter.getWidgetVar(), paramOpcTreeFilter);
        this.paramOpcTreeFilter = paramOpcTreeFilter;
    }

    public void changeInIframe() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        inIframe = Boolean.parseBoolean(params.get("inIframe"));
    }
}
