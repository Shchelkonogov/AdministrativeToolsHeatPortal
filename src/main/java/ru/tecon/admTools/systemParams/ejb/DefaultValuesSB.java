package ru.tecon.admTools.systemParams.ejb;

import ru.tecon.admTools.systemParams.SystemParamException;
import ru.tecon.admTools.systemParams.model.ObjectType;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless bean для загрузки данных по форме значения по умолчанию
 * @author Maksim Shchelkonogov
 */
@Stateless
@LocalBean
public class DefaultValuesSB {

    private static final Logger LOGGER = Logger.getLogger(DefaultValuesSB.class.getName());

    private static final String SEL_OBJECT_TYPES = "select obj_type_id, obj_type_name, obj_type_char " +
            "from table(sys_0001t.sel_obj_type())";
    private static final String SEL_DEFAULT_OBJECT_TYPE_ID = "select obj_type_def from dz_sys_param";
    private static final String FUN_UPD_DEFAULT_OBJECT_TYPE = "{? = call sys_0001t.upd_obj_type_def(?, ?, ?)}";

    private static final String SEL_DATA_BASE_LIST = "select * from table(sys_0001t.sel_db_name())";
    private static final String SEL_DEFAULT_DATA_BASE = "select default_db_name from dz_sys_param";
    private static final String FUN_UPD_DEFAULT_DATA_BASE_NAME = "{? = call sys_0001t.upd_default_db_name(?, ?, ?)}";

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    /**
     * Получение списка типов объектов
     * @return список типов объектов
     */
    public List<ObjectType> getObjectTypes() {
        List<ObjectType> result = new LinkedList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_OBJECT_TYPES)) {
            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new ObjectType(res.getInt("obj_type_id"), res.getString("obj_type_name"), res.getString("obj_type_char")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "error load object types", e);
        }
        return result;
    }

    /**
     * выгрузка идентификатора типа объекта по умолчанию
     * @return идентификатор объекта
     * @throws SystemParamException в случае ошибки выгрузки из базы
     */
    public int getDefaultObjectTypeID() throws SystemParamException {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_DEFAULT_OBJECT_TYPE_ID)) {
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return res.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "error load default object type", e);
        }

        throw new SystemParamException("error load default object type");
    }

    /**
     * обновление типа объекта по умолчанию
     * @param objectType тип объекта
     * @param login пользователь
     * @param ip адрес
     * @throws SystemParamException в случае ошибки загрузки данных
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateDefaultObjectType(ObjectType objectType, String login, String ip) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(FUN_UPD_DEFAULT_OBJECT_TYPE)) {
            cStm.registerOutParameter(1, Types.INTEGER);
            cStm.setInt(2, objectType.getId());
            cStm.setString(3, login);
            cStm.setString(4, ip);

            cStm.executeUpdate();

            LOGGER.info("update default type " + objectType);

            if (cStm.getInt(1) != 0) {
                throw new SystemParamException("Ошибка изменения типа объекта по умолчанию");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "error update default type", e);
            throw new SystemParamException("Внутренняя ошибка сервера");
        }
    }

    /**
     * @return список баз данных
     */
    public List<String> getDataBaseList() {
        List<String> result = new LinkedList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_DATA_BASE_LIST)) {
            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(res.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "error load data base list", e);
        }
        return result;
    }

    /**
     * @return имя базы данных по умолчанию
     * @throws SystemParamException в случае ошибки выгрузки данных
     */
    public String getDefaultDataBase() throws SystemParamException {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_DEFAULT_DATA_BASE)) {
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return res.getString(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "error load default data base", e);
        }

        throw new SystemParamException("error load default data base");
    }

    /**
     * Обновление базы данных по умолчанию
     * @param dataBase имя базы данных
     * @param login пользователь
     * @param ip адрес
     * @throws SystemParamException в случае ошибки записи в базу
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateDefaultDataBase(String dataBase, String login, String ip) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(FUN_UPD_DEFAULT_DATA_BASE_NAME)) {
            cStm.registerOutParameter(1, Types.INTEGER);
            cStm.setString(2, dataBase);
            cStm.setString(3, login);
            cStm.setString(4, ip);

            cStm.executeUpdate();

            LOGGER.info("update default data base " + dataBase);

            if (cStm.getInt(1) != 0) {
                throw new SystemParamException("Ошибка изменения первичной базы по умолчанию");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "error update default type", e);
            throw new SystemParamException("Внутренняя ошибка сервера");
        }
    }
}
