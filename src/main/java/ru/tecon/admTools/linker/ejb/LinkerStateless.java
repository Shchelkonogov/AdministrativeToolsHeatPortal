package ru.tecon.admTools.linker.ejb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import ru.tecon.admTools.linker.model.*;
import ru.tecon.admTools.systemParams.SystemParamException;
import ru.tecon.admTools.utils.AdmTools;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless bean для получения данных для формы "Линковщик"
 *
 * @author Maksim Shchelkonogov
 * 10.07.2023
 */
@Stateless
@LocalBean
public class LinkerStateless {

    private static final int httpTimeout = 1;

    private static final String scheme = "http";

    @Resource(name = "licenseIp")
    private String host;

    private static final int port = 1337;
    private static final List<String> path = List.of("api", "Linker", "CheckObjectLicense");

    private static final String SEL_REDIRECT = "select * from m_adm.get_td_application_url(?)";
    private final static String SELECT_LINKED_OBJECTS_DATA = "select * from lnk_0001t.sel_linked_object(?, '', '', cast(0 as smallint), ?);";
    private final static String SELECT_RECOUNT_DATA = "select * from lnk_0001t.sel_obj_recalc_mode(?, ?, ?);";
    private final static String PROCEDURE_RECOUNT_PRESSURE = "call lnk_0001t.recalc_pressure(?, ?, ?, ?);";
    private final static String PROCEDURE_RECOUNT_PRESSURE_TC = "call lnk_0001t.recalc_pressure_p1p2(?, ?, ?, ?);";
    private final static String PROCEDURE_RECOUNT_TIME = "call lnk_0001t.recalc_time(?, ?, ?, ?);";
    private final static String PROCEDURE_RECOUNT_RESISTANCE = "call lnk_0001t.recalc_resistance(?, ?, ?, ?);";
    private final static String PROCEDURE_SUBSCRIBE = "call lnk_0001t.set_subscribe(?, ?, ?, ?);";
    private final static String PROCEDURE_CHANGE_SCHEMA_NAME = "call lnk_0001t.create_schema(?, ?);";
    private final static String PROCEDURE_REMOVE_LINK = "call lnk_0001t.unlink_opc_object(?, ?, ?, ?)";
    private final static String PROCEDURE_REMOVE_ALL_LINKS = "call lnk_0001t.unlink_aspid_object(?, ?, ?)";
    private final static String SELECT_OPC_OBJECTS_FOR_LINK = "select * from lnk_0001t.sel_unlinked_opc_object('')";
    private final static String PROCEDURE_ADD_LINK = "call lnk_0001t.link_add_opc_object(?, ?, ?, ?, ?)";
    private final static String SELECT_FULL_NAME = "select admin.get_obj_path_all(?,', ') || ' (' || admin.get_obj_address(?) || ')'";
    private final static String SELECT_CALC_TREE = "select * from lnk_0001t.sel_param_tree_calc(?);";
    private final static String SELECT_CALC_PARAM = "select par_memo, par_name, stat_agr_name, color from lnk_0001t.sel_calc_param(?, ?, ?);";
    private final static String PROCEDURE_UNLINK_CALC_PARAM = "call lnk_0001t.unlink_calc_param(?, ?, ?);";
    private final static String PROCEDURE_LINK_CALC_PARAM = "call lnk_0001t.link_calc_param(?, ?, ?);";
    private final static String SELECT_OM_TREE = "select * from lnk_0001t.sel_param_tree_om(?);";
    private final static String SELECT_KM_TREE = "select * from lnk_0001t.sel_param_tree_km(?);";
    private final static String SELECT_OPC_TREE = "select * from lnk_0001t.sel_param_tree_opc(?, ?);";
    private final static String PROCEDURE_FIND_OPC_PARAM = "select lnk_0001t.find_opc_param(?, ?);";
    private final static String PROCEDURE_FIND_OM_KM_PARAM = "select lnk_0001t.find_om_param(?, ?);";
    private final static String PROCEDURE_LINK_PARAM = "call lnk_0001t.link_param(?, ?, ?, ?);";
    private final static String PROCEDURE_UNLINK_PARAM = "call lnk_0001t.unlink_param(?, ?, ?, ?);";
    private final static String PROCEDURE_READ_OPC_PARAM = "call lnk_0001t.read_tsa_param(?)";
    private final static String PROCEDURE_REQUEST_OPC_PARAM = "call lnk_0001t.get_opc_param_list(?, ?)";
    private final static String SELECT_REDIRECT = "select * from admin.opc_servers order by name;";
    private final static String PROCEDURE_REMOVE_OPC_OBJECT = "call lnk_0001t.del_unlinked_opc_object(?, ?, ?);";
    private final static String PROCEDURE_REMOVE_OPC_OBJECT_PARAMS = "call lnk_0001t.clear_unlinked_opc_object(?, ?, ?);";
    private final static String PROCEDURE_CREATE_FICTITIOUS_YY = "call lnk_0001t.add_unlinked_fict_uu(?);";
    private final static String SELECT_OPC_OBJECT_PARAMS = "select * from lnk_0001t.sel_unlinked_opc_param_list(?, ?)";
    private final static String SELECT_SCHEMA_LIST = "select * from lnk_0001t.get_schema_list2(?, ?)";
    private final static String PROCEDURE_LINK_BY_SCHEMA_NO = "call lnk_0001t.link_obj_no(?, ?)";
    private final static String PROCEDURE_LINK_BY_SCHEMA_YES = "call lnk_0001t.link_obj_yes(?, ?, ?, ?, ?)";
    private final static String PROCEDURE_LINK_BY_SCHEMA_NO_PARAM = "call lnk_0001t.link_obj_yes_without_params(?, ?, ?, ?)";
    private final static String SELECT_UU_ZONE_LIST = "select zone from lnk_0001t.sel_add_uu_zone_list(?, ?, ?)";
    private final static String PROCEDURE_ADD_LINK_BY_TEMPLATE = "call lnk_0001t.add_uu_zone(?, ?, ?, ?)";
    private final static String PROCEDURE_UPDATE_ORG_TREE = "call lnk_0001t.refresh_vtp_struct_tree()";

    private final static String CHEAT_1 = "call lnk_0001t.dev_link(?, ?, ?)";

    @Inject
    private Logger logger;

    @Inject
    private ObjectMapper objectMapper;

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    /**
     * Получение url для перехода
     *
     * @param name имя свойства для перехода
     * @return url
     */
    public String getRedirectUrl(String name) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_REDIRECT)) {
            stm.setString(1, name);

            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return res.getString(1);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error load catalog types", e);
        }
        return "";
    }

    /**
     * Загрузка линкованных данных, закладка "Линкованные объекты / Объекты"
     *
     * @param objectType тип объекта
     * @param user       пользователь
     * @return коллекция данных
     */
    public List<LinkedData> getLinkedData(int objectType, String user) {
        List<LinkedData> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_LINKED_OBJECTS_DATA)) {
            stm.setInt(1, objectType);
            stm.setString(2, user);

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new LinkedData(
                        new SystemObject(res.getInt("obj_id"), res.getString("obj_name")),
                        new Schema(res.getInt("schema_id"), res.getString("schema_name")),
                        new SystemObject(res.getInt("id"), res.getString("item_name")),
                        res.getString("server_name"),
                        res.getInt("subscribed") == 1,
                        res.getInt("obj_int_key")));
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error load linked object data", ex);
        }
        return result;
    }

    /**
     * Получение информации по пересчету для переданного объекта, информация записывается в этот объект
     *
     * @param linkedData объект для получения информации по перерасчету.
     */
    public void loadRecountData(LinkedData linkedData) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_RECOUNT_DATA)) {
            stm.setInt(1, linkedData.getDbObject().getId());
            stm.setInt(2, linkedData.getObjIntKey());
            stm.setString(3, linkedData.getDbObject().getName());

            ResultSet res = stm.executeQuery();
            if (res.next()) {
                linkedData.getRecount().put(RecountTypes.PRESSURE.name(), res.getInt("pressure_recalc_mode") == 1);
                linkedData.getRecount().put(RecountTypes.PRESSURE_TC.name(), res.getInt("pressure_p1p2_recalc_mode") == 1);
                linkedData.getRecount().put(RecountTypes.RESISTANCE.name(), res.getInt("resistance_recalc_mode") == 1);
                linkedData.getRecount().put(RecountTypes.TIME.name(), res.getInt("time_recalc_mode") == 1);
            }

            logger.log(Level.INFO, "Loaded recount data for object {0}", linkedData);
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error load recount data", ex);
        }
    }

    /**
     * Изменить перерасчет на объекте.
     *
     * @param linkedData объект для изменения перерасчета
     * @param type       тип перерасчета
     * @return в случае удачи, передается успешное сообщение
     * @throws SystemParamException ошибка выполнения обновления
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String recount(LinkedData linkedData, RecountTypes type) throws SystemParamException {
        try (Connection connect = ds.getConnection()) {
            String select;

            switch (type) {
                case TIME:
                    select = PROCEDURE_RECOUNT_TIME;
                    break;
                case PRESSURE:
                    select = PROCEDURE_RECOUNT_PRESSURE;
                    break;
                case PRESSURE_TC:
                    select = PROCEDURE_RECOUNT_PRESSURE_TC;
                    break;
                case RESISTANCE:
                    select = PROCEDURE_RECOUNT_RESISTANCE;
                    break;
                default:
                    throw new SystemParamException("Неизвестный тип пересчета");
            }

            try (CallableStatement cStm = connect.prepareCall(select)) {
                cStm.setInt(1, linkedData.getDbObject().getId());
                cStm.setInt(2, linkedData.getObjIntKey());
                cStm.setString(3, linkedData.getDbObject().getName());
                cStm.registerOutParameter(4, Types.VARCHAR);

                cStm.executeUpdate();

                return cStm.getString(4);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error change recount", e);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(e));
        }
    }

    /**
     * Изменения подписи для объекта
     *
     * @param linkedData объект для изменения подписи
     * @param user       пользователь
     * @throws SystemParamException ошибка выполнения обновления
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void subscribe(LinkedData linkedData, String user) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_SUBSCRIBE)) {

            cStm.setInt(1, linkedData.getDbObject().getId());
            cStm.setInt(2, linkedData.getObjIntKey());
            cStm.setShort(3, (short) (linkedData.isSubscribed() ? 1 : 0));
            cStm.setString(4, user);

            cStm.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error change subscribe", e);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(e));
        }
    }

    /**
     * Изменения схемы линковки для объекта
     *
     * @param linkedData объект для изменения схемы линковки
     * @throws SystemParamException ошибка выполнения обновления
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void changeSchemaName(LinkedData linkedData) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_CHANGE_SCHEMA_NAME)) {

            cStm.setString(1, linkedData.getSchema().getName());
            cStm.setInt(2, linkedData.getDbObject().getId());

            cStm.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error change schema name", e);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(e));
        }
    }

    /**
     * Удаления линквоки
     *
     * @param linkedData объект для разрыва связи
     * @param login      пользователь
     * @param ip         ip с которого обратился пользователь
     * @throws SystemParamException ошибка выполнения удаления
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeLink(LinkedData linkedData, String login, String ip) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_REMOVE_LINK)) {

            cStm.setInt(1, linkedData.getDbObject().getId());
            cStm.setInt(2, linkedData.getObjIntKey());
            cStm.setString(3, login);
            cStm.setString(4, ip);

            cStm.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error remove link", e);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(e));
        }
    }

    /**
     * Удаления всех линковок
     *
     * @param linkedData объект для разрыва всех связей
     * @param login      пользователь
     * @param ip         ip с которого обратился пользователь
     * @throws SystemParamException ошибка выполнения удаления
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeAllLinks(LinkedData linkedData, String login, String ip) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_REMOVE_ALL_LINKS)) {

            cStm.setInt(1, linkedData.getDbObject().getId());
            cStm.setString(2, login);
            cStm.setString(3, ip);

            cStm.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error remove link", e);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(e));
        }
    }

    /**
     * Получение списка объектов для долинковки
     *
     * @return коллекция объектов для долинковки
     */
    public List<OpcObjectForLinkData> getOpcObjectsForLink() {
        List<OpcObjectForLinkData> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_OPC_OBJECTS_FOR_LINK)) {

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new OpcObjectForLinkData(
                        new SystemObject(res.getInt("id"), res.getString("item_name")),
                        res.getInt("param_cnt"),
                        res.getInt("obj_int_key"), res.getString("server_name")));
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error load opc objects for link", ex);
        }
        return result;
    }

    /**
     * Добавления связи для объекта системы
     *
     * @param linkedData  объект системы к которому надо добавить связь
     * @param newLinkData объект автоматизации который надо добавить
     * @param login       пользователь
     * @param ip          ip с которого обратился пользователь
     * @throws SystemParamException ошибка выполнения обновления
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addLink(LinkedData linkedData, OpcObjectForLinkData newLinkData, String login, String ip) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_ADD_LINK)) {

            cStm.setInt(1, linkedData.getDbObject().getId());
            cStm.setInt(2, newLinkData.getOpcObject().getId());
            cStm.setInt(3, newLinkData.getObjIntKey());
            cStm.setString(4, login);
            cStm.setString(5, ip);

            cStm.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error add link", e);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(e));
        }
    }

    /**
     * Получение полного имени объекта
     *
     * @param id id объекта
     * @return имя объекта
     */
    public String getFullObjectName(int id) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_FULL_NAME)) {

            stm.setInt(1, id);
            stm.setInt(2, id);

            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return res.getString(1);
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error load opc objects for link", ex);
        }
        return "";
    }

    /**
     * Получение дынных для деревьев {@link TreeType}
     *
     * @param id              id объекта
     * @param type            тип дерева
     * @param opcTreeLinkType тип параметров для OPC дерева
     * @return список данных для дерева
     */
    public List<TreeData> getTreeData(int id, TreeType type, short opcTreeLinkType) {
        List<TreeData> result = new ArrayList<>();
        String select;
        switch (type) {
            case CALC:
                select = SELECT_CALC_TREE;
                break;
            case KM:
                select = SELECT_KM_TREE;
                break;
            case OM:
                select = SELECT_OM_TREE;
                break;
            case OPC:
                select = SELECT_OPC_TREE;
                break;
            default:
                return result;
        }

        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(select)) {

            stm.setInt(1, id);
            if (type == TreeType.OPC) {
                stm.setShort(2, opcTreeLinkType);
            }

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new TreeData(res.getString("id"), res.getString("name"), res.getString("parent"),
                        res.getString("my_id"), res.getString("my_type"), type));
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error load " + type + " tree data", ex);
        }
        return result;
    }

    /**
     * Получение дынных для деревьев {@link TreeType}
     *
     * @param id   id объекта
     * @param type тип дерева
     * @return список данных для дерева
     */
    public List<TreeData> getTreeData(int id, TreeType type) {
        return getTreeData(id, type, (short) 2);
    }

    /**
     * Поиск слинкованного параметра в OM/KM дереве для формы "Линкованные объекты / Параметры"
     *
     * @param id   id объекта
     * @param myId id выделенного элемента в OPC дереве
     * @return id элемента в OM/KM дереве
     */
    public String findOmKmParam(int id, String myId) {
        return findParam(id, myId, "OmKm");
    }

    /**
     * Поиск слинкованного параметра в OPC дереве для формы "Линкованные объекты / Параметры"
     *
     * @param id   id объекта
     * @param myId id выделенного элемента в OM/KM дереве
     * @return id элемента в OPC дереве
     */
    public String findOpcParam(int id, String myId) {
        return findParam(id, myId, "Opc");
    }

    private String findParam(int id, String myId, String type) {
        try (Connection connect = ds.getConnection()) {

            String select;
            switch (type) {
                case "Opc":
                    select = PROCEDURE_FIND_OPC_PARAM;
                    break;
                case "OmKm":
                    select = PROCEDURE_FIND_OM_KM_PARAM;
                    break;
                default:
                    return "";
            }

            try (PreparedStatement stm = connect.prepareStatement(select)) {
                stm.setInt(1, id);
                stm.setString(2, myId);

                ResultSet res = stm.executeQuery();
                if (res.next()) {
                    return res.getString(1);
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error find param", ex);
        }
        return "";
    }

    /**
     * Получение данных для таблицы в закладке "Линкованные объекты / Вычислимые параметры"
     *
     * @param objId     id объекта
     * @param paramId   id параметра
     * @param statAgrId id статистического агрегата параметра
     * @return данные для таблицы
     */
    public List<CalcDataTable> getCalcParamTable(int objId, int paramId, int statAgrId) {
        List<CalcDataTable> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_CALC_PARAM)) {

            stm.setInt(1, objId);
            stm.setInt(2, paramId);
            stm.setInt(3, statAgrId);

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new CalcDataTable(res.getString("par_memo"), res.getString("stat_agr_name"),
                        res.getString("par_name"), res.getInt("color")));
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error load calc param table", ex);
        }
        return result;
    }

    /**
     * Создает связь для параметра в закладке "Линкованные объекты / Вычислимые параметры"
     *
     * @param objectId  id объекта
     * @param paramId   id параметра
     * @param statAgrId id статистического агрегата
     * @throws SystemParamException ошибка создания связи
     */
    public void linkCalcParam(int objectId, int paramId, int statAgrId) throws SystemParamException {
        changeCalcParamLink(true, objectId, paramId, statAgrId);
    }

    /**
     * Удаляет связь для параметра в закладке "Линкованные объекты / Вычислимые параметры"
     *
     * @param objectId  id объекта
     * @param paramId   id параметра
     * @param statAgrId id статистического агрегата
     * @throws SystemParamException ошибка удаления связи
     */
    public void unlinkCalcParam(int objectId, int paramId, int statAgrId) throws SystemParamException {
        changeCalcParamLink(false, objectId, paramId, statAgrId);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void changeCalcParamLink(boolean link, int objectId, int paramId, int statAgrId) throws SystemParamException {
        try (Connection connect = ds.getConnection()) {

            String select;
            if (link) {
                select = PROCEDURE_LINK_CALC_PARAM;
            } else {
                select = PROCEDURE_UNLINK_CALC_PARAM;
            }

            try (CallableStatement cStm = connect.prepareCall(select)) {
                cStm.setInt(1, objectId);
                cStm.setInt(2, paramId);
                cStm.setInt(3, statAgrId);

                cStm.executeUpdate();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error change param link", e);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(e));
        }
    }

    /**
     * Создает связь для параметра в закладке "Линкованные объекты / Параметры"
     *
     * @param objectId   id объекта
     * @param paramId    id параметра
     * @param opcParamId id opc параметра
     * @return новая id для opc параметра
     * @throws SystemParamException ошибка создания связи
     */
    public String linkParam(int objectId, String paramId, String opcParamId) throws SystemParamException {
        return changeParamLink(true, objectId, paramId, opcParamId);
    }

    /**
     * Удаляет связь для параметра в закладке "Линкованные объекты / Параметры"
     *
     * @param objectId   id объекта
     * @param paramId    id параметра
     * @param opcParamId id opc параметра
     * @return новая id для opc параметра
     * @throws SystemParamException ошибка удаления связи
     */
    public String unlinkParam(int objectId, String paramId, String opcParamId) throws SystemParamException {
        return changeParamLink(false, objectId, paramId, opcParamId);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private String changeParamLink(boolean link, int objectId, String paramId, String opcParamId) throws SystemParamException {
        try (Connection connect = ds.getConnection()) {

            String select;
            if (link) {
                select = PROCEDURE_LINK_PARAM;
            } else {
                select = PROCEDURE_UNLINK_PARAM;
            }

            try (CallableStatement cStm = connect.prepareCall(select)) {
                cStm.setInt(1, objectId);
                cStm.setString(2, paramId);
                cStm.setString(3, opcParamId);
                cStm.registerOutParameter(4, Types.BIGINT);

                cStm.executeUpdate();

                return String.valueOf(cStm.getLong(4));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error change param link", e);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(e));
        }
    }

    /**
     * Метод обновляет параметры для opc дерева в базе
     *
     * @param opcObjectName имя opc объекта
     */
    public void readOpcParams(String opcObjectName) {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_READ_OPC_PARAM)) {
            cStm.setString(1, opcObjectName);

            cStm.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error read opc params", ex);
        }
    }

    /**
     * Метод отправляет запрос opc серверу для запроса параметров
     *
     * @param opcObjectName имя opc объекта
     * @return null
     * @throws SystemParamException если ошибка выполнения запроса
     */
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Future<Void> requestOpcParams(String opcObjectName) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_REQUEST_OPC_PARAM)) {
            cStm.setString(1, opcObjectName);
            cStm.registerOutParameter(2, Types.VARCHAR);

            cStm.executeUpdate();
            if (!cStm.getString(2).isEmpty()) {
                throw new SystemParamException(cStm.getString(2));
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error read opc params", ex);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(ex));
        }
        return null;
    }

    /**
     * Получение map источников данных
     *
     * @return map с данными
     */
    public List<Redirect> getRedirect() {
        List<Redirect> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_REDIRECT)) {
            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new Redirect(res.getString(1), res.getString(2)));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error load redirect", e);
        }
        return result;
    }

    /**
     * Удаление opc объекта
     * @param opcObject opc объект
     * @throws SystemParamException ошибка удаления
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeOpcObject(OpcObjectForLinkData opcObject) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_REMOVE_OPC_OBJECT)) {
            cStm.setInt(1, opcObject.getOpcObject().getId());
            cStm.setInt(2, opcObject.getObjIntKey());
            cStm.setString(3, opcObject.getOpcObject().getName());

            cStm.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error remove opc object", e);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(e));
        }
    }

    /**
     * Удаление параметров opc объекта
     * @param opcObject opc объект
     * @throws SystemParamException ошибка удаления
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeOpcObjectParams(OpcObjectForLinkData opcObject) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_REMOVE_OPC_OBJECT_PARAMS)) {
            cStm.setInt(1, opcObject.getOpcObject().getId());
            cStm.setInt(2, opcObject.getObjIntKey());
            cStm.setString(3, opcObject.getOpcObject().getName());

            cStm.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error remove opc object params", e);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(e));
        }
    }

    /**
     * Создание фиктивного узла учета
     * @param name имя объекта системы, для которого делается фиктивный узел учета
     * @throws SystemParamException ошибка создания узла учета
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createFictitiousYY(String name) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_CREATE_FICTITIOUS_YY)) {
            cStm.setString(1, name);

            cStm.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error create fictitious yy", e);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(e));
        }
    }

    /**
     * Получение списка параметров opc объекта
     * @param opcObject opc объект
     * @return список параметров
     */
    public List<OpcObjectForLinkData> getOpcObjectParams(OpcObjectForLinkData opcObject) {
        List<OpcObjectForLinkData> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_OPC_OBJECT_PARAMS)) {
            stm.setInt(1, opcObject.getOpcObject().getId());
            stm.setInt(2, opcObject.getObjIntKey());

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new OpcObjectForLinkData(
                        new SystemObject(res.getInt("id"), res.getString("item_name")),
                        res.getInt("obj_int_key")));
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error load opc object params", ex);
        }
        return result;
    }

    public boolean checkLicense(String objectId, String sessionId) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(httpTimeout * 60 * 1000)
                .setConnectionRequestTimeout(httpTimeout * 60 * 1000)
                .setSocketTimeout(httpTimeout * 60 * 1000).build();

        URIBuilder uriBuilder = new URIBuilder()
                .setScheme(scheme)
                .setHost(host)
                .setPort(port)
                .setPathSegments(path)
                .addParameter("objectId", objectId);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()) {
            HttpGet httpGet = new HttpGet(uriBuilder.build());

            httpGet.addHeader("Content-Type", "application/json");
            httpGet.addHeader("X-TD-AUTH", sessionId);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    License license = objectMapper.readValue(
                            EntityUtils.toString(response.getEntity()),
                            new TypeReference<License>() {
                            }
                    );

                    logger.log(Level.INFO, "license {0}", license);
                    return license.isLicensed;
                } else {
                    logger.log(Level.WARNING, "Error request license. Error code {0}", response.getStatusLine().getStatusCode());
                }
            }
        } catch (IOException | URISyntaxException e) {
            logger.log(Level.WARNING, "Error request license", e);
        }

        return false;
    }

    /**
     * Получение списка схем линковки
     * @param systemObjectId объект системы
     * @param opcObjectList список opc объектов
     * @return список схем линковок
     * @throws SystemParamException ошибка получения списка
     */
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public List<LinkSchemaData> getSchemaListForLink(int systemObjectId, List<OpcObjectForLinkData> opcObjectList) throws SystemParamException {
        List<LinkSchemaData> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_SCHEMA_LIST)) {
            Object[] objects = opcObjectList.stream()
                    .map(linkData -> linkData.getPgObject("lnk_0001t.t_buf_table_rec"))
                    .toArray();

            stm.setInt(1, systemObjectId);
            stm.setArray(2, connect.createArrayOf("lnk_0001t.t_buf_table_rec", objects));

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new LinkSchemaData(
                        new Schema(res.getInt("sch_id"), res.getString("sch_name")),
                        res.getInt("eq_linked"),
                        res.getInt("neq_linked"),
                        res.getInt("all_linked"),
                        res.getInt("eq_nlinked"),
                        res.getInt("neq_nlinked"),
                        res.getInt("all_nlinked")
                ));
            }
            result.sort(Collections.reverseOrder());
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error load schema list", ex);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(ex));
        }

        return result;
    }

    /**
     * Отмена линковки объектов
     * @param systemObjectId объект системы
     * @param opcObjectList список opc объектов
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void linkBySchemaNo(int systemObjectId, List<OpcObjectForLinkData> opcObjectList) {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_LINK_BY_SCHEMA_NO)) {

            Object[] objects = opcObjectList.stream()
                    .map(linkData -> linkData.getPgObject("lnk_0001t.t_buf_table_rec"))
                    .toArray();

            cStm.setInt(1, systemObjectId);
            cStm.setArray(2, connect.createArrayOf("lnk_0001t.t_buf_table_rec", objects));

            cStm.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error link by schema \"no\"", ex);
        }
    }

    /**
     * линковка объектов
     * @param systemObjectId объект системы
     * @param opcObjectList список opc объектов
     * @param schema схема линковки
     * @param login идентификатор пользователя
     * @param ip ip пользователя
     */
    public void linkBySchemaYes(int systemObjectId, ArrayList<OpcObjectForLinkData> opcObjectList, LinkSchemaData schema, String login, String ip) {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_LINK_BY_SCHEMA_YES)) {

            Object[] objects = opcObjectList.stream()
                    .map(linkData -> linkData.getPgObject("lnk_0001t.t_buf_table_rec"))
                    .toArray();

            cStm.setInt(1, systemObjectId);
            cStm.setArray(2, connect.createArrayOf("lnk_0001t.t_buf_table_rec", objects));
            cStm.setInt(3, schema.getSchema().getId());
            cStm.setString(4, login);
            cStm.setString(5, ip);

            cStm.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error link by schema \"yes\"", ex);
        }
    }

    /**
     * линковка объектов без параметров
     * @param systemObjectId объект системы
     * @param opcObjectList список opc объектов
     * @param login идентификатор пользователя
     * @param ip ip пользователя
     */
    public void linkBySchemaNoParam(int systemObjectId, ArrayList<OpcObjectForLinkData> opcObjectList, String login, String ip) {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_LINK_BY_SCHEMA_NO_PARAM)) {

            Object[] objects = opcObjectList.stream()
                    .map(linkData -> linkData.getPgObject("lnk_0001t.t_buf_table_rec"))
                    .toArray();

            cStm.setInt(1, systemObjectId);
            cStm.setArray(2, connect.createArrayOf("lnk_0001t.t_buf_table_rec", objects));
            cStm.setString(3, login);
            cStm.setString(4, ip);

            cStm.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error link by schema \"yes no param\"", ex);
        }
    }

    /**
     * Получения списка имен шаблонных вариантов линковки параметров
     * @param linkedData линкованный объект автоматизации
     * @return список шаблонных имен
     */
    public List<String> getTemplateLinkNames(LinkedData linkedData) {
        List<String> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_UU_ZONE_LIST)) {
            stm.setInt(1, linkedData.getOpcObject().getId());
            stm.setInt(2, linkedData.getObjIntKey());
            stm.setString(3, linkedData.getOpcObject().getName());

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(res.getString("zone"));
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error load template link names", ex);
        }
        return result;
    }

    /**
     * Линковка параметров по шаблону к линкованному объекту автоматизации
     * @param templateName имя шаблона
     * @param linkedData линкованный объект автоматизации
     * @throws SystemParamException в случае ошибки линковки
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addLinkTemplate(String templateName, LinkedData linkedData) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_ADD_LINK_BY_TEMPLATE)) {
            cStm.setInt(1, linkedData.getOpcObject().getId());
            cStm.setString(2, linkedData.getOpcObject().getName());
            cStm.setInt(3, linkedData.getObjIntKey());
            cStm.setString(4, templateName);

            cStm.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error create link by template", ex);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(ex));
        }
    }

    /**
     * Запрос на обновления дерева организационной структуры
     * @throws SystemParamException в случае ошибки обновления
     */
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void updateOrgTree() throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(PROCEDURE_UPDATE_ORG_TREE)) {

            cStm.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error update org tree", ex);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(ex));
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void cheat1(int id, String serverName, int objType) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(CHEAT_1)) {
            cStm.setInt(1, id);
            cStm.setString(2, serverName);
            cStm.setInt(3, objType);

            cStm.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error execute cheat 1", ex);
            throw new SystemParamException(AdmTools.getSQLExceptionMessage(ex));
        }
    }

    private static class License {

        private boolean isLicensed;

        public License() {
        }

        public void setIsLicensed(boolean licensed) {
            isLicensed = licensed;
        }

        public boolean isLicensed() {
            return isLicensed;
        }

        @Override
        public String toString() {
            return "License{" +
                    "isLicensed=" + isLicensed +
                    '}';
        }
    }
}
