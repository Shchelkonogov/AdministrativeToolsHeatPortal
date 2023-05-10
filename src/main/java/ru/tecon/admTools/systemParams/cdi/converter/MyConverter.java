package ru.tecon.admTools.systemParams.cdi.converter;

import ru.tecon.admTools.systemParams.model.Measure;
import ru.tecon.admTools.systemParams.model.struct.PropValType;

import java.util.List;

/**
 * Интерфейс для получения значений типов свойст и единиц измерений
 * @author Maksim Shchelkonogov
 */
public interface MyConverter {

    /**
     * @return список типо свойств
     */
    List<PropValType> getPropValTypes();
}
