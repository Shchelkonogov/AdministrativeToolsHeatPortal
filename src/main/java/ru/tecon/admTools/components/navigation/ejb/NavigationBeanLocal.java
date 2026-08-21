package ru.tecon.admTools.components.navigation.ejb;

import jakarta.ejb.Local;
import ru.tecon.admTools.components.navigation.model.ObjTypePropertyModel;
import ru.tecon.admTools.components.navigation.model.SpObject;
import ru.tecon.admTools.components.navigation.model.TreeNodeModel;

import java.util.List;

/**
 * @author Maksim Shchelkonogov
 * 12.09.2023
 */
@Local
public interface NavigationBeanLocal {

    /**
     * Получение свойств поиска для типа объекта
     *
     * @param objTypeId тип объекта
     * @return список свойств поиска
     */
    List<ObjTypePropertyModel> getObjTypeProps(long objTypeId);

    /**
     * Получение начального элемента для дерева организационной структуры.
     * Поскольку в ленивой загрузки используется id родителя,
     * а в каждой функции загрузки может быть разный родительский элемент.
     * Например: new TreeNodeModel("S", "", "", "", "S");
     *
     * @return начальный элемент дерева
     */
    TreeNodeModel getRootOrgTree();

    /**
     * Загрузка части дерева организационной структуры.
     *
     * @param objectTypeId тип объекта
     * @param searchTypeId тип свойства поиска
     * @param searchText текст поиска
     * @param userName имя пользователя
     * @param parentNode id родительского элемента
     * @return список элементов для добавления к родителю
     */
    List<TreeNodeModel> getOrgTree(int objectTypeId, long searchTypeId, String searchText,
                                          String userName, String parentNode);

    /**
     * Получение начального элемента для дерева территориальной структуры.
     * Поскольку в ленивой загрузки используется id родителя,
     * а в каждой функции загрузки может быть разный родительский элемент.
     * Например: new TreeNodeModel("S", "", "", "", "S");
     *
     * @return начальный элемент дерева
     */
    TreeNodeModel getRootTerTree();

    /**
     * Загрузка части дерева территориальной структуры.
     *
     * @param objectTypeId тип объекта
     * @param searchTypeId тип свойства поиска
     * @param searchText текст поиска
     * @param userName имя пользователя
     * @param parentNode id родительского элемента
     * @return список элементов для добавления к родителю
     */
    List<TreeNodeModel> getTerTree(int objectTypeId, long searchTypeId, String searchText,
                                   String userName, String parentNode);

    /**
     * Получение начального элемента для дерева структуры связи.
     * Поскольку в ленивой загрузки используется id родителя,
     * а в каждой функции загрузки может быть разный родительский элемент.
     * Например: new TreeNodeModel("S", "", "", "", "S");
     *
     * @return начальный элемент дерева
     */
    TreeNodeModel getRootLinkTree();

    /**
     * Загрузка части дерева структуры связи.
     *
     * @param objectTypeId тип объекта
     * @param searchTypeId тип свойства поиска
     * @param searchText текст поиска
     * @param userName имя пользователя
     * @param parentNode id родительского элемента
     * @return список элементов для добавления к родителю
     */
    List<TreeNodeModel> getLinkTree(int objectTypeId, long searchTypeId, String searchText,
                                   String userName, String parentNode);

    /**
     * Получение значений справочника
     *
     * @param spId идентификатор справочника
     * @param filterValue значение фильтрации
     * @return список значений справочника
     */
    List<SpObject> getSpData(int spId, String filterValue);
}
