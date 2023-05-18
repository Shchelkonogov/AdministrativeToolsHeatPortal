package ru.tecon.admTools.systemParams.cdi.scope.application;

import ru.tecon.admTools.systemParams.SystemParamException;
import ru.tecon.admTools.systemParams.ejb.DefaultValuesSB;
import ru.tecon.admTools.systemParams.model.ObjectType;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Контроллер на уровне приложения с данными о типах объектов
 *
 * @author Maksim Shchelkonogov
 * 23.03.2023
 */
@Named("objectTypeControllerApplication")
@ApplicationScoped
public class ObjectTypeController implements Serializable {

    private ObjectType defaultObjectType;
    private List<ObjectType> objectTypes = new LinkedList<>();

    @Inject
    private Logger logger;

    @EJB
    private DefaultValuesSB defaultValuesBean;

    @PostConstruct
    private void init() {
        logger.log(Level.INFO, "init bean");
        loadDefaultTypes();
    }

    /**
     * Загрузка типов объектов
     */
    public void loadDefaultTypes() {
        logger.log(Level.INFO, "load default types data");
        objectTypes = defaultValuesBean.getObjectTypes();
        try {
            int defaultObjectTypeID = defaultValuesBean.getDefaultObjectTypeID();

            defaultObjectType = objectTypes.stream()
                    .filter(objectType -> objectType.getId() == defaultObjectTypeID)
                    .findFirst()
                    .orElseThrow(SystemParamException::new);
        } catch (SystemParamException e) {
            logger.log(Level.WARNING, "error load default object type", e);
        }
    }

    public String getObjectNameById(int id) {
        Optional<ObjectType> any = objectTypes.stream().filter(objectType -> objectType.getId() == id).findAny();
        if (any.isPresent()) {
            return any.get().getName();
        } else {
            return "";
        }
    }

    public ObjectType getDefaultObjectType() {
        return defaultObjectType;
    }

    public List<ObjectType> getObjectTypes() {
        return objectTypes;
    }
}
