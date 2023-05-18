package ru.tecon.admTools.systemParams.cdi.struct;

import ru.tecon.admTools.systemParams.SystemParamException;
import ru.tecon.admTools.systemParams.cdi.scope.application.ObjectTypeController;
import ru.tecon.admTools.systemParams.ejb.struct.StructCurrentRemote;
import ru.tecon.admTools.systemParams.ejb.struct.StructSB;
import ru.tecon.admTools.systemParams.model.struct.StructType;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Контроллер для формы объекты группы структур
 *
 * @author Maksim Shchelkonogov
 */
@Named("structObjects")
@ViewScoped
public final class StructObjectsMB extends StructMB implements Serializable {

    private static final String HEADER = "Типы объектов";
    private static final String DIALOG_HEADER = "Создать новый объект";
    private static final String PROP_HEADER = "Свойства объекта";
    private static final String TYPE = "OBJ";

    @EJB(name = "structObjectsSB", mappedName = "ejb/structObjectsSB")
    private StructCurrentRemote structCurrentBean;

    @EJB
    private StructSB structBean;

    @PostConstruct
    public void init() {
        super.setStructCurrentBean(structCurrentBean);
        super.setStructBean(structBean);
        super.init(TYPE);
    }

    public String getHeader() {
        return HEADER;
    }

    public String getDialogHeader() {
        return DIALOG_HEADER;
    }

    public String getPropHeader() {
        return PROP_HEADER;
    }

    @Inject
    private ObjectTypeController objectTypeController;

    @Override
    List<StructType> getStructTypes() {
        List<StructType> result = new ArrayList<>();
        objectTypeController.getObjectTypes().forEach(objectType -> result.add(new StructType(objectType.getId(), objectType.getName(), objectType.getCode())));
        return result;
    }

    @Override
    int addStruct(StructType structType, String login, String ip) throws SystemParamException {
        int result = super.addStruct(structType, login, ip);
        objectTypeController.loadDefaultTypes();
        return result;
    }

    @Override
    void removeStruct(StructType structType, String login, String ip) throws SystemParamException {
        super.removeStruct(structType, login, ip);
        objectTypeController.loadDefaultTypes();
    }
}
