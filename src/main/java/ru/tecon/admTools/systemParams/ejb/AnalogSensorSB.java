package ru.tecon.admTools.systemParams.ejb;

import jakarta.annotation.Resource;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import ru.tecon.admTools.systemParams.SystemParamException;
import ru.tecon.admTools.systemParams.model.analogSensor.CriteriaData;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Maksim Shchelkonogov
 * 06.06.2025
 */
@Stateless
@LocalBean
public class AnalogSensorSB {

    @Inject
    private Logger logger;

    private static final String SEL_CRITERIA = "select * from sys_0001t.sel_wa_criteria()";
    private static final String UPD_CRITERIA_STATE = "call sys_0001t.upd_wa_criteria(?, ?, ?, ?, ?)";
    private static final String SEL_OFFLINE_DAYS = "select * from sys_0001t.sel_wa_offline_days()";
    private static final String UPD_OFFLINE_DAYS = "call sys_0001t.upd_wa_offline_days(?, ?, ?, ?)";
    private static final String SEL_DYNAMIC_LACK = "select * from sys_0001t.sel_wa_vgv_daynum()";
    private static final String UPD_DYNAMIC_LACK = "call sys_0001t.upd_wa_vgv_daynum(?, ?, ?, ?)";
    private static final String SEL_DELTA_TNV = "select * from sys_0001t.sel_wa_dtnv()";
    private static final String UPD_DELTA_TNV = "call sys_0001t.upd_wa_dtnv(?, ?, ?, ?)";

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    /**
     * Получение списка критериев
     *
     * @return список критериев
     */
    public List<CriteriaData> getCriteria() {
        List<CriteriaData> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_CRITERIA)) {

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new CriteriaData(res.getInt(1), res.getString(2), "Y".equalsIgnoreCase(res.getString(3))));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException", e);
        }

        result.sort(Comparator.comparing(CriteriaData::getId));

        return result;
    }

    /**
     * Изменение состояние критерия
     *
     * @param data критерий
     * @param login идентификатор пользователя
     * @param ip адрес пользователя
     * @throws SystemParamException если произошла ошибка записи данных в базу
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void changeCriteriaState(CriteriaData data, String login, String ip) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(UPD_CRITERIA_STATE)) {
            cStm.setInt(1, data.getId());
            cStm.setString(2, data.isState() ? "Y" : "N");
            cStm.setString(3, login);
            cStm.setString(4, ip);
            cStm.registerOutParameter(5, Types.SMALLINT);

            cStm.executeUpdate();

            logger.log(Level.INFO, "Update criteria state for " + data + " result " + cStm.getShort(5));

            if (cStm.getShort(5) != 0) {
                throw new SystemParamException("Невозможно изменить состояние критерия");
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error update generate stat", ex);
            throw new SystemParamException("Внутренняя ошибка сервера");
        }
    }

    /**
     * Получение количества дней, в течении которых не было связи с объектом в целом
     *
     * @return количество дней, в течении которых не было связи с объектом в целом
     */
    public int getOfflineDays() {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_OFFLINE_DAYS)) {

            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return res.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException", e);
        }

        throw new RuntimeException("Ошибка запроса данных");
    }

    /**
     * Изменение количества дней, в течении которых не было связи с объектом в целом
     *
     * @param offlineDays количество дней, в течении которых не было связи с объектом в целом
     * @param login идентификатор пользователя
     * @param ip адрес пользователя
     * @throws SystemParamException если произошла ошибка записи данных в базу
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateOfflineDays(int offlineDays, String login, String ip) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(UPD_OFFLINE_DAYS)) {
            cStm.setInt(1, offlineDays);
            cStm.setString(2, login);
            cStm.setString(3, ip);
            cStm.registerOutParameter(4, Types.SMALLINT);

            cStm.executeUpdate();

            logger.log(Level.INFO, "Update offline days result " + cStm.getShort(4));

            if (cStm.getShort(4) != 0) {
                throw new SystemParamException("Невозможно изменить количество дней, в течении которых не было связи с объектом в целом");
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error update offline days", ex);
            throw new SystemParamException("Внутренняя ошибка сервера");
        }
    }

    /**
     * Получение количества дней отсутствия динамики
     *
     * @return количество дней отсутствия динамики
     */
    public int getDynamicLack() {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_DYNAMIC_LACK)) {

            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return res.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException", e);
        }

        throw new RuntimeException("Ошибка запроса данных");
    }

    /**
     * Изменение количества дней отсутствия динамики
     *
     * @param dynamicLack количество дней отсутствия динамики
     * @param login идентификатор пользователя
     * @param ip адрес пользователя
     * @throws SystemParamException если произошла ошибка записи данных в базу
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateDynamicLack(int dynamicLack, String login, String ip) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(UPD_DYNAMIC_LACK)) {
            cStm.setInt(1, dynamicLack);
            cStm.setString(2, login);
            cStm.setString(3, ip);
            cStm.registerOutParameter(4, Types.SMALLINT);

            cStm.executeUpdate();

            logger.log(Level.INFO, "Update dynamic lack result " + cStm.getShort(4));

            if (cStm.getShort(4) != 0) {
                throw new SystemParamException("Невозможно изменить количество дней отсутствия динамики");
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error update dynamic lack", ex);
            throw new SystemParamException("Внутренняя ошибка сервера");
        }
    }

    /**
     * Получение отклонения Тнв
     *
     * @return отклонение Тнв
     */
    public int getDeltaTNV() {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_DELTA_TNV)) {

            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return res.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException", e);
        }

        throw new RuntimeException("Ошибка запроса данных");
    }

    /**
     * Изменение отклонения Тнв
     *
     * @param deltaTnv отклонение Тнв
     * @param login идентификатор пользователя
     * @param ip адрес пользователя
     * @throws SystemParamException если произошла ошибка записи данных в базу
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateDeltaTNV(int deltaTnv, String login, String ip) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(UPD_DELTA_TNV)) {
            cStm.setInt(1, deltaTnv);
            cStm.setString(2, login);
            cStm.setString(3, ip);
            cStm.registerOutParameter(4, Types.SMALLINT);

            cStm.executeUpdate();

            logger.log(Level.INFO, "Update delta tnv result " + cStm.getShort(4));

            if (cStm.getShort(4) != 0) {
                throw new SystemParamException("Невозможно изменить отклонение Тнв");
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error update delta tnv", ex);
            throw new SystemParamException("Внутренняя ошибка сервера");
        }
    }
}
