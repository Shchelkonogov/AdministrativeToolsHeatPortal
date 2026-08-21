package ru.tecon.admTools.linker.ejb;

import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import ru.tecon.admTools.components.navigation.ejb.NavigationBeanLocal;
import ru.tecon.admTools.components.navigation.model.ObjTypePropertyModel;
import ru.tecon.admTools.components.navigation.model.SpObject;
import ru.tecon.admTools.components.navigation.model.TreeNodeModel;
import ru.tecon.admTools.utils.AlphaNumComparator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless bean для работы с блоком навигации на форме
 */
@Stateless(name = "navigationBean")
@Local(NavigationBeanLocal.class)
public class NavigationSBean implements NavigationBeanLocal {

    private static final String SQL_OBJECT_TYPE_PROPERTY = "select obj_prop_id, obj_prop_name, sp_header_id " +
            "from dsp_0032t.get_obj_type_props(?)";
    private static final String SELECT_ORG_TREE = "select * from lnk_0001t.sel_unlinked_obj_tree(?, ?, ?, ?) where parent = ?";
    private static final String SEL_SP_DATA = "select sp_body_id, sp_body_name from sys_0001t.sel_sp_body(?) where lower(sp_body_name) like ? limit 50";

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    @Inject
    private Logger logger;

    @Override
    public TreeNodeModel getRootOrgTree() {
        return new TreeNodeModel("S", "S");
    }

    @Override
    public List<TreeNodeModel> getOrgTree(int objectTypeId, long searchTypeId, String searchText,
                                          String userName, String parentNode) {
        logger.log(Level.INFO, "Load org tree node data for objectType: {0}, searchType: {1}, " +
                "searchText: \"{2}\", userName: {3}, parentNode: {4}",
                new Object[]{objectTypeId, searchTypeId, searchText, userName, parentNode});

        List<TreeNodeModel> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_ORG_TREE)) {
            stm.setLong(1, objectTypeId);
            stm.setLong(2, searchTypeId);
            stm.setString(3, searchText);
            stm.setString(4, userName);
            stm.setString(5, parentNode);

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new TreeNodeModel(res.getString(1), res.getString(2), res.getString(3), res.getString(4), res.getString(5)));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error load ort tree node", e);
        }

        result.sort(new AlphaNumComparator());

        return result;
    }

    @Override
    public TreeNodeModel getRootTerTree() {
        return new TreeNodeModel("S", "S");
    }

    @Override
    public List<TreeNodeModel> getTerTree(int objectTypeId, long searchTypeId, String searchText, String userName, String parentNode) {
        return new ArrayList<>();
    }

    @Override
    public TreeNodeModel getRootLinkTree() {
        return new TreeNodeModel("S", "S");
    }

    @Override
    public List<TreeNodeModel> getLinkTree(int objectTypeId, long searchTypeId, String searchText, String userName, String parentNode) {
        return new ArrayList<>();
    }

    @Override
    public List<ObjTypePropertyModel> getObjTypeProps(long objTypeId) {
        List<ObjTypePropertyModel> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_OBJECT_TYPE_PROPERTY)) {
            stm.setLong(1, objTypeId);

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new ObjTypePropertyModel(res.getString(2), res.getLong(1), res.getString("sp_header_id") == null ? null : res.getInt("sp_header_id")));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error load search types", e);
        }
        return result;
    }

    @Override
    public List<SpObject> getSpData(int spId, String filterValue) {
        List<SpObject> result = new ArrayList<>();
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SEL_SP_DATA)) {
            stm.setInt(1, spId);
            stm.setString(2, filterValue == null ? "%%" : "%" + filterValue + "%");

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                result.add(new SpObject(res.getLong("sp_body_id"), res.getString("sp_body_name")));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error load sp values", e);
        }

        return result;
    }
}
