package ru.tecon.admTools.reliabilityLimit.ejb;

import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import ru.tecon.admTools.reliabilityLimit.model.LimitData;
import ru.tecon.admTools.systemParams.SystemParamException;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Maksim Shchelkonogov
 * 18.08.2026
 */
@Stateless
@Local(ReliabilityLimitLocal.class)
public class ReliabilityLimitSB implements ReliabilityLimitLocal {

    private static final String SELECT_GET_OBJECT_PATH = "select admin.get_obj_path_all(?) || ' (' || admin.get_obj_address(?) || ')'";
    private static final String SEL_LIMIT_DATA = "select par_id, par_name, techproc_type_code, zone, measure_name, t_min_tech, t_max_tech from dsp_0102t.sel_a_params(?)";
    private static final String UPD_LIMIT_DATA = "call dsp_0102t.save_a_param(?, ?, ?, ?, ?)";

    @Inject
    private Logger logger;

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    @Override
    public String getObjectPath(int objectID) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_GET_OBJECT_PATH)) {
            stm.setInt(1, objectID);
            stm.setInt(2, objectID);
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return res.getString(1);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error load object path", e);
        }

        return "";
    }

    @Override
    public List<LimitData> getLimitData(int objectID) {
        List<LimitData> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_LIMIT_DATA)) {
            stm.setInt(1, objectID);
            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new LimitData(res.getInt("par_id"), res.getString("par_name"), res.getString("techproc_type_code"),
                        res.getInt("zone"), res.getString("measure_name"),
                        res.getString("t_min_tech") == null ? null : res.getDouble("t_min_tech"),
                        res.getString("t_max_tech") == null ? null : res.getDouble("t_max_tech")));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error load limit data", e);
        }

        return result;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public void updateLimitData(int objectId, LimitData limitData, String login) throws SystemParamException {
        try (Connection connect = ds.getConnection();
             CallableStatement cStm = connect.prepareCall(UPD_LIMIT_DATA)) {
            cStm.setInt(1, objectId);
            cStm.setInt(2, limitData.getParId());
            if (limitData.getMin() == null) {
                cStm.setNull(3, Types.NUMERIC);
            } else {
                cStm.setBigDecimal(3, BigDecimal.valueOf(limitData.getMin()));
            }
            if (limitData.getMax() == null) {
                cStm.setNull(4, Types.NUMERIC);
            } else {
                cStm.setBigDecimal(4, BigDecimal.valueOf(limitData.getMax()));
            }
            cStm.setString(5, login);

            cStm.executeUpdate();

            logger.log(Level.INFO, "Update limit data");
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Error update limit data", ex);
            throw new SystemParamException("Внутренняя ошибка сервера");
        }
    }
}
