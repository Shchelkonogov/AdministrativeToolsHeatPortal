package ru.tecon.admTools.systemParams.ejb;

import ru.tecon.admTools.systemParams.SystemParamException;
import ru.tecon.admTools.systemParams.model.CoefficientRC;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless bean для получения данных из базы по форме коэффициенты для режимной карты
 * @author Maksim Shchelkonogov
 */
@Stateless
@LocalBean
public class CoefficientForRegimeCardSB {

    private static final Logger LOGGER = Logger.getLogger(CoefficientForRegimeCardSB.class.getName());

    private static final String SEL_COEFFICIENTS = "select * from sys_0001t.sel_rk_param()";
    private static final String FUN_UPDATE_COEFFICIENT = "call sys_0001t.upd_rk_param(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    /**
     * Получение списка коэффициентов
     * @return список коэффициентов
     */
    public List<CoefficientRC> getCoefficients() {
        List<CoefficientRC> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_COEFFICIENTS)) {
            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new CoefficientRC(res.getInt("id"), res.getString("par_memo"), res.getString("par_name"),
                        res.getDouble("t_min"), res.getDouble("t_max"), res.getBoolean("t_percent"),
                        res.getDouble("a_min"), res.getDouble("a_max"), res.getBoolean("a_percent")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "error load coefficients", e);
        }
        return result;
    }

    /**
     * Сохранение изменений коэффициентов в базе
     * @param coefficient коеффициент, который изменили
     * @param login идентификатор пользователя
     * @param ip адрес пользователя
     * @throws SystemParamException ошибка сохранения в базу
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateCoefficient(CoefficientRC coefficient, String login, String ip) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(FUN_UPDATE_COEFFICIENT)) {
            cStm.setInt(1, coefficient.getId());
            cStm.setObject(2, coefficient.gettMin(), Types.NUMERIC);
            cStm.setObject(3, coefficient.gettMax(), Types.NUMERIC);
            cStm.setShort(4, (short) (coefficient.isT() ? 1 : 0));
            cStm.setObject(5, coefficient.getaMin(), Types.NUMERIC);
            cStm.setObject(6, coefficient.getaMax(), Types.NUMERIC);
            cStm.setShort(7, (short) (coefficient.isA() ? 1 : 0));
            cStm.setString(8, login);
            cStm.setString(9, ip);
            cStm.registerOutParameter(10, Types.SMALLINT);

            cStm.executeUpdate();

            LOGGER.info("update coefficient " + coefficient + " result " + cStm.getShort(10));

            if (cStm.getShort(10) != 0) {
                throw new SystemParamException("Ошибка обновления коэффициента " + coefficient.getName());
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "SQLException", e);
            throw new SystemParamException("Внутренняя ошибка сервера");
        }
    }
}
