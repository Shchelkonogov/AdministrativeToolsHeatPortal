package ru.tecon.admTools.systemParams;

import ru.tecon.admTools.utils.AdmTools;

import java.sql.SQLException;

/**
 * Класс для обработки ошибок о невыполенние функций базы для формы системные параметры
 * @author Maksim Shchelkonogov
 */
public class SystemParamException extends Exception {

    public SystemParamException() {
        super();
    }

    public SystemParamException(String message) {
        super(message);
    }

    public SystemParamException(SQLException ex) {
        super(AdmTools.getSQLExceptionMessage(ex));
    }
}
