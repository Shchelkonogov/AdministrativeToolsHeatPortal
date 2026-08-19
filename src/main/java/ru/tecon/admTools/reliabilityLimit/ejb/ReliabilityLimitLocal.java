package ru.tecon.admTools.reliabilityLimit.ejb;

import jakarta.ejb.Local;
import ru.tecon.admTools.reliabilityLimit.model.LimitData;
import ru.tecon.admTools.systemParams.SystemParamException;

import java.util.List;

/**
 * @author Maksim Shchelkonogov
 * 18.08.2026
 */
@Local
public interface ReliabilityLimitLocal {

    /**
     * Метод выгружает строку описания объекта (организационный путь и территориальный адрес)
     * @param objectID id объекта
     * @return строка описания объекта
     */
    String getObjectPath(int objectID);

    List<LimitData> getLimitData(int objectID);

    void updateLimitData(int objectId, LimitData limitData, String login) throws SystemParamException;
}
