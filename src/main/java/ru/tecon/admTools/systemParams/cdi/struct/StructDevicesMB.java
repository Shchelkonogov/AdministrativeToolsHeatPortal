package ru.tecon.admTools.systemParams.cdi.struct;

import ru.tecon.admTools.systemParams.ejb.struct.StructCurrentRemote;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;

/**
 * Контроллер для формы Устройства группы структур
 * @author Maksim Shchelkonogov
 * 17.01.2023
 */
@Named("structDevices")
@ViewScoped
public class StructDevicesMB extends StructMB implements Serializable {

    private static final String HEADER = "Типы устройств";
    private static final String DIALOG_HEADER = "Создать новую сущность";
    private static final String PROP_HEADER = "Свойства устройства";

    @EJB(name = "structDevicesSB", mappedName = "ejb/structDevicesSB")
    private StructCurrentRemote devicesSB;

    @PostConstruct
    public void init() {
        super.setStructCurrentBean(devicesSB);
        super.setAddToRoot(false);
        super.initForm();
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
}
